package uet.ndh.ddsl.ast.model;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a complete DDSL module (file).
 * This is the root of the AST.
 * 
 * Pure data record - no logic except accept().
 */
public record ModuleDecl(
    SourceSpan span,
    String name,
    String basePackage,
    List<BoundedContextDecl> boundedContexts,
    String documentation
) implements AstNode {
    
    public ModuleDecl {
        boundedContexts = boundedContexts != null ? List.copyOf(boundedContexts) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitModule(this);
    }
}
