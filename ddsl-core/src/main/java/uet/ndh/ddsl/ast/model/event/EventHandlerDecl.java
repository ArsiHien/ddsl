package uet.ndh.ddsl.ast.model.event;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents an Event Handler declaration.
 * An event handler listens for a specific domain event and executes behaviors.
 *
 * Syntax:
 * <pre>
 * EventHandler HandlerName for EventName {
 *     when handling X:
 *         then ...
 * }
 * </pre>
 *
 * Examples:
 * <pre>
 * EventHandler NotificationHandler for ReservationPlaced {
 *     when handling onPlaced:
 *         then send confirmation email
 * }
 *
 * EventHandler InventoryHandler for OrderCancelled {
 *     when handling onCancelled:
 *         then restore stock levels
 *         then notify warehouse
 * }
 * </pre>
 */
public record EventHandlerDecl(
    SourceSpan span,
    String name,
    String targetEventName,
    List<BehaviorDecl> behaviors,
    String documentation
) implements AstNode {

    public EventHandlerDecl {
        behaviors = behaviors != null ? List.copyOf(behaviors) : List.of();
    }

    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitEventHandler(this);
    }
}