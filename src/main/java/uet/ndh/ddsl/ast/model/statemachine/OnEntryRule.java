package uet.ndh.ddsl.ast.model.statemachine;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.stmt.Stmt;

import java.util.List;

/**
 * Represents an "on entry" rule in a state machine.
 * 
 * Syntax:
 * <pre>
 * on entry:
 *     - entering StateName:
 *         - action1
 *         - action2
 * </pre>
 * 
 * Examples:
 * <pre>
 * on entry:
 *     - entering Processing:
 *         - record processing started at as now
 *         - set retry count to 0
 *     
 *     - entering Completed:
 *         - record completed at as now
 *         - enable receipt generation
 * </pre>
 */
public record OnEntryRule(
    SourceSpan span,
    String stateName,
    List<Stmt> statements
) {
    
    public OnEntryRule {
        statements = statements != null ? List.copyOf(statements) : List.of();
    }
    
    /**
     * Factory method.
     */
    public static OnEntryRule of(SourceSpan span, String stateName, List<Stmt> statements) {
        return new OnEntryRule(span, stateName, statements);
    }
}
