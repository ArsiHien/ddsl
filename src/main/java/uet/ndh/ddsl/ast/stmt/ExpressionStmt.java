package uet.ndh.ddsl.ast.stmt;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * An expression used as a statement.
 * Pure data record.
 */
public record ExpressionStmt(
    SourceSpan span,
    Expr expression
) implements Stmt {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitExpressionStmt(this);
    }
}
