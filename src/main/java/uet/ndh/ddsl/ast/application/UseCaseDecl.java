package uet.ndh.ddsl.ast.application;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.stmt.BlockStmt;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a Use Case - a single coherent unit of work
 * in the application layer.
 * 
 * Pure data record - no logic except accept().
 */
public record UseCaseDecl(
    SourceSpan span,
    String name,
    List<ParameterDecl> inputs,
    TypeRef returnType,
    BlockStmt body,
    List<BehaviorDecl> behaviors,
    List<UseCaseStepDecl> steps,
    String documentation
) implements AstNode {
    
    public UseCaseDecl {
        inputs = inputs != null ? List.copyOf(inputs) : List.of();
        behaviors = behaviors != null ? List.copyOf(behaviors) : List.of();
        steps = steps != null ? List.copyOf(steps) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitUseCase(this);
    }
}
