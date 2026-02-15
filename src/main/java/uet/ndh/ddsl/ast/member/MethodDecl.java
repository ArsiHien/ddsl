package uet.ndh.ddsl.ast.member;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.common.Visibility;
import uet.ndh.ddsl.ast.stmt.BlockStmt;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a method declaration.
 * Pure data record - no logic except accept().
 */
public record MethodDecl(
    SourceSpan span,
    String name,
    TypeRef returnType,
    List<ParameterDecl> parameters,
    BlockStmt body,
    Visibility visibility,
    boolean isStatic,
    boolean isAbstract,
    MethodKind kind,
    String documentation
) implements AstNode {
    
    public MethodDecl {
        parameters = parameters != null ? List.copyOf(parameters) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitMethod(this);
    }
    
    /**
     * The kind of method in DDD terms.
     */
    public enum MethodKind {
        COMMAND,     // Mutates state
        QUERY,       // Returns data without side effects
        FACTORY,     // Creates new objects
        HANDLER,     // Handles events
        REGULAR      // General purpose method
    }
}
