package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Represents a temporal sequence check between two events.
 * 
 * Syntax:
 * <pre>
 *     dateTimeExpr1 is before dateTimeExpr2
 *     dateTimeExpr1 is after dateTimeExpr2
 *     dateTimeExpr1 occurred before dateTimeExpr2
 *     dateTimeExpr1 occurred after dateTimeExpr2
 * </pre>
 * 
 * Examples:
 * <pre>
 *     confirmed at is after created at
 *     shipped at occurred after confirmed at
 *     payment received at is before order confirmed at
 * </pre>
 */
public record TemporalSequence(
    SourceSpan span,
    Expr dateTimeExpr,
    SequenceOp operator,
    Expr otherDateTimeExpr
) implements TemporalExpr {
    
    /**
     * Factory for "is before" sequence.
     */
    public static TemporalSequence isBefore(SourceSpan span, Expr first, Expr second) {
        return new TemporalSequence(span, first, SequenceOp.IS_BEFORE, second);
    }
    
    /**
     * Factory for "is after" sequence.
     */
    public static TemporalSequence isAfter(SourceSpan span, Expr first, Expr second) {
        return new TemporalSequence(span, first, SequenceOp.IS_AFTER, second);
    }
    
    /**
     * Factory for "occurred before" sequence.
     */
    public static TemporalSequence occurredBefore(SourceSpan span, Expr first, Expr second) {
        return new TemporalSequence(span, first, SequenceOp.OCCURRED_BEFORE, second);
    }
    
    /**
     * Factory for "occurred after" sequence.
     */
    public static TemporalSequence occurredAfter(SourceSpan span, Expr first, Expr second) {
        return new TemporalSequence(span, first, SequenceOp.OCCURRED_AFTER, second);
    }
    
    /**
     * Sequence operators.
     */
    public enum SequenceOp {
        IS_BEFORE,
        IS_AFTER,
        OCCURRED_BEFORE,
        OCCURRED_AFTER
    }
}
