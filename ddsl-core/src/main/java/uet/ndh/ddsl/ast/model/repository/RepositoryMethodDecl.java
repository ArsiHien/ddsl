package uet.ndh.ddsl.ast.model.repository;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.member.ParameterDecl;

import java.util.List;

/**
 * Represents a method in a repository interface.
 * Pure data record.
 */
public record RepositoryMethodDecl(
    SourceSpan span,
    String name,
    TypeRef returnType,
    List<ParameterDecl> parameters,
    RepositoryMethodType methodType,
    String documentation
) {
    
    public RepositoryMethodDecl {
        parameters = parameters != null ? List.copyOf(parameters) : List.of();
    }
    
    /**
     * Type of repository method.
     */
    public enum RepositoryMethodType {
        FIND_BY_ID,
        FIND_ALL,
        FIND_BY,
        SAVE,
        DELETE,
        EXISTS,
        COUNT,
        CUSTOM
    }
}
