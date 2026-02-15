package uet.ndh.ddsl.ast.member;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * Represents a method/constructor parameter.
 * Pure data record - no logic except accept().
 */
public record ParameterDecl(
    SourceSpan span,
    String name,
    TypeRef type,
    boolean isNullable,
    Expr defaultValue
) implements AstNode {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitParameter(this);
    }
}
