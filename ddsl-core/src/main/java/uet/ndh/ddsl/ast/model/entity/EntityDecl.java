package uet.ndh.ddsl.ast.model.entity;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.member.ConstructorDecl;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.InvariantDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a DDD Entity - an object with identity that persists
 * through time and state changes.
 * 
 * Entities have identity and lifecycle. As per the language design, 
 * entities (especially aggregate roots) can have behaviors that alter 
 * state while upholding invariants and generating domain events.
 * 
 * Pure data record - no logic except accept().
 * 
 * Example DDSL:
 * <pre>
 * entity Customer {
 *     id: CustomerId
 *     name: String
 *     email: Email
 *     
 *     when "changing name" with newName:
 *         require that: newName is not blank
 *         then: set name to newName
 * }
 * </pre>
 */
public record EntityDecl(
    SourceSpan span,
    String name,
    IdentityFieldDecl identity,
    List<FieldDecl> fields,
    List<MethodDecl> methods,
    List<ConstructorDecl> constructors,
    List<BehaviorDecl> behaviors,
    List<InvariantDecl> invariants,
    List<EventReferenceDecl> domainEvents,
    boolean isAggregateRoot,
    String documentation
) implements AstNode {
    
    public EntityDecl {
        fields = fields != null ? List.copyOf(fields) : List.of();
        methods = methods != null ? List.copyOf(methods) : List.of();
        constructors = constructors != null ? List.copyOf(constructors) : List.of();
        behaviors = behaviors != null ? List.copyOf(behaviors) : List.of();
        invariants = invariants != null ? List.copyOf(invariants) : List.of();
        domainEvents = domainEvents != null ? List.copyOf(domainEvents) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitEntity(this);
    }
}
