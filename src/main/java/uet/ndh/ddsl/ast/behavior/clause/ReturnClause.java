package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

import java.util.List;

/**
 * Represents a return clause used in Domain Services, Factories, and Use Cases.
 * 
 * Syntax:
 * <pre>
 *     return expression                    // Return value
 *     return success                       // Return success (use case)
 *     return success with expression       // Return success with value
 *     return failure with "message"        // Return failure
 *     return TypeName with:                // Return object (factory)
 *         - property set to expression
 * </pre>
 * 
 * Example:
 * <pre>
 *     return final total
 *     return success
 *     return success with order ID
 *     return failure with "Insufficient funds"
 *     return Order with:
 *         - id set to new generated OrderId
 *         - status set to Pending
 * </pre>
 * 
 * Pure data record.
 */
public record ReturnClause(
    SourceSpan span,
    ReturnType returnType,
    Expr expression,                         // For EXPRESSION, SUCCESS_WITH
    String errorMessage,                     // For FAILURE
    String objectTypeName,                   // For RETURN_OBJECT (factory pattern)
    List<PropertyInitialization> properties  // For RETURN_OBJECT
) implements Clause {
    
    public ReturnClause {
        properties = properties != null ? List.copyOf(properties) : List.of();
    }
    
    /**
     * Factory for simple expression return.
     */
    public static ReturnClause expression(SourceSpan span, Expr expression) {
        return new ReturnClause(span, ReturnType.EXPRESSION, expression, null, null, List.of());
    }
    
    /**
     * Factory for success return (use case).
     */
    public static ReturnClause success(SourceSpan span) {
        return new ReturnClause(span, ReturnType.SUCCESS, null, null, null, List.of());
    }
    
    /**
     * Factory for success with value return.
     */
    public static ReturnClause successWith(SourceSpan span, Expr value) {
        return new ReturnClause(span, ReturnType.SUCCESS_WITH, value, null, null, List.of());
    }
    
    /**
     * Factory for failure return.
     */
    public static ReturnClause failure(SourceSpan span, String message) {
        return new ReturnClause(span, ReturnType.FAILURE, null, message, null, List.of());
    }
    
    /**
     * Factory for object return (factory pattern).
     */
    public static ReturnClause object(SourceSpan span, String typeName, List<PropertyInitialization> properties) {
        return new ReturnClause(span, ReturnType.RETURN_OBJECT, null, null, typeName, properties);
    }
    
    @Override
    public ClauseType type() {
        return ClauseType.RETURN;
    }
    
    /**
     * Type of return clause.
     */
    public enum ReturnType {
        /** "return expression" */
        EXPRESSION,
        
        /** "return success" */
        SUCCESS,
        
        /** "return success with expression" */
        SUCCESS_WITH,
        
        /** "return failure with message" */
        FAILURE,
        
        /** "return TypeName with: properties" (factory) */
        RETURN_OBJECT
    }
    
    /**
     * Property initialization for return object clause.
     */
    public record PropertyInitialization(
        SourceSpan span,
        String propertyName,
        Expr valueExpression
    ) {}
}
