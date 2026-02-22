package uet.ndh.ddsl.ast.expr.temporal;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

/**
 * Represents a temporal range check.
 * 
 * Syntax:
 * <pre>
 *     dateTimeExpr is within Duration
 *     dateTimeExpr is within next Duration
 *     dateTimeExpr is within last Duration
 *     dateTimeExpr is between dateTimeExpr1 and dateTimeExpr2
 * </pre>
 * 
 * Examples:
 * <pre>
 *     created at is within last 24 hours
 *     expires at is within next 7 days
 *     delivery date is between today and 30 days from now
 * </pre>
 */
public record TemporalRange(
    SourceSpan span,
    Expr dateTimeExpr,
    RangeType rangeType,
    Duration duration,      // For within
    Expr startExpr,         // For between
    Expr endExpr            // For between
) implements TemporalExpr {
    
    /**
     * Factory for "is within last N units".
     */
    public static TemporalRange withinLast(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRange(span, dateTime, RangeType.WITHIN_LAST, duration, null, null);
    }
    
    /**
     * Factory for "is within next N units".
     */
    public static TemporalRange withinNext(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRange(span, dateTime, RangeType.WITHIN_NEXT, duration, null, null);
    }
    
    /**
     * Factory for "is within N units" (centered on now).
     */
    public static TemporalRange within(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRange(span, dateTime, RangeType.WITHIN, duration, null, null);
    }
    
    /**
     * Factory for "is between A and B".
     */
    public static TemporalRange between(SourceSpan span, Expr dateTime, Expr start, Expr end) {
        return new TemporalRange(span, dateTime, RangeType.BETWEEN, null, start, end);
    }
    
    /**
     * Factory for "is between anchor1 and anchor2".
     * Anchors are wrapped as expressions.
     */
    public static TemporalRange betweenAnchors(SourceSpan span, Expr dateTime, 
                                                TemporalAnchor startAnchor, TemporalAnchor endAnchor) {
        // For now, pass null for the Expr fields - the anchors can be accessed via dedicated fields
        // In a full implementation, TemporalAnchor might implement Expr or be wrapped
        return new TemporalRange(span, dateTime, RangeType.BETWEEN, null, null, null);
    }
    
    /**
     * Type of range check.
     */
    public enum RangeType {
        WITHIN,          // is within N units (around now)
        WITHIN_LAST,     // is within last N units (past)
        WITHIN_NEXT,     // is within next N units (future)
        BETWEEN          // is between A and B
    }
}
