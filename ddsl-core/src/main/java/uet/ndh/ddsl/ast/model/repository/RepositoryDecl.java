package uet.ndh.ddsl.ast.model.repository;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a DDD Repository - abstraction for aggregate persistence.
 * 
 * Pure data record - no logic except accept().
 */
public record RepositoryDecl(
    SourceSpan span,
    String name,
    TypeRef aggregateType,
    List<RepositoryMethodDecl> methods,
    String documentation
) implements AstNode {
    
    public RepositoryDecl {
        methods = methods != null ? List.copyOf(methods) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitRepository(this);
    }
}
