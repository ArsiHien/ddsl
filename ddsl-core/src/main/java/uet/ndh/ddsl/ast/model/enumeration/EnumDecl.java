package uet.ndh.ddsl.ast.model.enumeration;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * First-class enum declaration in DDSL.
 */
public record EnumDecl(
    SourceSpan span,
    String name,
    List<String> values,
    String documentation
) implements AstNode {

    public EnumDecl {
        values = values != null ? List.copyOf(values) : List.of();
    }

    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitEnum(this);
    }
}
