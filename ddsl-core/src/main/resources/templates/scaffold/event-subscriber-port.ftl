package ${packageName};

import ${packageName}.DomainEvent;

/**
 * Port interface for subscribing to domain events from infrastructure.
 *
 * Implementations receive domain events from external systems or message brokers
 * and bridge them into the application's event handling framework.
 */
public interface EventSubscriber {

    /**
     * Returns the type of domain event this subscriber is interested in.
     *
     * @return the class of the domain event type to subscribe to
     */
    Class<? extends DomainEvent> subscribedTo();

    /**
     * Called when a domain event occurs.
     *
     * @param event the domain event that occurred
     */
    void onEvent(DomainEvent event);
}