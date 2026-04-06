package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * Represents a collection group by expression.
 * 
 * Syntax:
 * <pre>
 *     collection grouped by propertyPath
 *     count of collection grouped by propertyPath
 *     sum of collection propertyPath grouped by otherProperty
 * </pre>
 * 
 * Examples:
 * <pre>
 *     orders grouped by status
 *     count of orders grouped by customer tier
 *     sum of orders total amounts grouped by category
 * </pre>
 */
public record CollectionGroupBy(
    SourceSpan span,
    Expr collection,
    String groupByProperty,
    GroupByAggregation aggregation,     // Optional aggregation
    String aggregationProperty          // For sum/avg aggregation
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return null;
    }
    
    /**
     * Factory for simple group by.
     */
    public static CollectionGroupBy simple(SourceSpan span, Expr collection, String groupBy) {
        return new CollectionGroupBy(span, collection, groupBy, null, null);
    }
    
    /**
     * Factory for count grouped by.
     */
    public static CollectionGroupBy count(SourceSpan span, Expr collection, String groupBy) {
        return new CollectionGroupBy(span, collection, groupBy, GroupByAggregation.COUNT, null);
    }
    
    /**
     * Factory for sum grouped by.
     */
    public static CollectionGroupBy sum(SourceSpan span, Expr collection, 
                                         String sumProperty, String groupBy) {
        return new CollectionGroupBy(span, collection, groupBy, GroupByAggregation.SUM, sumProperty);
    }
    
    /**
     * Type of aggregation in group by.
     */
    public enum GroupByAggregation {
        NONE,       // Just grouping
        COUNT,      // Count per group
        SUM,        // Sum per group
        AVERAGE,    // Average per group
        MAXIMUM,    // Max per group
        MINIMUM     // Min per group
    }
}
