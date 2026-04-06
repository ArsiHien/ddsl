package uet.ndh.ddsl.ast.model.factory;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a DDD Factory - encapsulates complex object creation.
 * 
 * Pure data record - no logic except accept().
 */
public record FactoryDecl(
    SourceSpan span,
    String name,
    TypeRef producedType,
    List<FactoryMethodDecl> creationMethods,
    List<FactoryCreationRuleDecl> creationRules,
    String documentation
) implements AstNode {
    
    public FactoryDecl {
        creationMethods = creationMethods != null ? List.copyOf(creationMethods) : List.of();
        creationRules = creationRules != null ? List.copyOf(creationRules) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitFactory(this);
    }
}
