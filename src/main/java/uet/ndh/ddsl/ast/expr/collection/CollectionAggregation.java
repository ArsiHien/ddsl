package uet.ndh.ddsl.ast.expr.collection;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a collection aggregation expression.
 * 
 * Syntax:
 * <pre>
 *     sum of collection propertyPath where condition
 *     count of collection where condition
 *     maximum of collection propertyPath where condition
 *     minimum of collection propertyPath where condition
 *     average of collection propertyPath where condition
 * </pre>
 * 
 * Examples:
 * <pre>
 *     sum of orders total amounts where status is Confirmed
 *     count of items where quantity is greater than 0
 *     maximum of orders total amounts where customer tier is Gold
 *     average of products ratings where status is Active
 * </pre>
 */
public record CollectionAggregation(
    SourceSpan span,
    AggregationType aggregationType,
    Expr collection,
    String propertyPath,                    // For sum, max, min, avg
    NaturalLanguageCondition whereCondition // Optional filter
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return null;
    }
    
    // Factory methods
    
    public static CollectionAggregation sum(SourceSpan span, Expr collection, 
                                             String property, NaturalLanguageCondition where) {
        return new CollectionAggregation(span, AggregationType.SUM, collection, property, where);
    }
    
    public static CollectionAggregation count(SourceSpan span, Expr collection,
                                               NaturalLanguageCondition where) {
        return new CollectionAggregation(span, AggregationType.COUNT, collection, null, where);
    }
    
    public static CollectionAggregation max(SourceSpan span, Expr collection,
                                             String property, NaturalLanguageCondition where) {
        return new CollectionAggregation(span, AggregationType.MAXIMUM, collection, property, where);
    }
    
    public static CollectionAggregation min(SourceSpan span, Expr collection,
                                             String property, NaturalLanguageCondition where) {
        return new CollectionAggregation(span, AggregationType.MINIMUM, collection, property, where);
    }
    
    public static CollectionAggregation avg(SourceSpan span, Expr collection,
                                             String property, NaturalLanguageCondition where) {
        return new CollectionAggregation(span, AggregationType.AVERAGE, collection, property, where);
    }
    
    /**
     * Type of aggregation.
     */
    public enum AggregationType {
        SUM,
        COUNT,
        MAXIMUM,
        MINIMUM,
        AVERAGE
    }
}
