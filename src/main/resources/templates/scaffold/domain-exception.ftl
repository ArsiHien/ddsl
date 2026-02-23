package ${packageName};

/**
 * ${description}
 *
 * Domain exceptions indicate business rule violations or invalid operations.
 * They should be caught and handled at the application layer.
 */
public class DomainException extends RuntimeException {
    
    /**
     * Create a domain exception with a message.
     *
     * @param message Description of what went wrong
     */
    public DomainException(String message) {
        super(message);
    }
    
    /**
     * Create a domain exception with a message and cause.
     *
     * @param message Description of what went wrong
     * @param cause The underlying cause
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Create a domain exception for an entity not found.
     *
     * @param entityType The type of entity
     * @param id The identity that was not found
     * @return A new DomainException
     */
    public static DomainException notFound(String entityType, Object id) {
        return new DomainException(entityType + " not found: " + id);
    }
    
    /**
     * Create a domain exception for an invalid state transition.
     *
     * @param currentState The current state
     * @param targetState The attempted target state
     * @return A new DomainException
     */
    public static DomainException invalidStateTransition(String currentState, String targetState) {
        return new DomainException(
            "Invalid state transition from " + currentState + " to " + targetState);
    }
    
    /**
     * Create a domain exception for a business rule violation.
     *
     * @param rule Description of the violated rule
     * @return A new DomainException
     */
    public static DomainException ruleViolation(String rule) {
        return new DomainException("Business rule violation: " + rule);
    }
}
