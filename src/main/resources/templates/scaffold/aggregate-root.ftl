package ${packageName};

import java.util.List;

/**
 * ${description}
 *
 * @param <ID> The type of the aggregate's identity
 */
public interface AggregateRoot<ID> extends Entity<ID> {
    
    /**
     * Get all domain events that have been registered by this aggregate.
     * Events are generated during state changes and should be published
     * after the aggregate is persisted.
     *
     * @return Immutable list of domain events
     */
    List<DomainEvent> getDomainEvents();
    
    /**
     * Clear all registered domain events.
     * Typically called after events have been successfully published.
     */
    void clearDomainEvents();
}
