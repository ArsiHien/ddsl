package uet.ndh.ddsl.ast.model.valueobject;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.InvariantDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a DDD Value Object - an immutable object with no identity,
 * defined only by its attributes.
 * 
 * Value objects embody immutable domain concepts defined by their attributes 
 * rather than identity. DDSL facilitates the definition of operations for 
 * value object behaviors that are side-effect-free and functional.
 * 
 * Pure data record - no logic except accept().
 * 
 * Example DDSL:
 * <pre>
 * valueobject Money {
 *     amount: BigDecimal
 *     currency: Currency
 *     
 *     operation add(other: Money): Money {
 *         require that: currency equals other.currency
 *         return Money(amount + other.amount, currency)
 *     }
 *     
 *     invariant "Amount must be positive" { amount &gt;= 0 }
 * }
 * </pre>
 */
public record ValueObjectDecl(
    SourceSpan span,
    String name,
    List<FieldDecl> fields,
    List<MethodDecl> methods,
    List<OperationDecl> operations,
    List<InvariantDecl> invariants,
    List<ValidationMethodDecl> validations,
    String documentation
) implements AstNode {
    
    public ValueObjectDecl {
        fields = fields != null ? List.copyOf(fields) : List.of();
        methods = methods != null ? List.copyOf(methods) : List.of();
        operations = operations != null ? List.copyOf(operations) : List.of();
        invariants = invariants != null ? List.copyOf(invariants) : List.of();
        validations = validations != null ? List.copyOf(validations) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitValueObject(this);
    }
}
