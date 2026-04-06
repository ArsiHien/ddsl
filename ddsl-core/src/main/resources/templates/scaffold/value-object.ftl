package ${packageName};

/**
 * ${description}
 *
 * Value objects should:
 * <ul>
 *   <li>Be immutable - all fields should be final</li>
 *   <li>Have structural equality - equals based on all attributes</li>
 *   <li>Have no identity - two VOs with same values are interchangeable</li>
 *   <li>Be self-validating - validate invariants in constructor</li>
 * </ul>
 *
 * Tip: Use Java records for value objects to get immutability,
 * equals/hashCode, and toString automatically.
 */
public interface ValueObject {
    // Marker interface for Value Objects
}
