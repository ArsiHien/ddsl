package uet.ndh.ddsl.codegen;

import uet.ndh.ddsl.core.CodeGenVisitor;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.application.applicationservice.ApplicationService;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.building.Visibility;
import uet.ndh.ddsl.core.codegen.*;
import uet.ndh.ddsl.core.model.*;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.factory.Factory;
import uet.ndh.ddsl.core.model.repository.RepositoryInterface;
import uet.ndh.ddsl.core.model.specification.Specification;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;
import uet.ndh.ddsl.core.type.PrimitiveKind;
import uet.ndh.ddsl.core.type.PrimitiveType;

import java.util.List;

/**
 * Generates Java code from domain model AST using visitor pattern.
 */
public class JavaCodeGenVisitor implements CodeGenVisitor {

    private final CodeArtifacts codeArtifacts;
    private String currentPackage;

    public JavaCodeGenVisitor() {
        PackageStructure defaultPackageStructure = new PackageStructure("");
        this.codeArtifacts = new CodeArtifacts(defaultPackageStructure);
    }

    public CodeArtifacts getCodeArtifacts() {
        return codeArtifacts;
    }

    @Override
    public void visitDomainModel(DomainModel model) {
        // Set up package structure
        PackageStructure packageStructure = new PackageStructure(model.getBasePackage());
        // Update the existing artifacts with new package structure

        // Visit all bounded contexts
        for (BoundedContext context : model.getBoundedContexts()) {
            context.accept(this);
        }
    }

    @Override
    public void visitBoundedContext(BoundedContext context) {
        // Set current package context
        currentPackage = context.getPackageName();

        // Visit all domain objects
        for (ValueObject vo : context.getValueObjects()) {
            vo.accept(this);
        }

        for (Aggregate aggregate : context.getAggregates()) {
            aggregate.accept(this);
        }

        for (DomainService service : context.getDomainServices()) {
            service.accept(this);
        }

        for (DomainEvent event : context.getDomainEvents()) {
            event.accept(this);
        }

        for (RepositoryInterface repository : context.getRepositories()) {
            repository.accept(this);
        }

        for (Factory factory : context.getFactories()) {
            factory.accept(this);
        }

        for (ApplicationService appService : context.getApplicationServices()) {
            appService.accept(this);
        }
    }

    @Override
    public void visitValueObject(ValueObject valueObject) {
        String packageName = currentPackage + ".domain.model";
        JavaClass javaClass = new JavaClass(packageName, valueObject.getName());

        // Add fields
        for (Field field : valueObject.getFields()) {
            javaClass.addField(field);
        }

        // Generate constructor
        Constructor constructor = generateValueObjectConstructor(valueObject);
        javaClass.addConstructor(constructor);

        // Add methods
        for (uet.ndh.ddsl.core.building.Method method : valueObject.getMethods()) {
            javaClass.addMethod(method);
        }

        // Generate equals and hashCode
        uet.ndh.ddsl.core.building.Method equalsMethod = generateEqualsMethod(valueObject.getFields());
        uet.ndh.ddsl.core.building.Method hashCodeMethod = generateHashCodeMethod(valueObject.getFields());
        javaClass.addMethod(equalsMethod);
        javaClass.addMethod(hashCodeMethod);

        // Add imports
        addCommonImports(javaClass);

        codeArtifacts.addClass(javaClass);
    }

    @Override
    public void visitAggregate(Aggregate aggregate) {
        // The aggregate itself doesn't generate a separate class
        // Instead, generate the root entity and internal entities
        if (aggregate.getRoot() != null) {
            aggregate.getRoot().accept(this);
        }

        for (Entity entity : aggregate.getEntities()) {
            entity.accept(this);
        }
    }

