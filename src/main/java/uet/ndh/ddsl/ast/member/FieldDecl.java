package uet.ndh.ddsl.ast.member;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.Constraint;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.common.Visibility;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a field declaration in an entity, value object, or service.
 * Pure data record - no logic except accept().
 */
public record FieldDecl(
    SourceSpan span,
    String name,
    TypeRef type,
    Visibility visibility,
    boolean isFinal,
    boolean isNullable,
    Expr defaultValue,
    List<Constraint> constraints,
    String documentation
) implements AstNode {
    
    public FieldDecl {
        constraints = constraints != null ? List.copyOf(constraints) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitField(this);
    }
}
