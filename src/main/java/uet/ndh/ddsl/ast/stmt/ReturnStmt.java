package uet.ndh.ddsl.ast.stmt;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A return statement.
 * Pure data record.
 */
public record ReturnStmt(
    SourceSpan span,
    Expr value  // nullable for void returns
) implements Stmt {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitReturnStmt(this);
    }
    
    public boolean hasValue() {
        return value != null;
    }
}
