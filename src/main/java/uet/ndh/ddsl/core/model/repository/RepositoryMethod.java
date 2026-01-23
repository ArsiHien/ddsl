package uet.ndh.ddsl.core.model.repository;

import lombok.Getter;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.building.Parameter;

import java.util.ArrayList;
import java.util.List; /**
 * Represents a method in a repository interface.
 */
public class RepositoryMethod {
    @Getter
    private final String name;
    @Getter
    private final JavaType returnType;
    private final List<Parameter> parameters;
    private final RepositoryMethodType methodType;

    public RepositoryMethod(String name, JavaType returnType, RepositoryMethodType methodType) {
        this.name = name;
        this.returnType = returnType;
        this.methodType = methodType;
        this.parameters = new ArrayList<>();
    }

    public List<Parameter> getParameters() {
        return new ArrayList<>(parameters);
    }

    public RepositoryMethodType getMethodType() {
        return methodType;
    }

    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }

    /**
     * Create a copy of this repository method.
     */
    public RepositoryMethod copy() {
        RepositoryMethod copy = new RepositoryMethod(this.name, this.returnType, this.methodType);
        for (Parameter param : this.parameters) {
            copy.addParameter(param.copy());
        }
        return copy;
    }

    /**
     * Generate method signature for repository interface.
     * @return Java method signature
     */
    public String generateMethodSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType.getSimpleName()).append(" ").append(name).append("(");

        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).generateParameterDeclaration());
        }

        sb.append(");");
        return sb.toString();
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Repository method name cannot be empty", location));
        }

        if (returnType == null) {
            errors.add(new ValidationError("Repository method return type cannot be null", location));
        }

        if (methodType == null) {
            errors.add(new ValidationError("Repository method type cannot be null", location));
        }

        return errors;
    }
}
