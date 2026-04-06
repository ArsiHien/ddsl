package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;
import uet.ndh.ddsl.ast.expr.Expr;

import java.util.List;

/**
 * Represents a then clause that delineates state mutation and conditional execution flow.
 * 
 * The then clause represents the finality of the declarative sequence where the system's
 * state is updated. It supports various statement types for state transitions.
 * 
 * Syntax:
 * <pre>
 * then:
 *     - set identifier to expression
 *     - change identifier to expression
 *     - record identifier as expression
 *     - calculate identifier as expression
 *     - add expression to identifier
 *     - remove expression from identifier
 *     - enable identifier
 *     - disable identifier
 *     - if condition: [statements] otherwise: [statements]
 *     - for each item in collection: [statements]
 * </pre>
 * 
 * Example:
 * <pre>
 * then:
 *     - set status to Pending
 *     - record created_time as now
 *     - enable express delivery
 *     - if total_amount &gt; 10000000:
 *         - set priority to High
 *     otherwise:
 *         - set priority to Normal
 *     - for each item in items:
 *         - add new order line to order lines
 * </pre>
 * 
 * Pure data record.
 */
public record ThenClause(
    SourceSpan span,
    List<ThenStatement> statements
) implements Clause {
    
    public ThenClause {
        statements = statements != null ? List.copyOf(statements) : List.of();
    }
    
    @Override
    public ClauseType clauseType() {
        return ClauseType.THEN;
    }
    
    /**
     * A single statement within a then clause.
     */
    public record ThenStatement(
        SourceSpan span,
        ThenStatementType type,
        String target,
        Expr expression,
        NaturalLanguageCondition condition,
        List<ThenStatement> nestedStatements,
        List<ThenStatement> elseStatements,
        List<ElseIfBranch> elseIfBranches,       // For "otherwise if" chains
        String loopVariable                       // For "for each item in collection"
    ) {
        public ThenStatement {
            nestedStatements = nestedStatements != null ? List.copyOf(nestedStatements) : List.of();
            elseStatements = elseStatements != null ? List.copyOf(elseStatements) : List.of();
            elseIfBranches = elseIfBranches != null ? List.copyOf(elseIfBranches) : List.of();
        }
        
        /**
         * Simplified factory for simple statements (set, change, record, etc.).
         */
        public static ThenStatement simple(SourceSpan span, ThenStatementType type, 
                String target, Expr expression) {
            return new ThenStatement(span, type, target, expression, null, 
                List.of(), List.of(), List.of(), null);
        }
        
        /**
         * Factory for enable/disable statements.
         */
        public static ThenStatement toggle(SourceSpan span, boolean enable, String target) {
            return new ThenStatement(span, enable ? ThenStatementType.ENABLE : ThenStatementType.DISABLE, 
                target, null, null, List.of(), List.of(), List.of(), null);
        }
        
        /**
         * Factory for if statements.
         */
        public static ThenStatement ifStatement(SourceSpan span, NaturalLanguageCondition condition,
                List<ThenStatement> thenBranch, List<ElseIfBranch> elseIfBranches, 
                List<ThenStatement> elseBranch) {
            return new ThenStatement(span, ThenStatementType.IF, null, null, condition, 
                thenBranch, elseBranch, elseIfBranches, null);
        }
        
        /**
         * Factory for foreach statements.
         */
        public static ThenStatement forEach(SourceSpan span, String loopVariable, 
                String collection, List<ThenStatement> body) {
            return new ThenStatement(span, ThenStatementType.FOR_EACH, collection, null, null, 
                body, List.of(), List.of(), loopVariable);
        }
        
        /**
         * Represents an "otherwise if" branch in a conditional.
         */
        public record ElseIfBranch(
            SourceSpan span,
            NaturalLanguageCondition condition,
            List<ThenStatement> statements
        ) {
            public ElseIfBranch {
                statements = statements != null ? List.copyOf(statements) : List.of();
            }
        }
        
        public enum ThenStatementType {
            /** "set identifier to expression" - General assignment */
            SET,
            
            /** "change identifier to expression" - State transition (emphasizes change) */
            CHANGE,
            
            /** "record identifier as expression" - Capturing information (timestamps, logs) */
            RECORD,
            
            /** "calculate identifier as expression" - Derived value assignment */
            CALCULATE,
            
            /** "create identifier from expression" - Object creation */
            CREATE,
            
            /** "add expression to identifier" - Collection mutation (add) */
            ADD,
            
            /** "remove expression from identifier" - Collection mutation (remove) */
            REMOVE,
            
            /** "enable identifier" - Boolean flag set to true */
            ENABLE,
            
            /** "disable identifier" - Boolean flag set to false */
            DISABLE,
            
            /** "if condition: statements" - Conditional block */
            IF,
            
            /** "for each item in collection: statements" - Loop */
            FOR_EACH,
            
            /** Method call statement: "identifier.method(args)" */
            METHOD_CALL
        }
    }
}
