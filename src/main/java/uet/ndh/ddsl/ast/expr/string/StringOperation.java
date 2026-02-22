package uet.ndh.ddsl.ast.expr.string;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * Represents a string operation expression.
 * 
 * Syntax:
 * <pre>
 *     // Transformations
 *     stringExpr converted to uppercase
 *     stringExpr converted to lowercase
 *     stringExpr trimmed
 *     stringExpr truncated to N characters
 *     stringExpr concatenated with stringExpr
 *     stringExpr without "text"
 *     first N characters of stringExpr
 *     last N characters of stringExpr
 *     stringExpr replaced "old" with "new"
 * </pre>
 * 
 * Examples:
 * <pre>
 *     name converted to uppercase
 *     description truncated to 200 characters
 *     first 100 characters of description
 *     email replaced " " with ""
 * </pre>
 */
public record StringOperation(
    SourceSpan span,
    Expr stringExpr,
    StringOperationType type,
    int lengthValue,        // For truncated to, first N, last N
    String literal,         // For without, replaced
    String replacement,     // For replaced "old" with "new"
    Expr otherStringExpr    // For concatenated with
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return null;
    }
    
    // Factory methods for transformations
    
    public static StringOperation toUppercase(SourceSpan span, Expr expr) {
        return new StringOperation(span, expr, StringOperationType.TO_UPPERCASE, 0, null, null, null);
    }
    
    public static StringOperation toLowercase(SourceSpan span, Expr expr) {
        return new StringOperation(span, expr, StringOperationType.TO_LOWERCASE, 0, null, null, null);
    }
    
    public static StringOperation trimmed(SourceSpan span, Expr expr) {
        return new StringOperation(span, expr, StringOperationType.TRIMMED, 0, null, null, null);
    }
    
    public static StringOperation truncatedTo(SourceSpan span, Expr expr, int length) {
        return new StringOperation(span, expr, StringOperationType.TRUNCATED_TO, length, null, null, null);
    }
    
    public static StringOperation concatenatedWith(SourceSpan span, Expr expr, Expr other) {
        return new StringOperation(span, expr, StringOperationType.CONCATENATED_WITH, 0, null, null, other);
    }
    
    /**
     * Overload for concatenating with a string literal.
     */
    public static StringOperation concatenatedWith(SourceSpan span, Expr expr, String literal) {
        return new StringOperation(span, expr, StringOperationType.CONCATENATED_WITH, 0, literal, null, null);
    }
    
    public static StringOperation without(SourceSpan span, Expr expr, String text) {
        return new StringOperation(span, expr, StringOperationType.WITHOUT, 0, text, null, null);
    }
    
    public static StringOperation firstNChars(SourceSpan span, int n, Expr expr) {
        return new StringOperation(span, expr, StringOperationType.FIRST_N_CHARACTERS, n, null, null, null);
    }
    
    /**
     * Alias for firstNChars with different parameter order.
     */
    public static StringOperation firstNCharacters(SourceSpan span, Expr expr, int n) {
        return new StringOperation(span, expr, StringOperationType.FIRST_N_CHARACTERS, n, null, null, null);
    }
    
    public static StringOperation lastNChars(SourceSpan span, int n, Expr expr) {
        return new StringOperation(span, expr, StringOperationType.LAST_N_CHARACTERS, n, null, null, null);
    }
    
    /**
     * Alias for lastNChars with different parameter order.
     */
    public static StringOperation lastNCharacters(SourceSpan span, Expr expr, int n) {
        return new StringOperation(span, expr, StringOperationType.LAST_N_CHARACTERS, n, null, null, null);
    }
    
    public static StringOperation replaced(SourceSpan span, Expr expr, String old, String replacement) {
        return new StringOperation(span, expr, StringOperationType.REPLACED, 0, old, replacement, null);
    }
    
    /**
     * Type of string operation.
     */
    public enum StringOperationType {
        TO_UPPERCASE,
        TO_LOWERCASE,
        TRIMMED,
        TRUNCATED_TO,
        CONCATENATED_WITH,
        WITHOUT,
        FIRST_N_CHARACTERS,
        LAST_N_CHARACTERS,
        REPLACED
    }
}
