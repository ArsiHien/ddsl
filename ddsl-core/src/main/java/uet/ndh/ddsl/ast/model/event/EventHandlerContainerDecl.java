package uet.ndh.ddsl.ast.model.event;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents the event-handlers container block.
 *
 * Syntax:
 * <pre>
 * event-handlers {
 *     EventHandler HandlerName for EventName {
 *         when handling X:
 *             then ...
 *     }
 *     EventHandler AnotherHandler for AnotherEvent {
 *         when handling onEvent:
 *             then ...
 *     }
 * }
 * </pre>
 *
 * Examples:
 * <pre>
 * event-handlers {
 *     EventHandler NotificationHandler for ReservationPlaced {
 *         when handling onPlaced:
 *             then send confirmation email
 *     }
 *
 *     EventHandler InventoryHandler for OrderCancelled {
 *         when handling onCancelled:
 *             then restore stock levels
 *     }
 * }
 * </pre>
 */
public record EventHandlerContainerDecl(
    SourceSpan span,
    List<EventHandlerDecl> handlers,
    String documentation
) implements AstNode {

    public EventHandlerContainerDecl {
        handlers = handlers != null ? List.copyOf(handlers) : List.of();
    }

    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitEventHandlerContainer(this);
    }
}