    @Override
    public void visitEntity(Entity entity) {
        String packageName = currentPackage + ".domain.model";
        JavaClass javaClass = new JavaClass(packageName, entity.getName());

        // Generate ID class first
        generateIdClass(entity, packageName);

        // Add ID field
        JavaType idType = new JavaType(entity.getName() + "Id", packageName);
        Field idField = new Field(entity.getIdentityField().name(), idType,
            Visibility.PRIVATE, true, false, null);
        javaClass.addField(idField);

        // Add other fields
        for (Field field : entity.getFields()) {
            javaClass.addField(field);
        }

        // Generate constructor
        Constructor constructor = generateEntityConstructor(entity);
        javaClass.addConstructor(constructor);

        // Add methods with entity context for business logic generation
        for (uet.ndh.ddsl.core.building.Method method : entity.getMethods()) {
            // Set entity context for automatic business logic generation
            method.setEntityContext(entity);
            method.setGenerateBusinessLogic(true);
            javaClass.addMethod(method);
        }

        // Generate getters
        generateGetters(javaClass, entity.getFields());

        // Generate equals and hashCode based on ID
        uet.ndh.ddsl.core.building.Method equalsMethod = generateEqualsMethodById(entity);
        uet.ndh.ddsl.core.building.Method hashCodeMethod = generateHashCodeMethodById(entity);
        javaClass.addMethod(equalsMethod);
        javaClass.addMethod(hashCodeMethod);

        // Add imports for business logic features
        addCommonImports(javaClass);
        addBusinessLogicImports(javaClass);

        codeArtifacts.addClass(javaClass);
    }

    @Override
    public void visitDomainService(DomainService service) {
        String packageName = currentPackage + ".domain.service";

        if (service.isInterface()) {
            JavaInterface javaInterface = new JavaInterface(packageName, service.getName());

            for (uet.ndh.ddsl.core.building.Method method : service.getMethods()) {
                javaInterface.addMethod(method);
            }

            codeArtifacts.addInterface(javaInterface);
        } else {
            JavaClass javaClass = new JavaClass(packageName, service.getName());

            for (uet.ndh.ddsl.core.building.Method method : service.getMethods()) {
                // Enable business logic generation for domain service methods
                method.setGenerateBusinessLogic(true);
                // Note: Domain services typically don't have entity context
                // but they can still generate meaningful logic
                javaClass.addMethod(method);
            }

            addCommonImports(javaClass);
            addBusinessLogicImports(javaClass);
            codeArtifacts.addClass(javaClass);
        }
    }

    @Override
    public void visitDomainEvent(DomainEvent event) {
        String packageName = currentPackage + ".domain.event";
        JavaClass javaClass = new JavaClass(packageName, event.getName());

        // Add fields
        for (Field field : event.getFields()) {
            javaClass.addField(field);
        }

        // Generate constructor
        Constructor constructor = generateValueObjectConstructor(event);
        javaClass.addConstructor(constructor);

        // Generate getters
        generateGetters(javaClass, event.getFields());

        addCommonImports(javaClass);
        codeArtifacts.addClass(javaClass);
    }

    @Override
    public void visitRepositoryInterface(RepositoryInterface repository) {
        String packageName = currentPackage + ".domain.repository";
        JavaInterface javaInterface = new JavaInterface(packageName, repository.getName());

        // Add methods from repository
        for (uet.ndh.ddsl.core.model.repository.RepositoryMethod repoMethod : repository.getMethods()) {
            uet.ndh.ddsl.core.building.Method method = new uet.ndh.ddsl.core.building.Method(repoMethod.getName(), repoMethod.getReturnType());
            repoMethod.getParameters().forEach(method::addParameter);
            javaInterface.addMethod(method);
        }

        codeArtifacts.addInterface(javaInterface);
    }

    @Override
    public void visitFactory(Factory factory) {
        String packageName = currentPackage + ".domain.factory";
        JavaClass javaClass = new JavaClass(packageName, factory.getName());

        // Convert factory methods to regular methods
        for (uet.ndh.ddsl.core.model.factory.FactoryMethod factoryMethod : factory.getMethods()) {
            uet.ndh.ddsl.core.building.Method method = new uet.ndh.ddsl.core.building.Method(factoryMethod.getName(), factoryMethod.getReturnType());
            factoryMethod.getParameters().forEach(method::addParameter);

            // Add method body if available
            if (factoryMethod.getCreationLogic() != null) {
                method.setBody(factoryMethod.getCreationLogic());
            }

            javaClass.addMethod(method);
        }

        addCommonImports(javaClass);
        codeArtifacts.addClass(javaClass);
    }

    @Override
    public void visitSpecification(Specification specification) {
        String packageName = currentPackage + ".domain.specification";
        JavaInterface javaInterface = new JavaInterface(packageName, specification.getName());

        // Add isSatisfiedBy method
        JavaType booleanType = new PrimitiveType(PrimitiveKind.BOOLEAN);
        uet.ndh.ddsl.core.building.Method method = new uet.ndh.ddsl.core.building.Method("isSatisfiedBy", booleanType);
        method.addParameter(new uet.ndh.ddsl.core.building.Parameter("candidate",
            specification.getTargetType(), false));

        javaInterface.addMethod(method);
        codeArtifacts.addInterface(javaInterface);
    }

