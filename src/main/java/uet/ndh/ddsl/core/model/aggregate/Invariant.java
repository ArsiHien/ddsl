package uet.ndh.ddsl.core.model.aggregate;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.building.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an invariant constraint in an aggregate.
 */
public record Invariant(String name, String description, Expression condition, String errorMessage) {

    /**
     * Generate invariant check method.
     * @return Java method for checking this invariant
     */
    public String generateInvariantCheck() {
        StringBuilder sb = new StringBuilder();
        sb.append("private void check").append(name).append("() {\n");
        sb.append("    if (!(").append(condition.generateCode()).append(")) {\n");
        sb.append("        throw new IllegalStateException(\"").append(errorMessage).append("\");\n");
        sb.append("    }\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Create a copy of this invariant.
     */
    public Invariant copy() {
        return new Invariant(this.name, this.description, this.condition.copy(), this.errorMessage);
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Invariant name cannot be empty", location));
        }

        if (condition == null) {
            errors.add(new ValidationError("Invariant condition cannot be null", location));
        }

        return errors;
    }
}
