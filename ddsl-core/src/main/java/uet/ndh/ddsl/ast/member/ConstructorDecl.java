package uet.ndh.ddsl.ast.member;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.Visibility;
import uet.ndh.ddsl.ast.stmt.BlockStmt;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a constructor declaration.
 * Pure data record - no logic except accept().
 */
public record ConstructorDecl(
    SourceSpan span,
    String name,
    List<ParameterDecl> parameters,
    BlockStmt body,
    Visibility visibility,
    String documentation
) implements AstNode {
    
    public ConstructorDecl {
        parameters = parameters != null ? List.copyOf(parameters) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitConstructor(this);
    }
}
