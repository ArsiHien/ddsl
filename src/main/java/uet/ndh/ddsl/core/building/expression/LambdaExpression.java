package uet.ndh.ddsl.core.building.expression;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Lambda expression for collection operations.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class LambdaExpression extends Expression {
    private final String parameter;
    private final Expression body;
    private final String returnType;

    public LambdaExpression(String parameter, Expression body, String returnType) {
        super();
        this.parameter = parameter;
        this.body = body;
        this.returnType = returnType;
    }

    @Override
    public String generateCode() {
        return parameter + " -> " + body.generateCode();
    }

    @Override
    public String getType() {
        return returnType;
    }

    @Override
    public Expression copy() {
        return new LambdaExpression(this.parameter, this.body.copy(), this.returnType);
    }
}
