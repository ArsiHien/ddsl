package ${packageName};

import java.util.Optional;

/**
 * ${description}
 *
 * Repositories provide collection-like access to aggregates.
 * They abstract away the details of data storage and retrieval.
 *
 * @param <T> The aggregate root type
 * @param <ID> The aggregate's identity type
 */
public interface Repository<T extends AggregateRoot<ID>, ID> {
    
    /**
     * Find an aggregate by its identity.
     *
     * @param id The aggregate's unique identifier
     * @return Optional containing the aggregate if found
     */
    Optional<T> findById(ID id);
    
    /**
     * Check if an aggregate exists with the given identity.
     *
     * @param id The aggregate's unique identifier
     * @return true if the aggregate exists
     */
    boolean existsById(ID id);
    
    /**
     * Save an aggregate (insert or update).
     * After saving, domain events should be published and cleared.
     *
     * @param aggregate The aggregate to save
     * @return The saved aggregate (may have updated version, etc.)
     */
    T save(T aggregate);
    
    /**
     * Delete an aggregate by its identity.
     *
     * @param id The aggregate's unique identifier
     */
    void deleteById(ID id);
}
