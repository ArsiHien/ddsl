package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A variable reference.
 * Pure data record.
 */
public record VariableExpr(
    SourceSpan span,
    String name
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitVariableExpr(this);
    }
}
