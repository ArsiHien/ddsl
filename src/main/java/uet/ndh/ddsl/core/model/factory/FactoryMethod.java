package uet.ndh.ddsl.core.model.factory;

import lombok.Getter;
import lombok.Setter;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.building.CodeBlock;
import uet.ndh.ddsl.core.building.Parameter;

import java.util.ArrayList;
import java.util.List; /**
 * Represents a factory method that creates domain objects.
 */
public class FactoryMethod {
    @Getter
    private final String name;
    private final List<Parameter> parameters;
    @Getter
    private final JavaType returnType;
    @Setter
    @Getter
    private CodeBlock creationLogic;

    public FactoryMethod(String name, JavaType returnType) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = new ArrayList<>();
    }

    public List<Parameter> getParameters() {
        return new ArrayList<>(parameters);
    }

    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }

    /**
     * Create a copy of this factory method.
     */
    public FactoryMethod copy() {
        FactoryMethod copy = new FactoryMethod(this.name, this.returnType);
        for (Parameter param : this.parameters) {
            copy.addParameter(param.copy());
        }
        if (this.creationLogic != null) {
            copy.setCreationLogic(this.creationLogic.copy());
        }
        return copy;
    }

    /**
     * Generate factory method code.
     * @return Complete Java factory method
     */
    public String generateFactoryMethod() {
        StringBuilder sb = new StringBuilder();
        sb.append("public static ").append(returnType.getSimpleName()).append(" ").append(name).append("(");

        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).generateParameterDeclaration());
        }

        sb.append(") {\n");

        if (creationLogic != null) {
            sb.append(creationLogic.generateCode());
        } else {
            sb.append("    // TODO: Implement factory method\n");
            sb.append("    return new ").append(returnType.getSimpleName()).append("();\n");
        }

        sb.append("}");
        return sb.toString();
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Factory method name cannot be empty", location));
        }

        if (returnType == null) {
            errors.add(new ValidationError("Factory method return type cannot be null", location));
        }

        return errors;
    }
}
