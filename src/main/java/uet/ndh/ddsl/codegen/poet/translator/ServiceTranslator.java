package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.*;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryMethodDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.TypeMapper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Translates DomainService and Repository AST nodes to JavaPoet TypeSpecs.
 * 
 * Domain services contain domain logic that doesn't naturally fit within entities.
 * Repositories provide collection-like access to aggregates.
 */
public class ServiceTranslator {
    
    private final TypeMapper typeMapper;
    private final ExpressionTranslator expressionTranslator;
    private final List<String> translationErrors;
    
    private static final ClassName OPTIONAL = ClassName.get(Optional.class);
    
    public ServiceTranslator(TypeMapper typeMapper) {
        this.typeMapper = typeMapper;
        this.expressionTranslator = new ExpressionTranslator(typeMapper);
        this.translationErrors = new ArrayList<>();
    }
    
    /**
     * Get any errors that occurred during translation (e.g. unresolved param types).
     */
    public List<String> getTranslationErrors() {
        return List.copyOf(translationErrors);
    }
    
    /**
     * Clear accumulated translation errors.
     */
    public void clearTranslationErrors() {
        translationErrors.clear();
    }
    
    /**
     * Translate a domain service to a CodeArtifact.
     * Uses full BehaviorDecl data to generate complete method bodies with domain logic.
     * Parameters are resolved to their domain types (Entity/VO/Aggregate).
     */
    public CodeArtifact translate(DomainServiceDecl service) {
        String packageName = typeMapper.getBasePackage() + ".service";
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(service.name())
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Domain Service: $L\n", service.name());
        
        // Add documentation if present
        if (service.documentation() != null) {
            classBuilder.addJavadoc("\n$L\n", service.documentation());
        }
        
        // Add dependencies as constructor parameters
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC);
        
        for (FieldDecl dependency : service.dependencies()) {
            TypeName depType = typeMapper.mapType(dependency.type());
            String fieldName = dependency.name();
            
            // Add field
            classBuilder.addField(FieldSpec.builder(depType, fieldName, Modifier.PRIVATE, Modifier.FINAL)
                .build());
            
            // Add constructor parameter
            constructor.addParameter(depType, fieldName);
            constructor.addStatement("this.$N = $N", fieldName, fieldName);
        }
        
        classBuilder.addMethod(constructor.build());
        
        // Prefer behaviors over plain methods for full code generation
        if (!service.behaviors().isEmpty()) {
            for (BehaviorDecl behavior : service.behaviors()) {
                // Validate parameter types before generating code
                validateBehaviorParamTypes(behavior, service.name());
                classBuilder.addMethod(expressionTranslator.translateBehavior(behavior));
            }
        } else {
            // Fallback: use plain method declarations (legacy path)
            for (MethodDecl method : service.methods()) {
                classBuilder.addMethod(translateMethodDeclWithTypeResolution(method));
            }
        }
        
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build())
            .skipJavaLangImports(true)
            .indent("    ")
            .build();
        
        return new CodeArtifact(
            service.name(),
            packageName,
            javaFile.toString(),
            CodeArtifact.ArtifactType.DOMAIN_SERVICE
        );
    }
    
    /**
     * Validate that behavior parameter types can be resolved to known domain types.
     * Reports errors for parameters that remain as 'Object' and cannot be resolved.
     */
    private void validateBehaviorParamTypes(BehaviorDecl behavior, String serviceName) {
        for (ParameterDecl param : behavior.parameters()) {
            if ("Object".equals(param.type().name())) {
                TypeName resolved = typeMapper.tryResolveParamType(param.name());
                if (resolved == null) {
                    translationErrors.add(
                        "Service '" + serviceName + "', method '" + behavior.getName() 
                        + "': parameter '" + param.name() 
                        + "' has unresolved type. No Entity, ValueObject, or Aggregate named '"
                        + capitalize(param.name()) + "' is defined. "
                        + "Define the type or use explicit type annotation (e.g., '" 
                        + param.name() + " as TypeName')."
                    );
                }
            }
        }
    }
    
    /**
     * Translate a repository to a CodeArtifact.
     */
    public CodeArtifact translateRepository(RepositoryDecl repository) {
        String packageName = typeMapper.getBasePackage() + ".repository";
        
        // Get aggregate type from TypeRef
        TypeName aggregateType = typeMapper.mapType(repository.aggregateType());
        TypeName idType = ClassName.get(Object.class); // Default ID type
        
        // Parameterized repository interface
        ClassName baseRepository = ClassName.get(typeMapper.getBasePackage() + ".shared", "Repository");
        
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(repository.name())
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ParameterizedTypeName.get(baseRepository, aggregateType, idType))
            .addJavadoc("Repository for $L aggregates.\n", repository.aggregateType().name());
        
        // Add custom query methods
        for (RepositoryMethodDecl method : repository.methods()) {
            interfaceBuilder.addMethod(translateRepositoryMethod(method));
        }
        
        JavaFile javaFile = JavaFile.builder(packageName, interfaceBuilder.build())
            .skipJavaLangImports(true)
            .indent("    ")
            .build();
        
        return new CodeArtifact(
            repository.name(),
            packageName,
            javaFile.toString(),
            CodeArtifact.ArtifactType.REPOSITORY
        );
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Translate a MethodDecl with type resolution for parameters.
     * If a parameter type is 'Object', tries to resolve it to a known domain type.
     */
    private MethodSpec translateMethodDeclWithTypeResolution(MethodDecl method) {
        TypeName returnType = method.returnType() != null 
            ? typeMapper.mapType(method.returnType())
            : TypeName.VOID;
        
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
            .addModifiers(Modifier.PUBLIC)
            .returns(returnType);
        
        // Add parameters with domain type resolution
        for (ParameterDecl param : method.parameters()) {
            TypeName paramType = typeMapper.mapType(param.type());
            // If the param type is Object (untyped in DDSL), try to resolve from domain types
            if ("Object".equals(param.type().name())) {
                TypeName resolved = typeMapper.tryResolveParamType(param.name());
                if (resolved != null) {
                    paramType = resolved;
                }
            }
            builder.addParameter(paramType, param.name());
        }
        
        // Add TODO body
        if (returnType.equals(TypeName.VOID)) {
            builder.addStatement("// TODO: Implement $N", method.name());
        } else {
            builder.addStatement("// TODO: Implement $N", method.name());
            builder.addStatement("throw new $T($S)", 
                ClassName.get(UnsupportedOperationException.class),
                "Not yet implemented");
        }
        
        return builder.build();
    }
    
    private MethodSpec translateRepositoryMethod(RepositoryMethodDecl method) {
        TypeName returnType = method.returnType() != null 
            ? typeMapper.mapType(method.returnType())
            : TypeName.VOID;
        
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(returnType);
        
        for (ParameterDecl param : method.parameters()) {
            TypeName paramType = typeMapper.mapType(param.type());
            builder.addParameter(paramType, param.name());
        }
        
        return builder.build();
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
