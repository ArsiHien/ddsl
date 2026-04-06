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
 *     - identifier as expression                    // Direct definition
 *     - identifier from serviceExpression           // Fetch from service
 *     - identifier created from expression          // Object construction
 *     - identifier calculated by serviceExpression  // Delegated computation
 *     - identifier fetched from serviceExpression   // Explicit retrieval
 *     - identifier determined by expression         // Rule-based evaluation
 *     - identifier mapped from expression           // Transformation
 * </pre>
 * 
 * Example:
 * <pre>
 * given:
 *     - subtotal as sum of order lines subtotals
 *     - customer tier from customer service
 *     - order lines created from cart items
 *     - total amount calculated by pricing service
 *     - product details fetched from product service
 *     - priority determined by total amount
 *     - order lines mapped from cart items
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
    public ClauseType clauseType() {
        return ClauseType.GIVEN;
    }
    
    /**
     * A single statement within a given clause.
     */
    public record GivenStatement(
        SourceSpan span,
        String identifier,
        GivenStatementType type,
        Expr expression,
        String serviceName,           // For service-based statements: "pricing service"
        List<String> serviceArguments // Additional arguments for service calls
    ) {
        public GivenStatement {
            serviceArguments = serviceArguments != null ? List.copyOf(serviceArguments) : List.of();
        }
        
        /**
         * Simplified constructor for non-service statements.
         */
        public GivenStatement(SourceSpan span, String identifier, GivenStatementType type, Expr expression) {
            this(span, identifier, type, expression, null, List.of());
        }
        
        public enum GivenStatementType {
            /** "identifier as expression" - Direct definition/computation */
            AS,
            
            /** "identifier from service" - Fetch from external source */
            FROM,
            
            /** "identifier created from expression" - Object construction */
            CREATED_FROM,
            
            /** "identifier calculated by service" - Delegated computation */
            CALCULATED_BY,
            
            /** "identifier fetched from service" - Explicit retrieval */
            FETCHED_FROM,
            
            /** "identifier determined by expression" - Rule-based evaluation */
            DETERMINED_BY,
            
            /** "identifier mapped from expression" - Transformation */
            MAPPED_FROM
        }
    }
}
