package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.building.expression.Expression;

/**
 * Return statement.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class ReturnStatement extends Statement {
    private final Expression expression;

    public ReturnStatement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String generateCode() {
        if (expression == null) {
            return "return;";
        } else {
            return "return " + expression.generateCode() + ";";
        }
    }

    @Override
    public Statement copy() {
        Expression copiedExpression = (expression != null) ? expression.copy() : null;
        return new ReturnStatement(copiedExpression);
    }
}
