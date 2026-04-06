package uet.ndh.ddsl.ast.model.entity;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;

/**
 * Represents a reference to a domain event that an entity can emit.
 * Pure data record.
 */
public record EventReferenceDecl(
    SourceSpan span,
    String eventName,
    TypeRef eventType,
    String triggerDescription
) {
}
