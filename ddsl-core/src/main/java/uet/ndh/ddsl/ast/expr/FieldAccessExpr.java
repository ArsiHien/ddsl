package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A field access expression (object.field).
 * Pure data record.
 */
public record FieldAccessExpr(
    SourceSpan span,
    Expr object,
    String fieldName
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitFieldAccessExpr(this);
    }
}
