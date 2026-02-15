package uet.ndh.ddsl.ast.model.factory;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.stmt.BlockStmt;

import java.util.List;

/**
 * Represents a factory method for creating objects.
 * Pure data record.
 */
public record FactoryMethodDecl(
    SourceSpan span,
    String name,
    TypeRef returnType,
    List<ParameterDecl> parameters,
    BlockStmt body,
    String documentation
) {
    
    public FactoryMethodDecl {
        parameters = parameters != null ? List.copyOf(parameters) : List.of();
    }
}
