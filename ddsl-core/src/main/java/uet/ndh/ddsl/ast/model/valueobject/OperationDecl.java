package uet.ndh.ddsl.ast.model.valueobject;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.member.ParameterDecl;

import java.util.List;

/**
 * Represents an operation in a value object.
 * Operations are side-effect-free and functional.
 * 
 * Pure data record.
 */
public record OperationDecl(
    SourceSpan span,
    String name,
    TypeRef returnType,
    List<ParameterDecl> parameters,
    Expr expression,
    String documentation
) {
    
    public OperationDecl {
        parameters = parameters != null ? List.copyOf(parameters) : List.of();
    }
}
