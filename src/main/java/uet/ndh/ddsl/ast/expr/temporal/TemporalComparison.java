package uet.ndh.ddsl.ast.expr.temporal;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

/**
 * Represents a temporal comparison with a specific anchor point.
 * 
 * Syntax:
 * <pre>
 *     dateTimeExpr is before now
 *     dateTimeExpr is after today
 *     dateTimeExpr is on yesterday
 *     dateTimeExpr is not before tomorrow
 * </pre>
 * 
 * Examples:
 * <pre>
 *     created at is before now
 *     expires at is after now
 *     payment due date is not before today
 * </pre>
 */
public record TemporalComparison(
    SourceSpan span,
    Expr dateTimeExpr,
    TemporalComparisonOp operator,
    TemporalAnchor anchor
) implements TemporalExpr {
    
    /**
     * Factory for "is before" comparison.
     */
    public static TemporalComparison before(SourceSpan span, Expr dateTime, TemporalAnchor anchor) {
        return new TemporalComparison(span, dateTime, TemporalComparisonOp.IS_BEFORE, anchor);
    }
    
    /**
     * Factory for "is after" comparison.
     */
    public static TemporalComparison after(SourceSpan span, Expr dateTime, TemporalAnchor anchor) {
        return new TemporalComparison(span, dateTime, TemporalComparisonOp.IS_AFTER, anchor);
    }
    
    /**
     * Factory for "is on" comparison.
     */
    public static TemporalComparison on(SourceSpan span, Expr dateTime, TemporalAnchor anchor) {
        return new TemporalComparison(span, dateTime, TemporalComparisonOp.IS_ON, anchor);
    }
    
    /**
     * Temporal comparison operators.
     */
    public enum TemporalComparisonOp {
        IS_BEFORE,
        IS_AFTER,
        IS_ON,
        IS_NOT_BEFORE,
        IS_NOT_AFTER
    }
}
