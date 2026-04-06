package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * The 'this' reference expression.
 * Pure data record.
 */
public record ThisExpr(
    SourceSpan span
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitThisExpr(this);
    }
}
