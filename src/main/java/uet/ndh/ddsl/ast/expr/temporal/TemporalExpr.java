package uet.ndh.ddsl.ast.expr.temporal;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * Sealed interface for all temporal expressions.
 * 
 * Temporal expressions handle date/time comparisons, ranges, and sequences:
 * - Comparisons: "X is before Y", "X is after now"
 * - Ranges: "X is within last 24 hours", "X is between A and B"
 * - Relative: "X is more than 5 days ago", "X is less than 7 days from now"
 * - Sequences: "X occurred before Y"
 */
public sealed interface TemporalExpr extends Expr 
    permits TemporalComparison, TemporalRange, TemporalRelative, TemporalSequence {
    
    /**
     * The date/time expression being evaluated.
     */
    Expr dateTimeExpr();
    
    @Override
    default <R> R accept(AstVisitor<R> visitor) {
        // Default implementation - visitors can override for specific temporal types
        return null;
    }
}
