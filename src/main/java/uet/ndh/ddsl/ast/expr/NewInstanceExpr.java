package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * A new instance creation expression.
 * Pure data record.
 */
public record NewInstanceExpr(
    SourceSpan span,
    TypeRef type,
    List<Expr> arguments
) implements Expr {
    
    public NewInstanceExpr {
        arguments = arguments != null ? List.copyOf(arguments) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitNewInstanceExpr(this);
    }
}
