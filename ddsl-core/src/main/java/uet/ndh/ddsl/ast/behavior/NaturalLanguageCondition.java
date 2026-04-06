package uet.ndh.ddsl.ast.behavior;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

import java.util.List;

/**
 * Represents a natural language condition used in require and then clauses.
 * 
 * Supports various patterns from the natural language DSL:
 * 
 * Collection conditions:
 * - "items is not empty"
 * - "all items have quantity greater than 0"
 * - "any item has discount flag"
 * - "no item has expired status"
 * 
 * Comparison conditions:
 * - "total amount is greater than 0"
 * - "quantity is at least 1"
 * - "order count is at most 100"
 * - "delivery date is within 30 days"
 * - "total amount exceeds minimum order value"
 * 
 * Existence conditions:
 * - "customer exists in system"
 * - "product does not exist"
 * 
 * State conditions:
 * - "current status is Pending"
 * - "current status is one of [Pending, Confirmed]"
 * - "payment has been processed"
 * 
 * Pure data record.
 */
public record NaturalLanguageCondition(
    SourceSpan span,
    String rawText,
    ConditionType type,
    Expr leftExpression,
    Expr rightExpression,
    ComparisonOperator comparisonOperator,
    String quantifierTarget,       // For collection conditions: "items", "orderLines"
    String propertyPath,           // For "all/any/no X have Y": the property path
    List<String> allowedValues,    // For "is one of [...]"
    List<String> tokens
) {
    
    public NaturalLanguageCondition {
        tokens = tokens != null ? List.copyOf(tokens) : List.of();
        allowedValues = allowedValues != null ? List.copyOf(allowedValues) : List.of();
    }
    
    /**
     * Factory method for simple conditions.
     */
    public static NaturalLanguageCondition simple(SourceSpan span, String rawText, Expr expression) {
        return new NaturalLanguageCondition(span, rawText, ConditionType.SIMPLE, 
            expression, null, null, null, null, List.of(), List.of());
    }
    
    /**
     * Factory method for comparison conditions.
     */
    public static NaturalLanguageCondition comparison(SourceSpan span, String rawText, 
            Expr left, ComparisonOperator op, Expr right) {
        return new NaturalLanguageCondition(span, rawText, ConditionType.COMPARISON, 
            left, right, op, null, null, List.of(), List.of());
    }
    
    /**
     * Factory method for collection empty/not-empty conditions.
     */
    public static NaturalLanguageCondition collectionEmpty(SourceSpan span, String rawText, 
            String collection, boolean isEmpty) {
        return new NaturalLanguageCondition(span, rawText, 
            isEmpty ? ConditionType.IS_EMPTY : ConditionType.IS_NOT_EMPTY, 
            null, null, null, collection, null, List.of(), List.of());
    }
    
    /**
     * Factory method for universal quantifier conditions (all items have...).
     */
    public static NaturalLanguageCondition universal(SourceSpan span, String rawText, 
            String collection, String property, Expr condition) {
        return new NaturalLanguageCondition(span, rawText, ConditionType.UNIVERSAL, 
            condition, null, null, collection, property, List.of(), List.of());
    }
    
    /**
     * Factory method for existential quantifier conditions (any item has...).
     */
    public static NaturalLanguageCondition existential(SourceSpan span, String rawText, 
            String collection, String property, Expr condition) {
        return new NaturalLanguageCondition(span, rawText, ConditionType.EXISTENTIAL, 
            condition, null, null, collection, property, List.of(), List.of());
    }
    
    /**
     * Factory method for negated existential conditions (no item has...).
     */
    public static NaturalLanguageCondition negatedExistential(SourceSpan span, String rawText, 
            String collection, String property, Expr condition) {
        return new NaturalLanguageCondition(span, rawText, ConditionType.NEGATED_EXISTENTIAL, 
            condition, null, null, collection, property, List.of(), List.of());
    }
    
    /**
     * Factory method for existence conditions (X exists in system).
     */
    public static NaturalLanguageCondition existence(SourceSpan span, String rawText, 
            String identifier, boolean exists) {
        return new NaturalLanguageCondition(span, rawText, 
            exists ? ConditionType.EXISTS_IN_SYSTEM : ConditionType.DOES_NOT_EXIST, 
            null, null, null, identifier, null, List.of(), List.of());
    }
    
    /**
     * Factory method for state conditions (status is X).
     */
    public static NaturalLanguageCondition state(SourceSpan span, String rawText, 
            Expr subject, Expr expectedState) {
        return new NaturalLanguageCondition(span, rawText, ConditionType.STATE_IS, 
            subject, expectedState, null, null, null, List.of(), List.of());
    }
    
    /**
     * Factory method for state enumeration conditions (status is one of [...]).
     */
    public static NaturalLanguageCondition stateOneOf(SourceSpan span, String rawText, 
            Expr subject, List<String> allowedStates) {
        return new NaturalLanguageCondition(span, rawText, ConditionType.STATE_ONE_OF, 
            subject, null, null, null, null, allowedStates, List.of());
    }
    
    /**
     * Factory method for past-tense state conditions (X has been Y).
     */
    public static NaturalLanguageCondition hasBeen(SourceSpan span, String rawText, 
            String subject, String pastParticiple) {
        return new NaturalLanguageCondition(span, rawText, ConditionType.HAS_BEEN, 
            null, null, null, subject, pastParticiple, List.of(), List.of());
    }
    
    /**
     * Type of natural language condition.
     */
    public enum ConditionType {
        // Simple
        SIMPLE,              // Direct boolean expression
        
        // Collection conditions
        IS_EMPTY,            // "is empty"
        IS_NOT_EMPTY,        // "is not empty"
        UNIVERSAL,           // "all items have..."
        EXISTENTIAL,         // "any item has..."
        NEGATED_EXISTENTIAL, // "no item has..."
        
        // Comparison conditions
        COMPARISON,          // Uses ComparisonOperator
        
        // Existence conditions
        EXISTS_IN_SYSTEM,    // "X exists in system"
        DOES_NOT_EXIST,      // "X does not exist"
        
        // State conditions
        STATE_IS,            // "status is Pending"
        STATE_ONE_OF,        // "status is one of [...]"
        HAS_BEEN,            // "payment has been processed"
        
        // Legacy types for compatibility
        IS_NULL,
        CONTAINS,
        CUSTOM
    }
    
    /**
     * Comparison operators for natural language comparisons.
     */
    public enum ComparisonOperator {
        EQUAL,            // "is equal to", "is", "equals"
        NOT_EQUAL,        // "is not"
        GREATER_THAN,     // "is greater than", "exceeds"
        LESS_THAN,        // "is less than"
        AT_LEAST,         // "is at least"
        AT_MOST,          // "is at most"
        WITHIN            // "is within"
    }
}
