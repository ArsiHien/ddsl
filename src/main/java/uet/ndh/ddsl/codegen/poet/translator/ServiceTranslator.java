package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.*;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryMethodDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.TypeMapper;

import javax.lang.model.element.Modifier;
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
    
    private static final ClassName OPTIONAL = ClassName.get(Optional.class);
    
    public ServiceTranslator(TypeMapper typeMapper) {
        this.typeMapper = typeMapper;
        this.expressionTranslator = new ExpressionTranslator(typeMapper);
    }
    
    /**
     * Translate a domain service to a CodeArtifact.
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
        
        // Add declared methods
        for (MethodDecl method : service.methods()) {
            classBuilder.addMethod(translateMethodDecl(method));
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
    
    private MethodSpec translateMethodDecl(MethodDecl method) {
        TypeName returnType = method.returnType() != null 
            ? typeMapper.mapType(method.returnType())
            : TypeName.VOID;
        
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
            .addModifiers(Modifier.PUBLIC)
            .returns(returnType);
        
        // Add parameters
        for (ParameterDecl param : method.parameters()) {
            TypeName paramType = typeMapper.mapType(param.type());
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
