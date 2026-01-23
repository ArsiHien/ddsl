package uet.ndh.ddsl.core.model.entity;

/**
 * Reference to a domain event raised by an entity.
 */
public record EventReference(String eventClass, String raisedWhen) {

    /**
     * Create a copy of this event reference.
     */
    public EventReference copy() {
        return new EventReference(this.eventClass, this.raisedWhen);
    }
}
