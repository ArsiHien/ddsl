package uet.ndh.ddsl.ast.model.valueobject;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

/**
 * Represents a validation method in a value object.
 * Pure data record.
 */
public record ValidationMethodDecl(
    SourceSpan span,
    String name,
    Expr condition,
    String errorMessage
) {
}
