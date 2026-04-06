package uet.ndh.ddsl.ast.application;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents an Application Service - orchestrates use cases
 * at the application layer.
 * 
 * Pure data record - no logic except accept().
 */
public record ApplicationServiceDecl(
    SourceSpan span,
    String name,
    List<UseCaseDecl> useCases,
    List<FieldDecl> dependencies,
    String documentation
) implements AstNode {
    
    public ApplicationServiceDecl {
        useCases = useCases != null ? List.copyOf(useCases) : List.of();
        dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitApplicationService(this);
    }
}