    @Override
    public void visitApplicationService(ApplicationService service) {
        String packageName = currentPackage + ".application.service";
        JavaClass javaClass = new JavaClass(packageName, service.getName());

        // Add dependency fields
        for (uet.ndh.ddsl.core.application.applicationservice.Dependency dep : service.getDependencies()) {
            Field depField = new Field(dep.getName(), dep.getType(),
                Visibility.PRIVATE, true, false, null);
            javaClass.addField(depField);
        }

        // Generate constructor with dependencies
        Constructor constructor = generateApplicationServiceConstructor(service);
        javaClass.addConstructor(constructor);

        // Generate use case methods
        for (uet.ndh.ddsl.core.application.applicationservice.UseCase useCase : service.getUseCases()) {
            uet.ndh.ddsl.core.building.Method useCaseMethod = generateUseCaseMethod(useCase);
            javaClass.addMethod(useCaseMethod);
        }

        // Generate DTOs
        for (uet.ndh.ddsl.core.application.applicationservice.UseCase useCase : service.getUseCases()) {
            if (useCase.getInputDto() != null) {
                generateDto(useCase.getInputDto(), currentPackage + ".application.dto");
            }
            if (useCase.getOutputDto() != null) {
                generateDto(useCase.getOutputDto(), currentPackage + ".application.dto");
            }
        }

        addCommonImports(javaClass);
        codeArtifacts.addClass(javaClass);
    }

    @Override
    public void visitMethod(Method method) {
        // Methods are handled in their containing classes
    }

    @Override
    public void visitField(Field field) {
        // Fields are handled in their containing classes
    }

    // Helper methods

    private void generateIdClass(Entity entity, String packageName) {
        JavaClass idClass = new JavaClass(packageName, entity.getName() + "Id");

        // Add value field based on identity type
        JavaType valueType = getJavaTypeForIdentityType(entity.getIdentityField().type());
        Field valueField = new Field("value", valueType, Visibility.PRIVATE, true, false, null);
        idClass.addField(valueField);

        // Generate constructor
        Constructor constructor = new Constructor(Constructor.Visibility.PUBLIC);
        constructor.addParameter(new uet.ndh.ddsl.core.building.Parameter("value", valueType, false));
        constructor.setBody(new uet.ndh.ddsl.core.building.CodeBlock());
        constructor.getBody().addRawCode("this.value = value;");
        idClass.addConstructor(constructor);

        // Generate static factory methods
        Method generateMethod = new Method("generate", new JavaType(entity.getName() + "Id", packageName),
            Visibility.PUBLIC, true, false);
        generateMethod.setBody(new uet.ndh.ddsl.core.building.CodeBlock());
        generateMethod.getBody().addRawCode("return new " + entity.getName() + "Id(UUID.randomUUID());");
        idClass.addMethod(generateMethod);

        Method ofMethod = new Method("of", new JavaType(entity.getName() + "Id", packageName),
            Visibility.PUBLIC, true, false);
        ofMethod.addParameter(new uet.ndh.ddsl.core.building.Parameter("value", valueType, false));
        ofMethod.setBody(new uet.ndh.ddsl.core.building.CodeBlock());
        ofMethod.getBody().addRawCode("return new " + entity.getName() + "Id(value);");
        idClass.addMethod(ofMethod);

        // Generate getter
        Method getter = new Method("getValue", valueType, Visibility.PUBLIC, false, false);
        getter.setBody(new uet.ndh.ddsl.core.building.CodeBlock());
        getter.getBody().addRawCode("return value;");
        idClass.addMethod(getter);

        // Generate equals and hashCode
        List<Field> fields = List.of(valueField);
        Method equalsMethod = generateEqualsMethod(fields);
        Method hashCodeMethod = generateHashCodeMethod(fields);
        idClass.addMethod(equalsMethod);
        idClass.addMethod(hashCodeMethod);

        addCommonImports(idClass);
        codeArtifacts.addClass(idClass);
    }

