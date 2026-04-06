package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * A binary expression (left op right).
 * Pure data record.
 */
public record BinaryExpr(
    SourceSpan span,
    Expr left,
    BinaryOperator operator,
    Expr right
) implements Expr {
    
    public enum BinaryOperator {
        // Arithmetic
        PLUS,
        MINUS,
        MULTIPLY,
        DIVIDE,
        MODULO,
        ADD,      // Alias for PLUS
        SUBTRACT, // Alias for MINUS
        
        // Comparison
        EQUALS,
        NOT_EQUALS,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        
        // Logical
        AND,
        OR,
        
        // String
        CONCAT,
        
        // Membership
        IN
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitBinaryExpr(this);
    }
}
