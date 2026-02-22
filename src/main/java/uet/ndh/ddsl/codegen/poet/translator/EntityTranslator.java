package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.*;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.entity.IdentityFieldDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.TypeMapper;

import javax.lang.model.element.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Translates standalone Entity, ValueObject, and DomainEvent AST nodes to JavaPoet TypeSpecs.
 * 
 * This translator handles types that are not part of an aggregate:
 * - Standalone entities
 * - Shared value objects
 * - Cross-aggregate domain events
 */
public class EntityTranslator {
    
    private final TypeMapper typeMapper;
    private final ExpressionTranslator expressionTranslator;
    
    private static final ClassName INSTANT = ClassName.get(Instant.class);
    private static final ClassName OBJECTS = ClassName.get(Objects.class);
    
    public EntityTranslator(TypeMapper typeMapper) {
        this.typeMapper = typeMapper;
        this.expressionTranslator = new ExpressionTranslator(typeMapper);
    }
    
    /**
     * Translate a standalone entity to a CodeArtifact.
     */
    public CodeArtifact translate(EntityDecl entity) {
        String packageName = typeMapper.getBasePackage();
        JavaFile javaFile = translateEntityToJavaFile(entity, packageName);
        
        return new CodeArtifact(
            entity.name(),
            packageName,
            javaFile.toString(),
            CodeArtifact.ArtifactType.ENTITY
        );
    }
    
    /**
     * Translate a value object to a CodeArtifact.
     */
    public CodeArtifact translateValueObject(ValueObjectDecl valueObject) {
        String packageName = typeMapper.getBasePackage();
        JavaFile javaFile = translateValueObjectToJavaFile(valueObject, packageName);
        
        return new CodeArtifact(
            valueObject.name(),
            packageName,
            javaFile.toString(),
            CodeArtifact.ArtifactType.VALUE_OBJECT
        );
    }
    
    /**
     * Translate a domain event to a CodeArtifact.
     */
    public CodeArtifact translateDomainEvent(DomainEventDecl event) {
        String packageName = typeMapper.getBasePackage();
        JavaFile javaFile = translateEventToJavaFile(event, packageName);
        
        return new CodeArtifact(
            event.name(),
            packageName,
            javaFile.toString(),
            CodeArtifact.ArtifactType.DOMAIN_EVENT
        );
    }
    
    // ========== Internal Translation Methods ==========
    
    private JavaFile translateEntityToJavaFile(EntityDecl entity, String packageName) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(entity.name())
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(typeMapper.getEntityInterface())
            .addJavadoc(generateJavadoc(entity.documentation(), "Entity: " + entity.name()));
        
        // Add identity field
        if (entity.identity() != null) {
            classBuilder.addField(buildIdentityField(entity.identity()));
        }
        
        // Add regular fields
        for (FieldDecl field : entity.fields()) {
            classBuilder.addField(buildField(field));
        }
        
        // Add constructor
        classBuilder.addMethod(buildEntityConstructor(entity));
        
        // Add behavior methods
        for (BehaviorDecl behavior : entity.behaviors()) {
            classBuilder.addMethod(expressionTranslator.translateBehavior(behavior));
        }
        
        // Add equals/hashCode/getId
        if (entity.identity() != null) {
            classBuilder.addMethod(buildEqualsMethod(entity.name(), entity.identity().name()));
            classBuilder.addMethod(buildHashCodeMethod(entity.identity().name()));
            classBuilder.addMethod(buildGetIdMethod(entity.identity()));
        }
        
        // Add getters for all fields
        for (FieldDecl field : entity.fields()) {
            classBuilder.addMethod(buildGetter(field));
        }
        
