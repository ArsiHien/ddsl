package uet.ndh.ddsl.ast.expr.collection;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a collection filter chain expression.
 * 
 * Syntax:
 * <pre>
 *     collection where condition
 *     collection where condition1 where condition2
 *     collection where condition of their propertyPath
 * </pre>
 * 
 * Examples:
 * <pre>
 *     orders where status is Confirmed
 *     orders where status is Confirmed where total amount exceeds 1000000
 *     orders where status is Active of their item count
 * </pre>
 */
public record CollectionFilter(
    SourceSpan span,
    Expr collection,
    List<NaturalLanguageCondition> whereConditions,
    String ofTheirProperty      // Optional "of their propertyPath"
) implements Expr {
    
    public CollectionFilter {
        whereConditions = whereConditions != null ? List.copyOf(whereConditions) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return null;
    }
    
    /**
     * Factory for single where clause.
     */
    public static CollectionFilter single(SourceSpan span, Expr collection,
                                           NaturalLanguageCondition where) {
        return new CollectionFilter(span, collection, List.of(where), null);
    }
    
    /**
     * Factory for chained where clauses.
     */
    public static CollectionFilter chained(SourceSpan span, Expr collection,
                                            List<NaturalLanguageCondition> wheres) {
        return new CollectionFilter(span, collection, wheres, null);
    }
    
    /**
     * Factory with "of their" property.
     */
    public static CollectionFilter withProperty(SourceSpan span, Expr collection,
                                                 List<NaturalLanguageCondition> wheres,
                                                 String property) {
        return new CollectionFilter(span, collection, wheres, property);
    }
}
