package uet.ndh.ddsl.ast.expr.temporal;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

/**
 * Represents a temporal anchor point (reference time).
 * 
 * Anchors can be:
 * - Fixed keywords: now, today, yesterday, tomorrow
 * - Relative: "5 days ago", "2 hours from now"
 * - Expressions: other date/time expressions
 * 
 * Examples:
 * <pre>
 *     now
 *     today
 *     yesterday
 *     tomorrow
 *     24 hours ago
 *     7 days from now
 *     start of month
 *     end of month
 * </pre>
 */
public record TemporalAnchor(
    SourceSpan span,
    AnchorType type,
    Duration relativeDuration,
    Expr expression
) {
    
    /**
     * Factory for "now" anchor.
     */
    public static TemporalAnchor now(SourceSpan span) {
        return new TemporalAnchor(span, AnchorType.NOW, null, null);
    }
    
    /**
     * Factory for "today" anchor.
     */
    public static TemporalAnchor today(SourceSpan span) {
        return new TemporalAnchor(span, AnchorType.TODAY, null, null);
    }
    
    /**
     * Factory for "yesterday" anchor.
     */
    public static TemporalAnchor yesterday(SourceSpan span) {
        return new TemporalAnchor(span, AnchorType.YESTERDAY, null, null);
    }
    
    /**
     * Factory for "tomorrow" anchor.
     */
    public static TemporalAnchor tomorrow(SourceSpan span) {
        return new TemporalAnchor(span, AnchorType.TOMORROW, null, null);
    }
    
    /**
     * Factory for "N units ago" anchor.
     */
    public static TemporalAnchor ago(SourceSpan span, Duration duration) {
        return new TemporalAnchor(span, AnchorType.AGO, duration, null);
    }
    
    /**
     * Factory for "N units from now" anchor.
     */
    public static TemporalAnchor fromNow(SourceSpan span, Duration duration) {
        return new TemporalAnchor(span, AnchorType.FROM_NOW, duration, null);
    }
    
    /**
     * Factory for expression-based anchor.
     */
    public static TemporalAnchor expression(SourceSpan span, Expr expr) {
        return new TemporalAnchor(span, AnchorType.EXPRESSION, null, expr);
    }
    
    /**
     * Type of temporal anchor.
     */
    public enum AnchorType {
        NOW,
        TODAY,
        YESTERDAY,
        TOMORROW,
        AGO,
        FROM_NOW,
        START_OF_MONTH,
        END_OF_MONTH,
        START_OF_YEAR,
        END_OF_YEAR,
        EXPRESSION
    }
}
