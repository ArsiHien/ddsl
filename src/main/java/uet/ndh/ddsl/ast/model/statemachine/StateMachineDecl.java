package uet.ndh.ddsl.ast.model.statemachine;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a state machine declaration.
 * 
 * Syntax:
 * <pre>
 * state machine for FieldName {
 *     states:
 *         - Pending (initial)
 *         - Confirmed
 *         - Completed (final)
 *     
 *     transitions:
 *         - Pending -> Confirmed: when payment received
 *         - Confirmed -> Completed: always
 *     
 *     guards:
 *         - cannot transition from A to B when condition
 *     
 *     on entry:
 *         - entering State: actions
 *     
 *     on exit:
 *         - leaving State: actions
 * }
 * </pre>
 */
public record StateMachineDecl(
    SourceSpan span,
    String name,                           // Optional name for standalone state machines
    String forField,                       // The field this state machine controls
    List<StateDecl> states,
    List<TransitionRule> transitions,
    List<GuardRule> guards,
    List<OnEntryRule> onEntryRules,
    List<OnExitRule> onExitRules,
    String documentation
) implements AstNode {
    
    public StateMachineDecl {
        states = states != null ? List.copyOf(states) : List.of();
        transitions = transitions != null ? List.copyOf(transitions) : List.of();
        guards = guards != null ? List.copyOf(guards) : List.of();
        onEntryRules = onEntryRules != null ? List.copyOf(onEntryRules) : List.of();
        onExitRules = onExitRules != null ? List.copyOf(onExitRules) : List.of();
    }
    
    /**
     * Get the initial state.
     */
    public StateDecl getInitialState() {
        return states.stream()
            .filter(StateDecl::isInitial)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get all final states.
     */
    public List<StateDecl> getFinalStates() {
        return states.stream()
            .filter(StateDecl::isFinal)
            .toList();
    }
    
    /**
     * Check if a transition from source to target is defined.
     */
    public boolean hasTransition(String source, String target) {
        return transitions.stream()
            .anyMatch(t -> t.matchesSource(source) && t.targetState().equals(target));
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        // TODO: Add visitStateMachine to AstVisitor
        return null;
    }
}
