package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A literal value (string, number, boolean).
 * Pure data record.
 */
public record LiteralExpr(
    SourceSpan span,
    Object value,
    LiteralType type
) implements Expr {
    
    public enum LiteralType {
        STRING,
        INTEGER,
        DECIMAL,
        BOOLEAN
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitLiteralExpr(this);
    }
}
