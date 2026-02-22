package uet.ndh.ddsl.ast.model.statemachine;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;

/**
 * Represents a guard rule in a state machine.
 * 
 * Syntax:
 * <pre>
 *     - cannot transition from State1 to State2 when condition
 *     - must transition from State1 to State2 when condition
 * </pre>
 * 
 * Examples:
 * <pre>
 *     - cannot transition from Completed to Refunding when refund window has expired
 *     - cannot transition from Processing to Completed when amount exceeds fraud threshold
 *     - must transition from PastDue to Suspended when past due is more than 7 days
 * </pre>
 */
public record GuardRule(
    SourceSpan span,
    GuardType type,
    String fromState,
    String toState,
    NaturalLanguageCondition condition
) {
    
    /**
     * Factory for "cannot transition" guard.
     */
    public static GuardRule cannot(SourceSpan span, String from, String to, 
                                    NaturalLanguageCondition condition) {
        return new GuardRule(span, GuardType.CANNOT, from, to, condition);
    }
    
    /**
     * Factory for "must transition" guard.
     */
    public static GuardRule must(SourceSpan span, String from, String to,
                                  NaturalLanguageCondition condition) {
        return new GuardRule(span, GuardType.MUST, from, to, condition);
    }
    
    /**
     * Type of guard.
     */
    public enum GuardType {
        CANNOT,     // Cannot transition when condition is true
        MUST        // Must transition when condition is true
    }
}
