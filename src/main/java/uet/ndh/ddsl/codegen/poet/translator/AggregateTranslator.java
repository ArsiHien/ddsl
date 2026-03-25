package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.*;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.common.Constraint;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
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
 * Translates AggregateDecl AST nodes to JavaPoet TypeSpecs.
 * 
 * This translator is responsible for generating:
 * - Aggregate root entity classes (implements AggregateRoot interface)
 * - Nested entity classes
 * - Value object records
 * - Domain event records
 * 
 * The generated aggregate root class:
 * - Implements the scaffold-generated AggregateRoot interface
 * - Contains identity field with proper equals/hashCode
 * - Has domain event collection for event sourcing
 * - Contains behavior methods generated via ExpressionTranslator
 * 
 * Usage:
 * <pre>
 * AggregateTranslator translator = new AggregateTranslator(typeMapper);
 * List<CodeArtifact> artifacts = translator.translate(aggregateDecl);
 * </pre>
 */
public class AggregateTranslator {
    
    private final TypeMapper typeMapper;
    private final ExpressionTranslator expressionTranslator;
    
    // Common type references
    private static final ClassName INSTANT = ClassName.get(Instant.class);
    private static final ClassName ARRAY_LIST = ClassName.get(ArrayList.class);
    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName OBJECTS = ClassName.get(Objects.class);
    
    public AggregateTranslator(TypeMapper typeMapper) {
        this.typeMapper = typeMapper;
        this.expressionTranslator = new ExpressionTranslator(typeMapper);
    }
    
    /**
     * Translate an AggregateDecl to a list of CodeArtifacts.
     * 
     * @param aggregate The aggregate declaration to translate
     * @return List of code artifacts (root entity, nested entities, value objects, events)
     */
    public List<CodeArtifact> translate(AggregateDecl aggregate) {
        List<CodeArtifact> artifacts = new ArrayList<>();
        String aggregatePackage = typeMapper.getBasePackage() + "." + aggregate.name().toLowerCase();
        
        // 1. Generate the aggregate root entity
        if (aggregate.root() != null) {
            JavaFile rootFile = translateAggregateRoot(aggregate, aggregatePackage);
            artifacts.add(new CodeArtifact(
                aggregate.root().name(),
                aggregatePackage,
                rootFile.toString(),
                CodeArtifact.ArtifactType.AGGREGATE_ROOT
            ));
        }
        
        // 2. Generate nested entities
        for (EntityDecl entity : aggregate.entities()) {
            JavaFile entityFile = translateEntity(entity, aggregatePackage);
            artifacts.add(new CodeArtifact(
                entity.name(),
                aggregatePackage,
                entityFile.toString(),
                CodeArtifact.ArtifactType.ENTITY
            ));
        }
        
        // 3. Generate value objects as records
        for (ValueObjectDecl valueObject : aggregate.valueObjects()) {
            JavaFile voFile = translateValueObject(valueObject, aggregatePackage);
            artifacts.add(new CodeArtifact(
                valueObject.name(),
                aggregatePackage,
                voFile.toString(),
                CodeArtifact.ArtifactType.VALUE_OBJECT
            ));
        }
        
        // 4. Generate domain events as records
        for (DomainEventDecl event : aggregate.events()) {
            JavaFile eventFile = translateDomainEvent(event, aggregatePackage);
            artifacts.add(new CodeArtifact(
                event.name(),
                aggregatePackage,
                eventFile.toString(),
                CodeArtifact.ArtifactType.DOMAIN_EVENT
            ));
        }
        
        return artifacts;
    }
    
