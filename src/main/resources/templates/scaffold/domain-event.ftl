package ${packageName};

import java.time.Instant;

/**
 * ${description}
 *
 * Domain events capture something that happened in the domain.
 * They are immutable facts about the past.
 *
 * Best practices:
 * <ul>
 *   <li>Name events in past tense (OrderPlaced, PaymentReceived)</li>
 *   <li>Include all relevant data - events should be self-contained</li>
 *   <li>Include timestamp for ordering and debugging</li>
 *   <li>Use records for automatic immutability</li>
 * </ul>
 */
public interface DomainEvent {
    
    /**
     * When this event occurred.
     *
     * @return The timestamp when the event was created
     */
    Instant occurredAt();
}
