package uet.ndh.ddsl.ast.stmt;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * An if-then-else statement.
 * Pure data record.
 */
public record IfStmt(
    SourceSpan span,
    Expr condition,
    Stmt thenBranch,
    Stmt elseBranch  // nullable
) implements Stmt {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitIfStmt(this);
    }
    
    public boolean hasElseBranch() {
        return elseBranch != null;
    }
}
