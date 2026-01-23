package uet.ndh.ddsl.core.building.expression;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Literal value expression (string, number, boolean, etc.).
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LiteralExpression extends Expression {
    private final Object value;
    private final String type;

    public LiteralExpression(Object value, String type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public String generateCode() {
        return switch (value) {
            case null -> "null";
            case String s -> "\"" + s + "\"";
            default -> value.toString();
        };
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Expression copy() {
        return new LiteralExpression(this.value, this.type);
    }
}