    /**
     * Translate the aggregate root entity.
     */
    private JavaFile translateAggregateRoot(AggregateDecl aggregate, String packageName) {
        EntityDecl root = aggregate.root();
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(root.name())
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(typeMapper.getAggregateRootInterface())
            .addJavadoc(generateJavadoc(root.documentation(), "Aggregate Root for " + aggregate.name()));
        
        // Add identity field
        if (root.identity() != null) {
            classBuilder.addField(buildIdentityField(root.identity()));
        }
        
        // Add regular fields (skip identity field - already added above)
        String identityName = root.identity() != null ? root.identity().name() : null;
        for (FieldDecl field : root.fields()) {
            if (identityName != null && identityName.equals(field.name())) continue;
            classBuilder.addField(buildField(field));
        }
        
        // Add domain events collection
        ClassName domainEventInterface = typeMapper.getDomainEventInterface();
        TypeName eventListType = ParameterizedTypeName.get(LIST, domainEventInterface);
        FieldSpec eventsField = FieldSpec.builder(eventListType, "domainEvents", Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new $T<>()", ARRAY_LIST)
            .build();
        classBuilder.addField(eventsField);
        
        // Add constructor
        classBuilder.addMethod(buildAggregateConstructor(root));
        
        // Register field types for behavior parameter resolution
        // e.g. field "guest: GuestProfile" allows param "guest" to resolve to GuestProfile
        typeMapper.clearFieldTypes();
        for (FieldDecl field : root.fields()) {
            if (field.type() != null) {
                typeMapper.registerFieldType(field.name(), field.type().name());
            }
        }
        
        // Add behavior methods using ExpressionTranslator (deduplicate root/aggregate overlap)
        List<BehaviorDecl> mergedBehaviors = new ArrayList<>(root.behaviors());
        for (BehaviorDecl behavior : aggregate.behaviors()) {
            if (!mergedBehaviors.contains(behavior)) {
                mergedBehaviors.add(behavior);
            }
        }
        for (BehaviorDecl behavior : mergedBehaviors) {
            classBuilder.addMethod(expressionTranslator.translateBehavior(behavior));
        }
        
        // Add domain event methods
        classBuilder.addMethod(buildRegisterEventMethod(domainEventInterface));
        classBuilder.addMethod(buildGetDomainEventsMethod(eventListType));
        classBuilder.addMethod(buildClearDomainEventsMethod());
        
        // Add equals/hashCode based on identity
        if (root.identity() != null) {
            classBuilder.addMethod(buildEqualsMethod(root.name(), root.identity().name()));
            classBuilder.addMethod(buildHashCodeMethod(root.identity().name()));
        }
        
        // Add getId() method for AggregateRoot interface
        if (root.identity() != null) {
            classBuilder.addMethod(buildGetIdMethod(root.identity()));
        }
        
        return JavaFile.builder(packageName, classBuilder.build())
            .skipJavaLangImports(true)
            .indent("    ")
            .build();
    }
    
    /**
     * Translate a nested entity.
     */
    private JavaFile translateEntity(EntityDecl entity, String packageName) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(entity.name())
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(typeMapper.getEntityInterface())
            .addJavadoc(generateJavadoc(entity.documentation(), "Entity: " + entity.name()));
        
        // Add identity field
        if (entity.identity() != null) {
            classBuilder.addField(buildIdentityField(entity.identity()));
        }
        
        // Add regular fields (skip identity field - already added above)
        String identityName = entity.identity() != null ? entity.identity().name() : null;
        for (FieldDecl field : entity.fields()) {
            if (identityName != null && identityName.equals(field.name())) continue;
            classBuilder.addField(buildField(field));
        }
        
        // Add constructor
        classBuilder.addMethod(buildEntityConstructor(entity));
        
        // Add behavior methods
        for (BehaviorDecl behavior : entity.behaviors()) {
            classBuilder.addMethod(expressionTranslator.translateBehavior(behavior));
        }
        
        // Add equals/hashCode
        if (entity.identity() != null) {
            classBuilder.addMethod(buildEqualsMethod(entity.name(), entity.identity().name()));
            classBuilder.addMethod(buildHashCodeMethod(entity.identity().name()));
            classBuilder.addMethod(buildGetIdMethod(entity.identity()));
        }
        
        return JavaFile.builder(packageName, classBuilder.build())
            .skipJavaLangImports(true)
            .indent("    ")
            .build();
    }
    