    private JavaType getJavaTypeForIdentityType(uet.ndh.ddsl.core.model.entity.IdentityType idType) {
        return switch (idType) {
            case UUID -> new PrimitiveType(PrimitiveKind.UUID);
            case LONG -> new PrimitiveType(PrimitiveKind.LONG);
            case STRING -> new PrimitiveType(PrimitiveKind.STRING);
            default -> new PrimitiveType(PrimitiveKind.UUID);
        };
    }

    private Constructor generateValueObjectConstructor(ValueObject valueObject) {
        Constructor constructor = new Constructor(Constructor.Visibility.PUBLIC);

        for (Field field : valueObject.getFields()) {
            constructor.addParameter(new uet.ndh.ddsl.core.building.Parameter(
                field.getName(), field.getType(), false));
        }

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        for (Field field : valueObject.getFields()) {
            body.addRawCode("this." + field.getName() + " = " + field.getName() + ";");
        }
        constructor.setBody(body);

        return constructor;
    }

    private Constructor generateValueObjectConstructor(DomainEvent event) {
        Constructor constructor = new Constructor(Constructor.Visibility.PUBLIC);

        for (Field field : event.getFields()) {
            constructor.addParameter(new uet.ndh.ddsl.core.building.Parameter(
                field.getName(), field.getType(), false));
        }

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        for (Field field : event.getFields()) {
            body.addRawCode("this." + field.getName() + " = " + field.getName() + ";");
        }
        constructor.setBody(body);

        return constructor;
    }

    private Constructor generateEntityConstructor(Entity entity) {
        Constructor constructor = new Constructor(Constructor.Visibility.PUBLIC);

        // Add ID parameter
        JavaType idType = new JavaType(entity.getName() + "Id", currentPackage + ".domain.model");
        constructor.addParameter(new uet.ndh.ddsl.core.building.Parameter(
            entity.getIdentityField().name(), idType, false));

        // Add other field parameters
        for (Field field : entity.getFields()) {
            if (!field.getName().equals("id")) {
                constructor.addParameter(new uet.ndh.ddsl.core.building.Parameter(
                    field.getName(), field.getType(), false));
            }
        }

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        body.addRawCode("this." + entity.getIdentityField().name() + " = " +
            entity.getIdentityField().name() + ";");

        for (Field field : entity.getFields()) {
            if (!field.getName().equals("id")) {
                body.addRawCode("this." + field.getName() + " = " + field.getName() + ";");
            }
        }
        constructor.setBody(body);

        return constructor;
    }

    private Constructor generateApplicationServiceConstructor(ApplicationService service) {
        Constructor constructor = new Constructor(Constructor.Visibility.PUBLIC);

        for (uet.ndh.ddsl.core.application.applicationservice.Dependency dep : service.getDependencies()) {
            constructor.addParameter(new uet.ndh.ddsl.core.building.Parameter(
                dep.getName(), dep.getType(), false));
        }

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        for (uet.ndh.ddsl.core.application.applicationservice.Dependency dep : service.getDependencies()) {
            body.addRawCode("this." + dep.getName() + " = " + dep.getName() + ";");
        }
        constructor.setBody(body);

        return constructor;
    }

    private Method generateUseCaseMethod(uet.ndh.ddsl.core.application.applicationservice.UseCase useCase) {
        JavaType returnType = useCase.getOutputDto() != null ?
            new JavaType(useCase.getOutputDto().getName(), currentPackage + ".application.dto") :
            new PrimitiveType(PrimitiveKind.STRING); // void

        Method method = new Method(useCase.getName(), returnType, Visibility.PUBLIC, false, false);

        if (useCase.getInputDto() != null) {
            JavaType inputType = new JavaType(useCase.getInputDto().getName(),
                currentPackage + ".application.dto");
            method.addParameter(new uet.ndh.ddsl.core.building.Parameter(
                "command", inputType, false));
        }

        // Generate method body with basic structure
        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        body.addRawCode("// TODO: Implement " + useCase.getName());
        if (useCase.getOutputDto() != null) {
            body.addRawCode("return null; // TODO: Return proper result");
        }
        method.setBody(body);

        return method;
    }

