package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;

import java.util.List;

/**
 * Represents a flow clause used in Use Cases to orchestrate steps.
 * 
 * The flow clause coordinates the sequence of operations in a use case,
 * allowing nested require, given, then, and return clauses.
 * 
 * Syntax:
 * <pre>
 * flow:
 *     require that:
 *         - conditions
 *     given:
 *         - transformations
 *     then:
 *         - actions
 *     return success with value
 * </pre>
 * 
 * Example:
 * <pre>
 * flow:
 *     require that:
 *         - customer exists
 *         - all items are in stock
 *     
 *     given:
 *         - order created from cart items with customer ID
 *     
 *     then:
 *         - save order to repository
 *     
 *     return success with order ID
 * </pre>
 * 
 * Pure data record.
 */
public record FlowClause(
    SourceSpan span,
    List<FlowStep> steps
) implements Clause {
    
    public FlowClause {
        steps = steps != null ? List.copyOf(steps) : List.of();
    }
    
    @Override
    public ClauseType type() {
        return ClauseType.FLOW;
    }
    
    /**
     * A single step within a flow clause.
     * Each step can be a require, given, then, or return clause.
     */
    public record FlowStep(
        SourceSpan span,
        FlowStepType stepType,
        Clause clause  // The actual clause (RequireClause, GivenClause, ThenClause, or ReturnClause)
    ) {
        public enum FlowStepType {
            REQUIRE,
            GIVEN,
            THEN,
            RETURN
        }
    }
}
