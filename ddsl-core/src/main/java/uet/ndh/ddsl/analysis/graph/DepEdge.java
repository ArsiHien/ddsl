package uet.ndh.ddsl.analysis.graph;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Represents a directed edge in the dependency graph.
 * Each edge represents a semantic dependency relationship between two domain components.
 * 
 * @param from           Source node (the dependent component)
 * @param to             Target node (the dependency)
 * @param edgeType       Type of dependency relationship
 * @param referenceSpan  Source location where the reference occurs
 */
public record DepEdge(
    DepNode from,
    DepNode to,
    EdgeType edgeType,
    SourceSpan referenceSpan
) {
    /**
     * Creates a builder for constructing DepEdge instances.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder pattern for DepEdge construction.
     */
    public static class Builder {
        private DepNode from;
        private DepNode to;
        private EdgeType edgeType;
        private SourceSpan referenceSpan;
        
        public Builder from(DepNode from) {
            this.from = from;
            return this;
        }
        
        public Builder to(DepNode to) {
            this.to = to;
            return this;
        }
        
        public Builder edgeType(EdgeType edgeType) {
            this.edgeType = edgeType;
            return this;
        }
        
        public Builder referenceSpan(SourceSpan referenceSpan) {
            this.referenceSpan = referenceSpan;
            return this;
        }
        
        public DepEdge build() {
            if (from == null || to == null || edgeType == null) {
                throw new IllegalStateException("from, to, and edgeType are required");
            }
            return new DepEdge(from, to, edgeType,
                referenceSpan != null ? referenceSpan : SourceSpan.unknown());
        }
    }
    
    /**
     * Returns true if this edge crosses bounded context boundaries.
     */
    public boolean isCrossContext() {
        return edgeType == EdgeType.CROSS_CONTEXT ||
               !from.boundedContext().equals(to.boundedContext());
    }
    
    @Override
    public String toString() {
        return String.format("%s --[%s]--> %s", from.qualifiedId(), edgeType, to.qualifiedId());
    }
}
