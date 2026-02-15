package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.Map;

/**
 * A map literal expression.
 * Pure data record.
 */
public record MapExpr(
    SourceSpan span,
    Map<Expr, Expr> entries
) implements Expr {
    
    public MapExpr {
        entries = entries != null ? Map.copyOf(entries) : Map.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitMapExpr(this);
    }
}
