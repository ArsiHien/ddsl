package uet.ndh.ddsl.ast.model.factory;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

/**
 * Represents a creation rule in a factory.
 * Pure data record.
 */
public record FactoryCreationRuleDecl(
    SourceSpan span,
    String name,
    Expr condition,
    String description
) {
}
