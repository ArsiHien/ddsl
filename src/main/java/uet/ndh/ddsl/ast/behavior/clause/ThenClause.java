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
 *     - create identifier from expression
 *     - add expression to identifier
 *     - remove expression from identifier
 *     - if condition: [statements] otherwise: [statements]
 *     - for each item in collection: [statements]
 * </pre>
 * 
 * Example:
 * <pre>
 * then:
 *     - set status to Pending
 *     - record created_time as now
 *     - if total_amount &gt; 10000000:
 *         - set priority to High
 *     - otherwise:
 *         - set priority to Normal
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
    public ClauseType type() {
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
        List<ThenStatement> elseStatements
    ) {
        public ThenStatement {
            nestedStatements = nestedStatements != null ? List.copyOf(nestedStatements) : List.of();
            elseStatements = elseStatements != null ? List.copyOf(elseStatements) : List.of();
        }
        
        public enum ThenStatementType {
            SET,
            CHANGE,
            RECORD,
            CALCULATE,
            CREATE,
            ADD,
            REMOVE,
            IF,
            FOR_EACH
        }
    }
}
