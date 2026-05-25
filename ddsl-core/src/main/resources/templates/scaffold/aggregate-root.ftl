package ${packageName};

/**
 * ${description}
 *
 * <p>Aggregates use {@link EventPublisher} directly to publish domain events
 * rather than collecting them for later publication.</p>
 *
 * @param <ID> The type of the aggregate's identity
 */
public interface AggregateRoot<ID> extends Entity<ID> {
    // Aggregates publish events directly via EventPublisher
    // No getDomainEvents() or clearDomainEvents() methods - deprecated pattern
}
