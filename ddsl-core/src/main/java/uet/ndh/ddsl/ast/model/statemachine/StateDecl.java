package uet.ndh.ddsl.ast.model.statemachine;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Represents a state declaration within a state machine.
 * 
 * Syntax:
 * <pre>
 *     - StateName
 *     - StateName (initial)
 *     - StateName (final)
 *     - StateName (initial, final)
 * </pre>
 * 
 * Examples:
 * <pre>
 *     - Pending (initial)
 *     - Confirmed
 *     - Delivered (final)
 *     - Cancelled (final)
 * </pre>
 */
public record StateDecl(
    SourceSpan span,
    String name,
    boolean isInitial,
    boolean isFinal
) {
    
    /**
     * Factory for regular state.
     */
    public static StateDecl of(SourceSpan span, String name) {
        return new StateDecl(span, name, false, false);
    }
    
    /**
     * Factory for initial state.
     */
    public static StateDecl initial(SourceSpan span, String name) {
        return new StateDecl(span, name, true, false);
    }
    
    /**
     * Factory for final state.
     */
    public static StateDecl finalState(SourceSpan span, String name) {
        return new StateDecl(span, name, false, true);
    }
    
    /**
     * Factory for initial and final state (single-state machine).
     */
    public static StateDecl initialAndFinal(SourceSpan span, String name) {
        return new StateDecl(span, name, true, true);
    }
}
