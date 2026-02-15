package uet.ndh.ddsl.ast.stmt;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * An assignment statement (target = value).
 * Pure data record.
 */
public record AssignmentStmt(
    SourceSpan span,
    Expr target,  // Variable or field access
    Expr value,
    AssignmentOperator operator
) implements Stmt {
    
    public enum AssignmentOperator {
        ASSIGN,      // =
        PLUS_ASSIGN, // +=
        MINUS_ASSIGN // -=
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitAssignmentStmt(this);
    }
}
