package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * A list literal expression.
 * Pure data record.
 */
public record ListExpr(
    SourceSpan span,
    List<Expr> elements
) implements Expr {
    
    public ListExpr {
        elements = elements != null ? List.copyOf(elements) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitListExpr(this);
    }
}
