package uet.ndh.ddsl.ast.expr.collection;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * Represents a collection flatten expression.
 * 
 * Syntax:
 * <pre>
 *     all propertyPath across collection
 *     all propertyPath from collection
 * </pre>
 * 
 * Examples:
 * <pre>
 *     all items across orders
 *     all lines from orders where status is Confirmed
 *     all product IDs across order lines
 * </pre>
 */
public record CollectionFlatten(
    SourceSpan span,
    String propertyPath,
    Expr collection,
    NaturalLanguageCondition whereCondition,    // Optional filter
    FlattenType type
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return null;
    }
    
    /**
     * Factory for "all X across Y".
     */
    public static CollectionFlatten across(SourceSpan span, String property, Expr collection) {
        return new CollectionFlatten(span, property, collection, null, FlattenType.ACROSS);
    }
    
    /**
     * Factory for "all X from Y".
     */
    public static CollectionFlatten from(SourceSpan span, String property, Expr collection) {
        return new CollectionFlatten(span, property, collection, null, FlattenType.FROM);
    }
    
    /**
     * Factory with where condition.
     */
    public static CollectionFlatten withFilter(SourceSpan span, String property, Expr collection,
                                                NaturalLanguageCondition where, FlattenType type) {
        return new CollectionFlatten(span, property, collection, where, type);
    }
    
    /**
     * Type of flatten operation.
     */
    public enum FlattenType {
        ACROSS,     // all X across Y
        FROM        // all X from Y
    }
}
