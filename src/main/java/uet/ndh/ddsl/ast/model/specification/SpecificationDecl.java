package uet.ndh.ddsl.ast.model.specification;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a DDD Specification - encapsulates a business rule
 * that can be evaluated against an object.
 * 
 * Pure data record - no logic except accept().
 */
public record SpecificationDecl(
    SourceSpan span,
    String name,
    TypeRef targetType,
    Expr predicate,
    List<ParameterDecl> parameters,
    String documentation
) implements AstNode {
    
    public SpecificationDecl {
        parameters = parameters != null ? List.copyOf(parameters) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitSpecification(this);
    }
}
