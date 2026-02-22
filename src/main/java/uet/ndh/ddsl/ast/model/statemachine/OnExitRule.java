package uet.ndh.ddsl.ast.model.statemachine;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.stmt.Stmt;

import java.util.List;

/**
 * Represents an "on exit" rule in a state machine.
 * 
 * Syntax:
 * <pre>
 * on exit:
 *     - leaving StateName:
 *         - action1
 *         - action2
 * </pre>
 * 
 * Examples:
 * <pre>
 * on exit:
 *     - leaving Disputed:
 *         - record dispute resolved at as now
 *         - notify customer about resolution
 * </pre>
 */
public record OnExitRule(
    SourceSpan span,
    String stateName,
    List<Stmt> statements
) {
    
    public OnExitRule {
        statements = statements != null ? List.copyOf(statements) : List.of();
    }
    
    /**
     * Factory method.
     */
    public static OnExitRule of(SourceSpan span, String stateName, List<Stmt> statements) {
        return new OnExitRule(span, stateName, statements);
    }
}
