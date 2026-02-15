package uet.ndh.ddsl.ast.model.aggregate;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.member.InvariantDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.factory.FactoryDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a DDD Aggregate - a cluster of domain objects
 * that can be treated as a single unit.
 * 
 * Aggregates delineate consistency boundaries and transactional units in the domain model.
 * They encompass field definitions, invariants, and behavior specifications.
 * 
 * Behaviors denote domain operations that alter aggregate state while upholding
 * invariants and generating domain events.
 * 
 * Pure data record - no logic except accept().
 * 
 * Example DDSL:
 * <pre>
 * aggregate Order {
 *     root OrderRoot {
 *         id: OrderId
 *         customerId: CustomerId
 *         items: List&lt;OrderItem&gt;
 *     }
 *     entity OrderItem { ... }
 *     invariant "Order must have at least one item" { items.size() &gt; 0 }
 *     
 *     when "placing an order" with items:
 *         require that: items is not empty
 *         then: set status to Placed
 *         and emit OrderPlaced event
 * }
 * </pre>
 */
public record AggregateDecl(
    SourceSpan span,
    String name,
    EntityDecl root,
    List<EntityDecl> entities,
    List<ValueObjectDecl> valueObjects,
    List<InvariantDecl> invariants,
    List<BehaviorDecl> behaviors,
    List<MethodDecl> commands,
    List<FactoryDecl> factories,
    List<DomainEventDecl> events,
    String documentation
) implements AstNode {
    
    public AggregateDecl {
        entities = entities != null ? List.copyOf(entities) : List.of();
        valueObjects = valueObjects != null ? List.copyOf(valueObjects) : List.of();
        invariants = invariants != null ? List.copyOf(invariants) : List.of();
        behaviors = behaviors != null ? List.copyOf(behaviors) : List.of();
        commands = commands != null ? List.copyOf(commands) : List.of();
        factories = factories != null ? List.copyOf(factories) : List.of();
        events = events != null ? List.copyOf(events) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitAggregate(this);
    }
}
