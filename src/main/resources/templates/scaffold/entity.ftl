package ${packageName};

/**
 * ${description}
 *
 * @param <ID> The type of the entity's identity
 */
public interface Entity<ID> {
    
    /**
     * Get the unique identifier of this entity.
     * Entity identity is immutable and established at creation time.
     *
     * @return The entity's unique identifier
     */
    ID getId();
}
