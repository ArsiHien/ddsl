package uet.ndh.ddsl.core.building.expression;

/**
 * Collection operations enum.
 */
public enum CollectionOperation {
    FILTER,     // Filter items by predicate
    MAP,        // Transform each item
    SUM,        // Sum all numeric items
    AVG,        // Average of all numeric items
    COUNT,      // Count of items
    MIN,        // Minimum item
    MAX,        // Maximum item
    ANY,        // Any item matches (with optional predicate)
    ALL,        // All items match predicate
    FIRST,      // First item
    LAST        // Last item
}
