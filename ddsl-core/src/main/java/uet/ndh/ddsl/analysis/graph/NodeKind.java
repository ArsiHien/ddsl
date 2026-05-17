package uet.ndh.ddsl.analysis.graph;

/**
 * Enum representing the different kinds of nodes in the dependency graph.
 * Each node represents a DDD tactical pattern or architectural component.
 */
public enum NodeKind {
    /**
     * Bounded Context - the highest level linguistic and conceptual boundary.
     */
    BOUNDED_CONTEXT,
    
    /**
     * Module - organizational unit within a bounded context.
     */
    MODULE,
    
    /**
     * Aggregate - cluster of domain objects treated as a single unit.
     */
    AGGREGATE,
    
    /**
     * Entity - object with identity that persists through state changes.
     */
    ENTITY,
    
    /**
     * Value Object - immutable object defined by its attributes, no identity.
     */
    VALUE_OBJECT,
    
    /**
     * Domain Service - stateless operation that doesn't belong to an entity/VO.
     */
    DOMAIN_SERVICE,
    
    /**
     * Domain Event - something that happened in the domain that experts care about.
     */
    DOMAIN_EVENT,
    
    /**
     * Repository - abstraction for aggregate persistence.
     */
    REPOSITORY,
    
    /**
     * Factory - responsible for creating complex domain objects.
     */
    FACTORY,
    
    /**
     * Specification - predicate for business rule validation.
     */
    SPECIFICATION
}
