package uet.ndh.ddsl.analysis.graph;

/**
 * Enum representing the different types of dependency edges in the graph.
 * Each edge type represents a specific semantic relationship between domain components.
 */
public enum EdgeType {
    /**
     * Composition relationship - parent contains child (e.g., Aggregate contains Entities).
     */
    COMPOSITION,
    
    /**
     * Type dependency - field, parameter, or return type references another type.
     */
    TYPE_DEPENDENCY,
    
    /**
     * Event publishing - aggregate/entity publishes a domain event.
     */
    EVENT_PUBLISH,
    
    /**
     * Event handling - event handler listens to a domain event.
     */
    EVENT_HANDLER,
    
    /**
     * Repository access - repository manages a specific aggregate type.
     */
    REPOSITORY_ACCESS,
    
    /**
     * Service dependency - service depends on another component via field injection.
     */
    SERVICE_DEPENDENCY,
    
    /**
     * Cross-context dependency - any dependency that crosses bounded context boundaries.
     * This is an overlay type applied alongside other edge types.
     */
    CROSS_CONTEXT
}
