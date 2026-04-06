package uet.ndh.ddsl.ast.stmt;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A variable declaration statement.
 * Pure data record.
 */
public record VariableDeclarationStmt(
    SourceSpan span,
    String name,
    TypeRef type,  // nullable if type inference is used
    Expr initializer,
    boolean isFinal
) implements Stmt {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitVariableDeclarationStmt(this);
    }
    
    public boolean hasExplicitType() {
        return type != null;
    }
}
