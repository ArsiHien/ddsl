package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Base record for all behavior clauses.
 */
public sealed interface Clause permits RequireClause, GivenClause, ThenClause, EmitClause {
    SourceSpan span();
    ClauseType type();
    
    /**
     * Type of clause in behavior specification.
     */
    enum ClauseType {
        REQUIRE,  // Preconditions
        GIVEN,    // Data transformations
        THEN,     // State mutations
        EMIT      // Event emission
    }
}
