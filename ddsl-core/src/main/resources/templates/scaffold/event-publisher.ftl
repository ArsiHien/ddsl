package ${packageName};

import ${packageName}.DomainEvent;
import java.util.List;

/**
 * Interface for publishing domain events.
 * Implementations handle the actual event dispatch mechanism.
 */
public interface EventPublisher {

    /**
     * Publish a domain event.
     *
     * @param event the domain event to publish
     */
    void publish(DomainEvent event);

    /**
     * Publish multiple domain events.
     *
     * @param events the domain events to publish
     */
    default void publishAll(List<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}