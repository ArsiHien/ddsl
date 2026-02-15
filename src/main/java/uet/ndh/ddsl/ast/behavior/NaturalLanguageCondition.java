package uet.ndh.ddsl.ast.behavior;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

import java.util.List;

/**
 * Represents a natural language condition used in require clauses.
 * 
 * Supports various patterns:
 * - Simple: "customer is active"
 * - Comparison: "amount greater than 0"
 * - Collection quantifiers: "all items have positive price"
 * 
 * Pure data record.
 */
public record NaturalLanguageCondition(
    SourceSpan span,
    String rawText,
    ConditionType type,
    Expr expression,
    List<String> tokens
) {
    
    public NaturalLanguageCondition {
        tokens = tokens != null ? List.copyOf(tokens) : List.of();
    }
    
    /**
     * Type of natural language condition.
     */
    public enum ConditionType {
        SIMPLE,              // Direct boolean expression
        COMPARISON,          // Comparison operators
        IS_NULL,             // "is null", "is not null"
        IS_EMPTY,            // "is empty", "is not empty"
        CONTAINS,            // "contains", "does not contain"
        UNIVERSAL,           // "all items have..."
        EXISTENTIAL,         // "any item has..."
        NEGATED_EXISTENTIAL, // "no item has..."
        CUSTOM               // Custom expression
    }
}
