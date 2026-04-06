package uet.ndh.ddsl.ast.model.statemachine;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;
import uet.ndh.ddsl.ast.expr.temporal.Duration;

import java.util.List;

/**
 * Represents a transition rule in a state machine.
 * 
 * Syntax:
 * <pre>
 *     - SourceState -> TargetState: TransitionCondition
 *     - [State1, State2] -> TargetState: when condition
 *     - any -> TargetState: never
 * </pre>
 * 
 * TransitionCondition:
 * <pre>
 *     always
 *     when condition
 *     only when condition
 *     only within Duration of identifier
 *     never
 * </pre>
 * 
 * Examples:
 * <pre>
 *     - Pending -> Confirmed: when payment received
 *     - [Pending, Confirmed] -> Cancelled: always
 *     - Delivered -> Returned: only when delivered at is less than 30 days ago
 *     - Confirmed -> Cancelled: only within 24 hours of confirmed at
 *     - Cancelled -> any: never
 * </pre>
 */
public record TransitionRule(
    SourceSpan span,
    List<String> sourceStates,      // Multiple sources or "any"
    boolean anySource,              // True if "any" was specified
    String targetState,
    TransitionCondition condition
) {
    
    public TransitionRule {
        sourceStates = sourceStates != null ? List.copyOf(sourceStates) : List.of();
    }
    
    /**
     * Check if this rule matches a given source state.
     */
    public boolean matchesSource(String state) {
        return anySource || sourceStates.contains(state);
    }
    
    /**
     * Factory for single source transition.
     */
    public static TransitionRule single(SourceSpan span, String source, String target, 
                                         TransitionCondition condition) {
        return new TransitionRule(span, List.of(source), false, target, condition);
    }
    
    /**
     * Factory for multiple source transition.
     */
    public static TransitionRule multiple(SourceSpan span, List<String> sources, String target,
                                           TransitionCondition condition) {
        return new TransitionRule(span, sources, false, target, condition);
    }
    
    /**
     * Factory for "any" source transition.
     */
    public static TransitionRule fromAny(SourceSpan span, String target, TransitionCondition condition) {
        return new TransitionRule(span, List.of(), true, target, condition);
    }
    
    /**
     * Represents the condition for a transition.
     */
    public record TransitionCondition(
        ConditionType type,
        NaturalLanguageCondition condition,     // For WHEN, ONLY_WHEN
        Duration withinDuration,                // For ONLY_WITHIN
        String withinReference                  // For ONLY_WITHIN (e.g., "confirmed at")
    ) {
        
        /**
         * Factory for "always".
         */
        public static TransitionCondition always() {
            return new TransitionCondition(ConditionType.ALWAYS, null, null, null);
        }
        
        /**
         * Factory for "never".
         */
        public static TransitionCondition never() {
            return new TransitionCondition(ConditionType.NEVER, null, null, null);
        }
        
        /**
         * Factory for "when condition".
         */
        public static TransitionCondition when(NaturalLanguageCondition condition) {
            return new TransitionCondition(ConditionType.WHEN, condition, null, null);
        }
        
        /**
         * Factory for "only when condition".
         */
        public static TransitionCondition onlyWhen(NaturalLanguageCondition condition) {
            return new TransitionCondition(ConditionType.ONLY_WHEN, condition, null, null);
        }
        
        /**
         * Factory for "only within duration of reference".
         */
        public static TransitionCondition onlyWithin(Duration duration, String reference) {
            return new TransitionCondition(ConditionType.ONLY_WITHIN, null, duration, reference);
        }
        
        /**
         * Type of transition condition.
         */
        public enum ConditionType {
            ALWAYS,         // always
            NEVER,          // never
            WHEN,           // when condition
            ONLY_WHEN,      // only when condition
            ONLY_WITHIN     // only within duration of reference
        }
    }
}
