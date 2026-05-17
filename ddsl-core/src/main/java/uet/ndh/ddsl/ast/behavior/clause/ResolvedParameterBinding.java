package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

/**
 * Represents a resolved parameter binding for event emission.
 * Captures how an event field parameter is bound to a source.
 *
 * <p>During semantic analysis, emit clause parameters are resolved to their
 * source bindings. This record captures the result of that resolution.</p>
 *
 * @param span           source location of this binding
 * @param parameterName  event field name (e.g., "guestName", "bookingId")
 * @param kind           how the parameter was resolved
 * @param sourceName     source variable/field name
 * @param sourceExpression optional full expression for complex bindings
 */
public record ResolvedParameterBinding(
    SourceSpan span,
    String parameterName,
    BindingKind kind,
    String sourceName,
    Expr sourceExpression
) {

    /**
     * Describes how a parameter binding was resolved.
     */
    public enum BindingKind {
        /** Bound via explicit with-clause: {@code emit Event with arg} */
        EXPLICIT,
        /** Resolved from aggregate field */
        FIELD,
        /** Resolved from behavior parameter */
        PARAMETER,
        /** Resolved from given-clause variable */
        GIVEN_LOCAL,
        /** Auto-bound to Instant.now() for temporal fields */
        TEMPORAL_NOW,
        /** Could not be resolved (error case) */
        UNRESOLVED
    }
}