    private void generateDto(uet.ndh.ddsl.core.application.applicationservice.DataTransferObject dto,
            String packageName) {
        JavaClass javaClass = new JavaClass(packageName, dto.getName());

        // Add fields
        for (Field field : dto.getFields()) {
            javaClass.addField(field);
        }

        // Generate constructor
        Constructor constructor = new Constructor(Constructor.Visibility.PUBLIC);
        for (Field field : dto.getFields()) {
            constructor.addParameter(new uet.ndh.ddsl.core.building.Parameter(
                field.getName(), field.getType(), false));
        }

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        for (Field field : dto.getFields()) {
            body.addRawCode("this." + field.getName() + " = " + field.getName() + ";");
        }
        constructor.setBody(body);
        javaClass.addConstructor(constructor);

        // Generate getters
        generateGetters(javaClass, dto.getFields());

        addCommonImports(javaClass);
        codeArtifacts.addClass(javaClass);
    }

    private void generateGetters(JavaClass javaClass, List<Field> fields) {
        for (Field field : fields) {
            String getterName = "get" + capitalize(field.getName());
            Method getter = new Method(getterName, field.getType(), Visibility.PUBLIC, false, false);
            getter.setBody(new uet.ndh.ddsl.core.building.CodeBlock());
            getter.getBody().addRawCode("return " + field.getName() + ";");
            javaClass.addMethod(getter);
        }
    }

    private Method generateEqualsMethod(List<Field> fields) {
        JavaType booleanType = new PrimitiveType(PrimitiveKind.BOOLEAN);
        JavaType objectType = new JavaType("Object", "java.lang");

        Method equals = new Method("equals", booleanType, Visibility.PUBLIC, false, false);
        equals.addParameter(new uet.ndh.ddsl.core.building.Parameter("obj", objectType, false));

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        body.addRawCode("if (this == obj) return true;");
        body.addRawCode("if (obj == null || getClass() != obj.getClass()) return false;");

        if (!fields.isEmpty()) {
            String className = ""; // We'd need to pass this or get it from context
            body.addRawCode("// TODO: Implement proper equals method");
        }

        body.addRawCode("return true;");
        equals.setBody(body);

        return equals;
    }

    private Method generateHashCodeMethod(List<Field> fields) {
        PrimitiveType intType = new PrimitiveType(PrimitiveKind.INT);
        Method hashCode = new Method("hashCode", intType, Visibility.PUBLIC, false, false);

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        body.addRawCode("// TODO: Implement proper hashCode method");
        body.addRawCode("return Objects.hash(" +
            String.join(", ", fields.stream().map(Field::getName).toArray(String[]::new)) + ");");
        hashCode.setBody(body);

        return hashCode;
    }

    private Method generateEqualsMethodById(Entity entity) {
        JavaType booleanType = new PrimitiveType(PrimitiveKind.BOOLEAN);
        JavaType objectType = new JavaType("Object", "java.lang");

        Method equals = new Method("equals", booleanType, Visibility.PUBLIC, false, false);
        equals.addParameter(new uet.ndh.ddsl.core.building.Parameter("obj", objectType, false));

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        body.addRawCode("if (this == obj) return true;");
        body.addRawCode("if (obj == null || getClass() != obj.getClass()) return false;");
        body.addRawCode(entity.getName() + " that = (" + entity.getName() + ") obj;");
        body.addRawCode("return Objects.equals(" + entity.getIdentityField().name() +
            ", that." + entity.getIdentityField().name() + ");");
        equals.setBody(body);

        return equals;
    }

    private Method generateHashCodeMethodById(Entity entity) {
        PrimitiveType intType = new PrimitiveType(PrimitiveKind.INT);
        Method hashCode = new Method("hashCode", intType, Visibility.PUBLIC, false, false);

        uet.ndh.ddsl.core.building.CodeBlock body = new uet.ndh.ddsl.core.building.CodeBlock();
        body.addRawCode("return Objects.hash(" + entity.getIdentityField().name() + ");");
        hashCode.setBody(body);

        return hashCode;
    }

    private void addCommonImports(JavaClass javaClass) {
        javaClass.addImport("java.util.*");
        javaClass.addImport("java.time.*");
        javaClass.addImport("java.util.Objects");
    }

    private void addBusinessLogicImports(JavaClass javaClass) {
        // Add imports needed for business logic generation
        javaClass.addImport("java.util.stream.Collectors");
        javaClass.addImport("java.util.Comparator");
        javaClass.addImport("java.util.function.Function");
        javaClass.addImport("java.util.function.Predicate");
        javaClass.addImport("java.math.BigDecimal");
        javaClass.addImport("java.text.Normalizer");
        javaClass.addImport("java.util.regex.Pattern");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}


