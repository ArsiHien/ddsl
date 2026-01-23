package uet.ndh.ddsl.core.building.expression;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a variable reference expression.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VariableExpression extends Expression {
    private final String variableName;
    private final String type;

    public VariableExpression(String variableName, String type) {
        this.variableName = variableName;
        this.type = type;
    }

    // Backward compatibility constructor for cases where type is not needed
    public VariableExpression(String variableName) {
        this(variableName, "Object");
    }

    @Override
    public String generateCode() {
        return variableName;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Expression copy() {
        return new VariableExpression(this.variableName, this.type);
    }
}
