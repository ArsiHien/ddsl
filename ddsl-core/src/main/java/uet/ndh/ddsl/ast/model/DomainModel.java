package uet.ndh.ddsl.ast.model;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents the root Program/DomainModel - the top-level container
 * that holds one or more Bounded Contexts.
 * 
 * From the language design document:
 * "The Program class serves as the root container, while the BoundedContext 
 * acts as the fundamental scoping mechanism."
 * 
 * Pure data record - no logic except accept().
 */
public record DomainModel(
    SourceSpan span,
    String name,
    List<BoundedContextDecl> boundedContexts,
    String documentation
) implements AstNode {
    
    public DomainModel {
        boundedContexts = boundedContexts != null ? List.copyOf(boundedContexts) : List.of();
    }
    
    /**
     * Convenience constructor for a single bounded context.
     */
    public DomainModel(SourceSpan span, String name, BoundedContextDecl context, String documentation) {
        this(span, name, context != null ? List.of(context) : List.of(), documentation);
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitDomainModel(this);
    }
}
