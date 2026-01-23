package uet.ndh.ddsl.core.building;

import lombok.Getter;
import lombok.Setter;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.building.logic.BusinessLogicGenerator;
import uet.ndh.ddsl.core.model.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a method in a domain object.
 */
@Getter
public class Method {
    private final String name;
    private final JavaType returnType;
    private final List<Parameter> parameters;
    private final Visibility visibility;
    private final boolean isStatic;
    private final boolean isFinal;
    @Setter
    private CodeBlock body;
    private final List<JavaType> throwsExceptions;

    // New fields for business logic generation
    @Setter
    private Entity entityContext;
    @Setter
    private boolean generateBusinessLogic = true;

    public Method(String name, JavaType returnType, Visibility visibility,
                  boolean isStatic, boolean isFinal) {
        this.name = name;
        this.returnType = returnType;
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.parameters = new ArrayList<>();
        this.throwsExceptions = new ArrayList<>();
        this.generateBusinessLogic = true;
    }

    public Method(String name, JavaType returnType) {
        this(name, returnType, Visibility.PUBLIC, false, false);
    }

    // Builder pattern implementation
    public static MethodBuilder builder() {
        return new MethodBuilder();
    }

    public static class MethodBuilder {
        private String name;
        private JavaType returnType;
        private Visibility visibility = Visibility.PUBLIC;
        private boolean isStatic = false;
        private boolean isFinal = false;
        private CodeBlock body;
        private Entity entityContext;
        private boolean generateBusinessLogic = true;

        public MethodBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MethodBuilder returnType(JavaType returnType) {
            this.returnType = returnType;
            return this;
        }

        public MethodBuilder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public MethodBuilder isStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        public MethodBuilder isFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public MethodBuilder body(CodeBlock body) {
            this.body = body;
            return this;
        }

        public MethodBuilder entityContext(Entity entityContext) {
            this.entityContext = entityContext;
            return this;
        }

        public MethodBuilder generateBusinessLogic(boolean generateBusinessLogic) {
            this.generateBusinessLogic = generateBusinessLogic;
            return this;
        }

        public Method build() {
            Method method = new Method(name, returnType, visibility, isStatic, isFinal);
            method.setBody(body);
            method.setEntityContext(entityContext);
            method.setGenerateBusinessLogic(generateBusinessLogic);
            return method;
        }
    }

    public List<Parameter> getParameters() {
        return new ArrayList<>(parameters);
    }

    public List<JavaType> getThrowsExceptions() {
        return new ArrayList<>(throwsExceptions);
    }

    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }

    public void addThrowsException(JavaType exceptionType) {
        throwsExceptions.add(exceptionType);
    }

    /**
     * Generate method signature.
     * @return Java method signature
     */
    public String generateMethodSignature() {
        StringBuilder sb = new StringBuilder();

        // Visibility
        switch (visibility) {
            case PRIVATE: sb.append("private "); break;
            case PROTECTED: sb.append("protected "); break;
            case PUBLIC: sb.append("public "); break;
            case PACKAGE_PRIVATE: break;
        }

        // Modifiers
        if (isStatic) sb.append("static ");
        if (isFinal) sb.append("final ");

        // Return type
        sb.append(returnType.getSimpleName()).append(" ");

        // Method name
        sb.append(name).append("(");

        // Parameters
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).generateParameterDeclaration());
        }
        sb.append(")");

        // Throws clause
        if (!throwsExceptions.isEmpty()) {
            sb.append(" throws ");
            for (int i = 0; i < throwsExceptions.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(throwsExceptions.get(i).getSimpleName());
            }
        }

        return sb.toString();
    }

    /**
     * Generate complete method including body.
     * @return Complete Java method
     */
    public String generateMethodBody() {
        StringBuilder sb = new StringBuilder();
        sb.append(generateMethodSignature()).append(" {\n");

        if (body != null && !body.isEmpty()) {
            // Use existing body
            sb.append(body.generateCode());
        } else if (generateBusinessLogic) {
            // Generate business logic automatically
            CodeBlock generatedBody = generateAutomaticBusinessLogic();
            sb.append(generatedBody.generateCode());
        } else {
            // Default TODO implementation
            if (returnType.getSimpleName().equals("void")) {
                sb.append("        // TODO: Implement method\n");
            } else {
                sb.append("        // TODO: Implement method\n");
                sb.append("        return null;\n");
            }
        }

        sb.append("    }");
        return sb.toString();
    }

    /**
     * Generate business logic automatically based on method name and context.
     */
    private CodeBlock generateAutomaticBusinessLogic() {
        BusinessLogicGenerator generator = new BusinessLogicGenerator();

        // Get parameter names
        List<String> parameterNames = parameters.stream()
            .map(Parameter::name)
            .toList();

        // Generate logic based on method context
        var statements = generator.generateMethodLogic(
            name,
            returnType.getSimpleName(),
            parameterNames,
            entityContext
        );

        return new CodeBlock(statements);
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Method name cannot be empty", location));
        }

        if (returnType == null) {
            errors.add(new ValidationError("Return type cannot be null", location));
        }

        return errors;
    }

    /**
     * Create a deep copy of this method for normalization.
     */
    public Method copy() {
        Method copy = new Method(this.name, this.returnType, this.visibility,
                                this.isStatic, this.isFinal);

        // Copy all parameters
        for (Parameter param : this.parameters) {
            copy.addParameter(param.copy());
        }

        // Copy throws exceptions
        for (JavaType exception : this.throwsExceptions) {
            copy.addThrowsException(exception);
        }

        // Copy body if present
        if (this.body != null) {
            copy.setBody(this.body.copy());
        }

        copy.setEntityContext(this.entityContext);
        copy.setGenerateBusinessLogic(this.generateBusinessLogic);

        return copy;
    }
}
