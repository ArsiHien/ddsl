package ${packageName};

import ${packageName}.DomainEvent;

/**
 * Port interface for handling domain events in the application layer.
 *
 * Implementations process incoming domain events and execute the appropriate
 * business logic in response to domain occurrences.
 */
public interface EventHandler {

    /**
     * Handle a domain event.
     *
     * @param event the domain event to process
     */
    void handle(DomainEvent event);
}