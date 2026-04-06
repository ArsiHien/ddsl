package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A null literal expression.
 * Pure data record.
 */
public record NullExpr(
    SourceSpan span
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitNullExpr(this);
    }
}
