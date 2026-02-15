package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;

import java.util.List;

/**
 * Represents a require clause that defines preconditions and guard logic.
 * 
 * The require clause functions as a set of guard conditions. In the generated
 * implementation, these predicates are translated into validation logic that
 * enforces domain constraints.
 * 
 * Syntax:
 * <pre>
 * require that:
 *     - condition1, otherwise "error message 1"
 *     - condition2, otherwise "error message 2"
 * </pre>
 * 
 * Collection-based conditions utilize first-order logic quantifiers:
 * - Universal: "all items have property" (∀x ∈ I: P(x))
 * - Existential: "any item has property" (∃x ∈ I: P(x))
 * - Negated existential: "no item has property" (¬∃x ∈ I: P(x))
 * 
 * Pure data record.
 */
public record RequireClause(
    SourceSpan span,
    List<RequireCondition> conditions
) implements Clause {
    
    public RequireClause {
        conditions = conditions != null ? List.copyOf(conditions) : List.of();
    }
    
    @Override
    public ClauseType type() {
        return ClauseType.REQUIRE;
    }
    
    /**
     * A single condition within a require clause.
     */
    public record RequireCondition(
        SourceSpan span,
        NaturalLanguageCondition condition,
        String errorMessage
    ) {}
}
