package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * A method call expression.
 * Pure data record.
 */
public record MethodCallExpr(
    SourceSpan span,
    Expr receiver,  // nullable for static/local calls
    String methodName,
    List<Expr> arguments
) implements Expr {
    
    public MethodCallExpr {
        arguments = arguments != null ? List.copyOf(arguments) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitMethodCallExpr(this);
    }
    
    public boolean hasReceiver() {
        return receiver != null;
    }
}
