package uet.ndh.ddsl.ast.stmt;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A for-each loop statement.
 * Pure data record.
 */
public record ForEachStmt(
    SourceSpan span,
    String variableName,
    Expr collection,
    Stmt body
) implements Stmt {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitForEachStmt(this);
    }
}
