package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A ternary conditional expression (condition ? then : else).
 * Pure data record.
 */
public record TernaryExpr(
    SourceSpan span,
    Expr condition,
    Expr thenExpr,
    Expr elseExpr
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitTernaryExpr(this);
    }
}
