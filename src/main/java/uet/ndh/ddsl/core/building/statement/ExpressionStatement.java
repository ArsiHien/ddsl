package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.building.expression.Expression;

/**
 * Expression statement (method call, etc.).
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class ExpressionStatement extends Statement {
    private final Expression expression;

    public ExpressionStatement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String generateCode() {
        return expression.generateCode() + ";";
    }

    @Override
    public Statement copy() {
        return new ExpressionStatement(this.expression.copy());
    }
}
