package uet.ndh.ddsl.core.building.expression;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Field access expression (object.field or object.method()).
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class FieldAccessExpression extends Expression {
    private final Expression target;
    private final String fieldName;
    private final String type;

    public FieldAccessExpression(Expression target, String fieldName, String type) {
        this.target = target;
        this.fieldName = fieldName;
        this.type = type;
    }

    @Override
    public String generateCode() {
        return target.generateCode() + "." + fieldName;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Expression copy() {
        return new FieldAccessExpression(this.target.copy(), this.fieldName, this.type);
    }
}
