package uet.ndh.ddsl.ast.model.service;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a DDD Domain Service - stateless operations that don't
 * naturally belong to any entity or value object.
 * 
 * Pure data record - no logic except accept().
 */
public record DomainServiceDecl(
    SourceSpan span,
    String name,
    List<MethodDecl> methods,
    List<FieldDecl> dependencies,
    String documentation
) implements AstNode {
    
    public DomainServiceDecl {
        methods = methods != null ? List.copyOf(methods) : List.of();
        dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitDomainService(this);
    }
}