    /**
     * Translate a value object as a Java record.
     */
    private JavaFile translateValueObject(ValueObjectDecl valueObject, String packageName) {
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
        
        // Add compact constructor for validation if there are field constraints or invariants
        boolean hasFieldConstraints = valueObject.fields().stream()
            .anyMatch(f -> !f.constraints().isEmpty());
        boolean hasInvariants = !valueObject.invariants().isEmpty();
        
        if (hasFieldConstraints || hasInvariants) {
            MethodSpec.Builder compactConstructor = MethodSpec.compactConstructorBuilder()
                .addModifiers(Modifier.PUBLIC);
            
            // Generate field constraint validations
            if (hasFieldConstraints) {
                compactConstructor.addCode("// Field constraint validations\n");
                for (FieldDecl field : valueObject.fields()) {
                    for (Constraint constraint : field.constraints()) {
                        compactConstructor.addCode(
                            generateConstraintValidation(field, constraint));
                    }
                }
                if (hasInvariants) {
                    compactConstructor.addCode("\n");
                }
            }
            
            // Generate validation code from invariants
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
    
    /**
     * Translate a domain event as a Java record.
     */
    private JavaFile translateDomainEvent(DomainEventDecl event, String packageName) {
        // Build record constructor with components
        MethodSpec.Builder recordConstructorBuilder = MethodSpec.constructorBuilder();
        
        // Add occurredAt timestamp as first component
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
        
        // Add factory method with auto-timestamp
        MethodSpec.Builder factoryMethod = MethodSpec.methodBuilder("now")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(ClassName.get(packageName, event.name()));
        
        // Add parameters for payload fields
        List<String> argNames = new ArrayList<>();
        argNames.add("$T.now()");
        List<Object> args = new ArrayList<>();
        args.add(INSTANT);
        
        for (FieldDecl field : event.fields()) {
            TypeName fieldType = typeMapper.mapType(field.type());
            factoryMethod.addParameter(fieldType, field.name());
            argNames.add("$N");
            args.add(field.name());
        }
        
        String format = "return new $T(" + String.join(", ", argNames) + ")";
        Object[] formatArgs = new Object[args.size() + 1];
        formatArgs[0] = ClassName.get(packageName, event.name());
        for (int i = 0; i < args.size(); i++) {
            formatArgs[i + 1] = args.get(i);
        }
        factoryMethod.addStatement(format, formatArgs);
        
        recordBuilder.addMethod(factoryMethod.build());
        
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
    
    private MethodSpec buildAggregateConstructor(EntityDecl root) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC);
        
        // Add identity parameter
        if (root.identity() != null) {
            TypeName idType = typeMapper.mapType(root.identity().type());
            constructor.addParameter(idType, root.identity().name());
            constructor.addStatement("this.$N = $N", root.identity().name(), root.identity().name());
        }
        
        // Add field parameters (skip identity field - already added above)
        String identityName = root.identity() != null ? root.identity().name() : null;
        for (FieldDecl field : root.fields()) {
            if (identityName != null && identityName.equals(field.name())) continue;
            TypeName fieldType = typeMapper.mapType(field.type());
            constructor.addParameter(fieldType, field.name());
            constructor.addStatement("this.$N = $N", field.name(), field.name());
        }
        
        return constructor.build();
    }
    
    private MethodSpec buildEntityConstructor(EntityDecl entity) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC);
        
        if (entity.identity() != null) {
            TypeName idType = typeMapper.mapType(entity.identity().type());
            constructor.addParameter(idType, entity.identity().name());
            constructor.addStatement("this.$N = $N", entity.identity().name(), entity.identity().name());
        }
        
        String identityName = entity.identity() != null ? entity.identity().name() : null;
        for (FieldDecl field : entity.fields()) {
            if (identityName != null && identityName.equals(field.name())) continue;
            TypeName fieldType = typeMapper.mapType(field.type());
            constructor.addParameter(fieldType, field.name());
            constructor.addStatement("this.$N = $N", field.name(), field.name());
        }
        
