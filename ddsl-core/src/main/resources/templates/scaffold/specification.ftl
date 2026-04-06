package ${packageName};

import java.util.function.Predicate;

/**
 * ${description}
 *
 * Specifications encapsulate business rules as reusable, composable objects.
 * They can be combined using and(), or(), and not() operations.
 *
 * Example usage:
 * <pre>
 * Specification<Order> isPaid = order -> order.getStatus() == Status.PAID;
 * Specification<Order> isLarge = order -> order.getTotal() > 1000;
 * 
 * Specification<Order> largeAndPaid = isPaid.and(isLarge);
 * 
 * if (largeAndPaid.isSatisfiedBy(order)) {
 *     // Apply VIP discount
 * }
 * </pre>
 *
 * @param <T> The type of object this specification applies to
 */
@FunctionalInterface
public interface Specification<T> extends Predicate<T> {
    
    /**
     * Check if the candidate satisfies this specification.
     *
     * @param candidate The object to check
     * @return true if the candidate satisfies the specification
     */
    boolean isSatisfiedBy(T candidate);
    
    /**
     * Predicate compatibility - delegates to isSatisfiedBy.
     */
    @Override
    default boolean test(T candidate) {
        return isSatisfiedBy(candidate);
    }
    
    /**
     * Combine this specification with another using AND logic.
     * Both specifications must be satisfied.
     *
     * @param other The other specification
     * @return A new specification that is satisfied when both are satisfied
     */
    default Specification<T> and(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }
    
    /**
     * Combine this specification with another using OR logic.
     * At least one specification must be satisfied.
     *
     * @param other The other specification
     * @return A new specification that is satisfied when either is satisfied
     */
    default Specification<T> or(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }
    
    /**
     * Negate this specification.
     *
     * @return A new specification that is satisfied when this one is not
     */
    default Specification<T> not() {
        return candidate -> !this.isSatisfiedBy(candidate);
    }
    
    /**
     * Create a specification that is always satisfied.
     *
     * @param <T> The type of object
     * @return A specification that always returns true
     */
    static <T> Specification<T> alwaysTrue() {
        return candidate -> true;
    }
    
    /**
     * Create a specification that is never satisfied.
     *
     * @param <T> The type of object
     * @return A specification that always returns false
     */
    static <T> Specification<T> alwaysFalse() {
        return candidate -> false;
    }
}
