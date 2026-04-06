package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A unary expression (op operand).
 * Pure data record.
 */
public record UnaryExpr(
    SourceSpan span,
    UnaryOperator operator,
    Expr operand
) implements Expr {
    
    public enum UnaryOperator {
        NEGATE,  // -
        NOT      // !
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitUnaryExpr(this);
    }
}
