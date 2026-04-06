package uet.ndh.ddsl.ast.member;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * Represents an invariant - a business rule that must always be true.
 * Pure data record - no logic except accept().
 */
public record InvariantDecl(
    SourceSpan span,
    String name,
    String errorMessage,
    Expr condition,
    String documentation
) implements AstNode {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitInvariant(this);
    }
}