        return JavaFile.builder(packageName, classBuilder.build())
            .skipJavaLangImports(true)
            .indent("    ")
            .build();
    }
    
    private JavaFile translateValueObjectToJavaFile(ValueObjectDecl valueObject, String packageName) {
        // Build record constructor with components from fields
        MethodSpec.Builder recordConstructorBuilder = MethodSpec.constructorBuilder();
        for (FieldDecl field : valueObject.fields()) {
            TypeName fieldType = typeMapper.mapType(field.type());
            recordConstructorBuilder.addParameter(ParameterSpec.builder(fieldType, field.name()).build());
        }
        
        TypeSpec.Builder recordBuilder = TypeSpec.recordBuilder(valueObject.name())
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(typeMapper.getValueObjectInterface())
            .addJavadoc(generateJavadoc(valueObject.documentation(), "Value Object: " + valueObject.name()))
            .recordConstructor(recordConstructorBuilder.build());
        
        // Add compact constructor for validation
        if (!valueObject.invariants().isEmpty()) {
            MethodSpec.Builder compactConstructor = MethodSpec.compactConstructorBuilder()
                .addModifiers(Modifier.PUBLIC);
            
            for (var invariant : valueObject.invariants()) {
                compactConstructor.addCode(expressionTranslator.translateInvariantValidation(invariant));
            }
            
            recordBuilder.addMethod(compactConstructor.build());
        }
        
        return JavaFile.builder(packageName, recordBuilder.build())
            .skipJavaLangImports(true)
            .indent("    ")
            .build();
    }
    
    private JavaFile translateEventToJavaFile(DomainEventDecl event, String packageName) {
        // Build record constructor with components
        MethodSpec.Builder recordConstructorBuilder = MethodSpec.constructorBuilder();
        
        // Add occurredAt timestamp
        recordConstructorBuilder.addParameter(ParameterSpec.builder(INSTANT, "occurredAt").build());
        
        // Add event payload fields
        for (FieldDecl field : event.fields()) {
            TypeName fieldType = typeMapper.mapType(field.type());
            recordConstructorBuilder.addParameter(ParameterSpec.builder(fieldType, field.name()).build());
        }
        
        TypeSpec.Builder recordBuilder = TypeSpec.recordBuilder(event.name())
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(typeMapper.getDomainEventInterface())
            .addJavadoc(generateJavadoc(event.documentation(), "Domain Event: " + event.name()))
            .recordConstructor(recordConstructorBuilder.build());
        
        // Add factory method
        recordBuilder.addMethod(buildEventFactoryMethod(event, packageName));
        
        return JavaFile.builder(packageName, recordBuilder.build())
            .skipJavaLangImports(true)
            .indent("    ")
            .build();
    }
    
    // ========== Helper Methods ==========
    
    private FieldSpec buildIdentityField(IdentityFieldDecl identity) {
        TypeName idType = typeMapper.mapType(identity.type());
        return FieldSpec.builder(idType, identity.name(), Modifier.PRIVATE, Modifier.FINAL)
            .build();
    }
    
    private FieldSpec buildField(FieldDecl field) {
        TypeName fieldType = typeMapper.mapType(field.type());
        FieldSpec.Builder builder = FieldSpec.builder(fieldType, field.name(), Modifier.PRIVATE);
        
        if (field.isFinal()) {
            builder.addModifiers(Modifier.FINAL);
        }
        
        return builder.build();
    }
    
    private MethodSpec buildEntityConstructor(EntityDecl entity) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC);
        
        if (entity.identity() != null) {
            TypeName idType = typeMapper.mapType(entity.identity().type());
            constructor.addParameter(idType, entity.identity().name());
            constructor.addStatement("this.$N = $N", entity.identity().name(), entity.identity().name());
        }
        
        for (FieldDecl field : entity.fields()) {
            TypeName fieldType = typeMapper.mapType(field.type());
            constructor.addParameter(fieldType, field.name());
            constructor.addStatement("this.$N = $N", field.name(), field.name());
        }
        
        return constructor.build();
    }
    
    private MethodSpec buildGetter(FieldDecl field) {
        TypeName fieldType = typeMapper.mapType(field.type());
        String getterName = "get" + capitalize(field.name());
        
        return MethodSpec.methodBuilder(getterName)
            .addModifiers(Modifier.PUBLIC)
            .returns(fieldType)
            .addStatement("return this.$N", field.name())
            .build();
    }
    
    private MethodSpec buildEqualsMethod(String className, String identityFieldName) {
        return MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.BOOLEAN)
            .addParameter(ClassName.get(Object.class), "obj")
            .addStatement("if (this == obj) return true")
            .addStatement("if (obj == null || getClass() != obj.getClass()) return false")
            .addStatement("$N other = ($N) obj", className, className)
            .addStatement("return $T.equals(this.$N, other.$N)", OBJECTS, identityFieldName, identityFieldName)
            .build();
    }
    
    private MethodSpec buildHashCodeMethod(String identityFieldName) {
        return MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TypeName.INT)
            .addStatement("return $T.hash($N)", OBJECTS, identityFieldName)
            .build();
    }
    
    private MethodSpec buildGetIdMethod(IdentityFieldDecl identity) {
        TypeName idType = typeMapper.mapType(identity.type());
        return MethodSpec.methodBuilder("getId")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(idType)
            .addStatement("return this.$N", identity.name())
            .build();
    }
    
    private MethodSpec buildEventFactoryMethod(DomainEventDecl event, String packageName) {
        MethodSpec.Builder factoryMethod = MethodSpec.methodBuilder("now")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(ClassName.get(packageName, event.name()));
        
        StringBuilder args = new StringBuilder("$T.now()");
        List<Object> formatArgs = new ArrayList<>();
        formatArgs.add(INSTANT);
        
        for (FieldDecl field : event.fields()) {
            TypeName fieldType = typeMapper.mapType(field.type());
            factoryMethod.addParameter(fieldType, field.name());
            args.append(", $N");
            formatArgs.add(field.name());
        }
        
        String format = "return new $T(" + args + ")";
        Object[] allArgs = new Object[formatArgs.size() + 1];
        allArgs[0] = ClassName.get(packageName, event.name());
        for (int i = 0; i < formatArgs.size(); i++) {
            allArgs[i + 1] = formatArgs.get(i);
        }
        
        factoryMethod.addStatement(format, allArgs);
        
        return factoryMethod.build();
    }
    
    private CodeBlock generateJavadoc(String documentation, String defaultDoc) {
        String doc = documentation != null && !documentation.isEmpty() 
            ? documentation 
            : defaultDoc;
        return CodeBlock.of("$L\n", doc);
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
