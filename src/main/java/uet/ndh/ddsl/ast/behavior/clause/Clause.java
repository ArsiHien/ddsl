package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Base record for all behavior clauses.
 * 
 * Sealed interface permitting all clause types used in behaviors, 
 * domain services, factories, and use cases.
 */
public sealed interface Clause permits RequireClause, GivenClause, ThenClause, EmitClause, ReturnClause, FlowClause, ErrorAccumulationClause {
    SourceSpan span();
    ClauseType clauseType();
    
    /**
     * Type of clause in behavior specification.
     */
    enum ClauseType {
        REQUIRE,  // Preconditions
        GIVEN,    // Data transformations
        THEN,     // State mutations
        EMIT,     // Event emission
        RETURN,   // Return value
        FLOW      // Use case orchestration
    }
}
