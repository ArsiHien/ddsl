package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a specification condition expression.
 * 
 * Syntax:
 * <pre>
 *     expression satisfies SpecificationRef
 *     expression does not satisfy SpecificationRef
 *     expression matches SpecificationRef
 *     expression is eligible for SpecificationRef
 * </pre>
 * 
 * SpecificationRef can be:
 * <pre>
 *     Identifier                              // Simple spec
 *     Identifier(ArgumentList)                // Parameterized spec
 *     SpecificationRef and SpecificationRef   // Composite AND
 *     SpecificationRef or SpecificationRef    // Composite OR
 *     not SpecificationRef                    // Negation
 * </pre>
 * 
 * Examples:
 * <pre>
 *     customer satisfies ActiveCustomer
 *     order satisfies EligibleForDiscount(50000)
 *     customer satisfies ActiveCustomer and VIPCustomer
 *     order does not satisfy HighRiskOrder
 * </pre>
 */
public record SpecificationCondition(
    SourceSpan span,
    Expr subject,
    SpecificationConditionType conditionType,
    SpecificationRef specificationRef
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return null;
    }
    
    /**
     * Factory for "satisfies".
     */
    public static SpecificationCondition satisfies(SourceSpan span, Expr subject, 
                                                    SpecificationRef spec) {
        return new SpecificationCondition(span, subject, 
            SpecificationConditionType.SATISFIES, spec);
    }
    
    /**
     * Factory for "does not satisfy".
     */
    public static SpecificationCondition doesNotSatisfy(SourceSpan span, Expr subject,
                                                         SpecificationRef spec) {
        return new SpecificationCondition(span, subject,
            SpecificationConditionType.DOES_NOT_SATISFY, spec);
    }
    
    /**
     * Factory for "matches".
     */
    public static SpecificationCondition matches(SourceSpan span, Expr subject,
                                                  SpecificationRef spec) {
        return new SpecificationCondition(span, subject,
            SpecificationConditionType.MATCHES, spec);
    }
    
    /**
     * Factory for "is eligible for".
     */
    public static SpecificationCondition isEligibleFor(SourceSpan span, Expr subject,
                                                        SpecificationRef spec) {
        return new SpecificationCondition(span, subject,
            SpecificationConditionType.IS_ELIGIBLE_FOR, spec);
    }
    
    /**
     * Type of specification condition.
     */
    public enum SpecificationConditionType {
        SATISFIES,
        DOES_NOT_SATISFY,
        MATCHES,
        IS_ELIGIBLE_FOR
    }
    
    /**
     * Reference to a specification (simple, parameterized, or composite).
     */
    public sealed interface SpecificationRef 
        permits SpecificationRef.Simple, SpecificationRef.Parameterized,
                SpecificationRef.Composite, SpecificationRef.Negation {
        
        /**
         * Simple specification reference.
         */
        record Simple(String name) implements SpecificationRef {}
        
        /**
         * Parameterized specification reference.
         */
        record Parameterized(String name, List<Expr> arguments) implements SpecificationRef {
            public Parameterized {
                arguments = arguments != null ? List.copyOf(arguments) : List.of();
            }
        }
        
        /**
         * Composite specification (AND/OR).
         */
        record Composite(
            SpecificationRef left,
            CompositeType compositeType,
            SpecificationRef right
        ) implements SpecificationRef {
            
            public enum CompositeType {
                AND, OR
            }
        }
        
        /**
         * Negation of a specification.
         */
        record Negation(SpecificationRef inner) implements SpecificationRef {}
    }
}
