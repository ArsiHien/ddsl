package uet.ndh.ddsl.core.building.expression;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a binary expression (left operator right).
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BinaryExpression extends Expression {
    private final Expression left;
    private final BinaryOperator operator;
    private final Expression right;
    private final String resultType;

    public BinaryExpression(Expression left, BinaryOperator operator, Expression right, String resultType) {
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.resultType = resultType;
    }

    @Override
    public String generateCode() {
        return "(" + left.generateCode() + " " + operator.getSymbol() + " " + right.generateCode() + ")";
    }

    @Override
    public String getType() {
        return resultType;
    }

    @Override
    public Expression copy() {
        return new BinaryExpression(this.left.copy(), this.operator, this.right.copy(), this.resultType);
    }
}
