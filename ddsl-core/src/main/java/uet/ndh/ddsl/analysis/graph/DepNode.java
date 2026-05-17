package uet.ndh.ddsl.analysis.graph;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Represents a node in the dependency graph.
 * Each node corresponds to a domain component (aggregate, entity, service, etc.)
 * and contains metadata about its location and relationships.
 * 
 * @param qualifiedId       Fully qualified identifier (e.g., "HotelBooking.Reservation")
 * @param name              Simple name of the component (e.g., "Reservation")
 * @param kind              The type of domain component
 * @param boundedContext    Name of the containing bounded context
 * @param span              Source location for error reporting
 * @param astNode           Back-reference to the original AST node
 */
public record DepNode(
    String qualifiedId,
    String name,
    NodeKind kind,
    String boundedContext,
    SourceSpan span,
    AstNode astNode
) {
    /**
     * Creates a builder for constructing DepNode instances.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder pattern for DepNode construction.
     */
    public static class Builder {
        private String qualifiedId;
        private String name;
        private NodeKind kind;
        private String boundedContext;
        private SourceSpan span;
        private AstNode astNode;
        
        public Builder qualifiedId(String qualifiedId) {
            this.qualifiedId = qualifiedId;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder kind(NodeKind kind) {
            this.kind = kind;
            return this;
        }
        
        public Builder boundedContext(String boundedContext) {
            this.boundedContext = boundedContext;
            return this;
        }
        
        public Builder span(SourceSpan span) {
            this.span = span;
            return this;
        }
        
        public Builder astNode(AstNode astNode) {
            this.astNode = astNode;
            return this;
        }
        
        public DepNode build() {
            if (qualifiedId == null || name == null || kind == null) {
                throw new IllegalStateException("qualifiedId, name, and kind are required");
            }
            return new DepNode(qualifiedId, name, kind, 
                boundedContext != null ? boundedContext : "", 
                span != null ? span : SourceSpan.unknown(), 
                astNode);
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s]", qualifiedId, kind);
    }
}
