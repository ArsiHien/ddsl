package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.temporal.Duration;

/**
 * Represents a temporal relative comparison (distance from now).
 * 
 * Syntax:
 * <pre>
 *     dateTimeExpr is more than Duration ago
 *     dateTimeExpr is less than Duration ago
 *     dateTimeExpr is at least Duration ago
 *     dateTimeExpr is at most Duration ago
 *     dateTimeExpr is exactly Duration ago
 *     dateTimeExpr is more than Duration from now
 *     dateTimeExpr is less than Duration from now
 * </pre>
 * 
 * Examples:
 * <pre>
 *     created at is more than 24 hours ago
 *     last login is less than 7 days ago
 *     expires at is less than 7 days from now
 *     subscription started at is exactly 1 year ago
 * </pre>
 */
public record TemporalRelative(
    SourceSpan span,
    Expr dateTimeExpr,
    RelativeOp operator,
    Duration duration,
    RelativeDirection direction
) implements TemporalExpr {
    
    /**
     * Factory for "is more than N ago".
     */
    public static TemporalRelative moreThanAgo(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRelative(span, dateTime, RelativeOp.MORE_THAN, duration, RelativeDirection.AGO);
    }
    
    /**
     * Factory for "is less than N ago".
     */
    public static TemporalRelative lessThanAgo(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRelative(span, dateTime, RelativeOp.LESS_THAN, duration, RelativeDirection.AGO);
    }
    
    /**
     * Factory for "is at least N ago".
     */
    public static TemporalRelative atLeastAgo(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRelative(span, dateTime, RelativeOp.AT_LEAST, duration, RelativeDirection.AGO);
    }
    
    /**
     * Factory for "is at most N ago".
     */
    public static TemporalRelative atMostAgo(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRelative(span, dateTime, RelativeOp.AT_MOST, duration, RelativeDirection.AGO);
    }
    
    /**
     * Factory for "is exactly N ago".
     */
    public static TemporalRelative exactlyAgo(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRelative(span, dateTime, RelativeOp.EXACTLY, duration, RelativeDirection.AGO);
    }
    
    /**
     * Factory for "is more than N from now".
     */
    public static TemporalRelative moreThanFromNow(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRelative(span, dateTime, RelativeOp.MORE_THAN, duration, RelativeDirection.FROM_NOW);
    }
    
    /**
     * Factory for "is less than N from now".
     */
    public static TemporalRelative lessThanFromNow(SourceSpan span, Expr dateTime, Duration duration) {
        return new TemporalRelative(span, dateTime, RelativeOp.LESS_THAN, duration, RelativeDirection.FROM_NOW);
    }
    
    /**
     * Relative comparison operators.
     */
    public enum RelativeOp {
        MORE_THAN,
        LESS_THAN,
        AT_LEAST,
        AT_MOST,
        EXACTLY
    }
    
    /**
     * Direction of relative comparison.
     */
    public enum RelativeDirection {
        AGO,        // In the past
        FROM_NOW    // In the future
    }
}
