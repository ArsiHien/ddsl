package uet.ndh.ddsl.ast.stmt;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * A block of statements enclosed in braces.
 * Pure data record.
 */
public record BlockStmt(
    SourceSpan span,
    List<Stmt> statements
) implements Stmt {
    
    public BlockStmt {
        statements = statements != null ? List.copyOf(statements) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitBlockStmt(this);
    }
}
