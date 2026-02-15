package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

import java.util.List;

/**
 * Represents a given clause that performs data transformations and defines temporary aliases.
 * 
 * The given clause is utilized to perform data transformations and define temporary aliases.
 * This mechanism enhances readability by isolating complex expressions from the primary
 * state transition logic.
 * 
 * Syntax:
 * <pre>
 * given:
 *     - identifier created from expression
 *     - identifier calculated by expression
 *     - identifier as expression
 * </pre>
 * 
 * Example:
 * <pre>
 * given:
 *     - order_lines created from cart_items
 *     - subtotal as sum of order_lines.subtotals
 *     - final_total as subtotal minus discount
 * </pre>
 * 
 * Pure data record.
 */
public record GivenClause(
    SourceSpan span,
    List<GivenStatement> statements
) implements Clause {
    
    public GivenClause {
        statements = statements != null ? List.copyOf(statements) : List.of();
    }
    
    @Override
    public ClauseType type() {
        return ClauseType.GIVEN;
    }
    
    /**
     * A single statement within a given clause.
     */
    public record GivenStatement(
        SourceSpan span,
        String identifier,
        GivenStatementType type,
        Expr expression
    ) {
        public enum GivenStatementType {
            CREATED_FROM,
            CALCULATED_BY,
            AS
        }
    }
}