        return constructor.build();
    }
    
    private MethodSpec buildRegisterEventMethod(ClassName domainEventInterface) {
        return MethodSpec.methodBuilder("registerEvent")
            .addModifiers(Modifier.PROTECTED)
            .addParameter(domainEventInterface, "event")
            .addStatement("this.domainEvents.add(event)")
            .build();
    }
    
    private MethodSpec buildGetDomainEventsMethod(TypeName eventListType) {
        return MethodSpec.methodBuilder("getDomainEvents")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(eventListType)
            .addStatement("return $T.copyOf(this.domainEvents)", LIST)
            .build();
    }
    
    private MethodSpec buildClearDomainEventsMethod() {
        return MethodSpec.methodBuilder("clearDomainEvents")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addStatement("this.domainEvents.clear()")
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
    
    private CodeBlock generateJavadoc(String documentation, String defaultDoc) {
        String doc = documentation != null && !documentation.isEmpty() 
            ? documentation 
            : defaultDoc;
        return CodeBlock.of("$L\n", doc);
    }
    
    /**
     * Generate validation code for a single field constraint annotation.
     */
    private CodeBlock generateConstraintValidation(FieldDecl field, Constraint constraint) {
        String fieldName = field.name();
        ClassName illegalArg = ClassName.get(IllegalArgumentException.class);
        
        return switch (constraint.type()) {
            case REQUIRED, NOT_NULL -> CodeBlock.builder()
                .beginControlFlow("if ($N == null)", fieldName)
                .addStatement("throw new $T($S)", illegalArg, 
                    fieldName + " is required")
                .endControlFlow()
                .build();
                
            case NOT_EMPTY -> CodeBlock.builder()
                .beginControlFlow("if ($N == null || $N.isEmpty())", fieldName, fieldName)
                .addStatement("throw new $T($S)", illegalArg, 
                    fieldName + " must not be empty")
                .endControlFlow()
                .build();
                
            case NOT_BLANK -> CodeBlock.builder()
                .beginControlFlow("if ($N == null || $N.isBlank())", fieldName, fieldName)
                .addStatement("throw new $T($S)", illegalArg, 
                    fieldName + " must not be blank")
                .endControlFlow()
                .build();
            
            case MAX_LENGTH -> {
                String maxVal = extractConstraintValue(constraint);
                yield CodeBlock.builder()
                    .beginControlFlow("if ($N != null && $N.length() > $L)", fieldName, fieldName, maxVal)
                    .addStatement("throw new $T($S)", illegalArg, 
                        fieldName + " must not exceed " + maxVal + " characters")
                    .endControlFlow()
                    .build();
            }
            
            case MIN_LENGTH -> {
                String minVal = extractConstraintValue(constraint);
                yield CodeBlock.builder()
                    .beginControlFlow("if ($N != null && $N.length() < $L)", fieldName, fieldName, minVal)
                    .addStatement("throw new $T($S)", illegalArg, 
                        fieldName + " must be at least " + minVal + " characters")
                    .endControlFlow()
                    .build();
            }
            
            case MIN -> {
                String minVal = extractConstraintValue(constraint);
                yield CodeBlock.builder()
                    .beginControlFlow("if ($N < $L)", fieldName, minVal)
                    .addStatement("throw new $T($S)", illegalArg, 
                        fieldName + " must be at least " + minVal)
                    .endControlFlow()
                    .build();
            }
            
            case MAX -> {
                String maxVal = extractConstraintValue(constraint);
                yield CodeBlock.builder()
                    .beginControlFlow("if ($N > $L)", fieldName, maxVal)
                    .addStatement("throw new $T($S)", illegalArg, 
                        fieldName + " must not exceed " + maxVal)
                    .endControlFlow()
                    .build();
            }
            
            case POSITIVE -> CodeBlock.builder()
                .beginControlFlow("if ($N <= 0)", fieldName)
                .addStatement("throw new $T($S)", illegalArg, 
                    fieldName + " must be positive")
                .endControlFlow()
                .build();
                
            case NEGATIVE -> CodeBlock.builder()
                .beginControlFlow("if ($N >= 0)", fieldName)
                .addStatement("throw new $T($S)", illegalArg, 
                    fieldName + " must be negative")
                .endControlFlow()
                .build();
            
            case PATTERN -> {
                String pattern = extractConstraintStringValue(constraint);
                yield CodeBlock.builder()
                    .beginControlFlow("if ($N != null && !$N.matches($S))", fieldName, fieldName, pattern)
                    .addStatement("throw new $T($S)", illegalArg, 
                        fieldName + " does not match required pattern")
                    .endControlFlow()
                    .build();
            }
            
            case EMAIL -> CodeBlock.builder()
                .beginControlFlow("if ($N != null && !$N.matches($S))", fieldName, fieldName, 
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
                .addStatement("throw new $T($S)", illegalArg, 
                    fieldName + " must be a valid email address")
                .endControlFlow()
                .build();
            
            case UNIQUE -> CodeBlock.builder()
                .add("// @unique constraint for $N enforced at repository/persistence level\n", fieldName)
                .build();
                
            case IDENTITY, IMMUTABLE, COMPUTED, DEFAULT -> 
                CodeBlock.builder().build();
                
            default -> CodeBlock.builder()
                .add("// TODO: Validate $N constraint $L\n", fieldName, constraint.type())
                .build();
        };
    }
    
    private String extractConstraintValue(Constraint constraint) {
        if (constraint.value() != null) {
            return expressionTranslator.translateExpression(constraint.value()).toString();
        }
        return "0";
    }
    
    private String extractConstraintStringValue(Constraint constraint) {
        if (constraint.value() != null) {
            String val = expressionTranslator.translateExpression(constraint.value()).toString();
            if (val.startsWith("\"") && val.endsWith("\"")) {
                return val.substring(1, val.length() - 1);
            }
            return val;
        }
        return "";
    }
}
