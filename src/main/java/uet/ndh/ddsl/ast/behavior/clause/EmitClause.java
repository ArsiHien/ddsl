package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

import java.util.List;

/**
 * Represents an emit clause that handles the generation of domain events.
 * 
 * The emit clause designates domain events to be published to external bounded
 * contexts, enabling asynchronous communication and decoupling within the system.
 * 
 * Syntax:
 * <pre>
 *     and emit EventName event
 *     and emit EventName event with argument
 *     and emit EventName event with arg1 and arg2
 * </pre>
 * 
 * Example:
 * <pre>
 *     and emit OrderPlaced event
 *     and emit OrderCancelled event with reason
 *     and emit OrderUpdated event with updatedFields and updatedAt
 * </pre>
 * 
 * Pure data record.
 */
public record EmitClause(
    SourceSpan span,
    String eventName,
    List<String> eventArguments,          // Simple argument names: "reason", "updatedFields"
    List<EventPropertyMapping> propertyMappings  // Complex property mappings
) implements Clause {
    
    public EmitClause {
        eventArguments = eventArguments != null ? List.copyOf(eventArguments) : List.of();
        propertyMappings = propertyMappings != null ? List.copyOf(propertyMappings) : List.of();
    }
    
    /**
     * Simple constructor for emit without arguments.
     */
    public static EmitClause simple(SourceSpan span, String eventName) {
        return new EmitClause(span, eventName, List.of(), List.of());
    }
    
    /**
     * Constructor for emit with simple argument names.
     */
    public static EmitClause withArguments(SourceSpan span, String eventName, List<String> arguments) {
        return new EmitClause(span, eventName, arguments, List.of());
    }
    
    @Override
    public ClauseType type() {
        return ClauseType.EMIT;
    }
    
    /**
     * Maps aggregate/entity properties to event properties.
     */
    public record EventPropertyMapping(
        SourceSpan span,
        String eventProperty,
        Expr sourceExpression
    ) {}
}
