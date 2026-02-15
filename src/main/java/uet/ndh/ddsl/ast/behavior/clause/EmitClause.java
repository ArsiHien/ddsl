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
 * </pre>
 * 
 * Example:
 * <pre>
 *     and emit OrderPlaced event
 * </pre>
 * 
 * Pure data record.
 */
public record EmitClause(
    SourceSpan span,
    String eventName,
    List<EventPropertyMapping> propertyMappings
) implements Clause {
    
    public EmitClause {
        propertyMappings = propertyMappings != null ? List.copyOf(propertyMappings) : List.of();
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
