package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * Represents a string condition expression.
 * 
 * Syntax:
 * <pre>
 *     // Content checks
 *     stringExpr contains "text"
 *     stringExpr does not contain "text"
 *     stringExpr starts with "prefix"
 *     stringExpr ends with "suffix"
 *     stringExpr matches "pattern"
 *     stringExpr does not match "pattern"
 *     
 *     // Length checks
 *     stringExpr has length at least N
 *     stringExpr has length at most N
 *     stringExpr has length between A and B
 *     stringExpr is empty
 *     stringExpr is not empty
 *     stringExpr is blank
 *     stringExpr is not blank
 *     
 *     // Format checks
 *     stringExpr is valid email
 *     stringExpr is valid phone number
 *     stringExpr is valid URL
 * </pre>
 */
public record StringCondition(
    SourceSpan span,
    Expr stringExpr,
    StringConditionType type,
    String literal,         // For contains, starts with, matches, etc.
    int lengthValue,        // For has length at least/most
    int lengthMin,          // For has length between
    int lengthMax,          // For has length between
    FormatType formatType   // For is valid format
) implements Expr {
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return null;
    }
    
    // Factory methods for content checks
    
    public static StringCondition contains(SourceSpan span, Expr expr, String text) {
        return new StringCondition(span, expr, StringConditionType.CONTAINS, text, 0, 0, 0, null);
    }
    
    public static StringCondition doesNotContain(SourceSpan span, Expr expr, String text) {
        return new StringCondition(span, expr, StringConditionType.DOES_NOT_CONTAIN, text, 0, 0, 0, null);
    }
    
    public static StringCondition startsWith(SourceSpan span, Expr expr, String prefix) {
        return new StringCondition(span, expr, StringConditionType.STARTS_WITH, prefix, 0, 0, 0, null);
    }
    
    public static StringCondition endsWith(SourceSpan span, Expr expr, String suffix) {
        return new StringCondition(span, expr, StringConditionType.ENDS_WITH, suffix, 0, 0, 0, null);
    }
    
    public static StringCondition matches(SourceSpan span, Expr expr, String pattern) {
        return new StringCondition(span, expr, StringConditionType.MATCHES, pattern, 0, 0, 0, null);
    }
    
    public static StringCondition doesNotMatch(SourceSpan span, Expr expr, String pattern) {
        return new StringCondition(span, expr, StringConditionType.DOES_NOT_MATCH, pattern, 0, 0, 0, null);
    }
    
    // Factory methods for length checks
    
    public static StringCondition hasLengthAtLeast(SourceSpan span, Expr expr, int length) {
        return new StringCondition(span, expr, StringConditionType.HAS_LENGTH_AT_LEAST, null, length, 0, 0, null);
    }
    
    public static StringCondition hasLengthAtMost(SourceSpan span, Expr expr, int length) {
        return new StringCondition(span, expr, StringConditionType.HAS_LENGTH_AT_MOST, null, length, 0, 0, null);
    }
    
    public static StringCondition hasLengthGreaterThan(SourceSpan span, Expr expr, int length) {
        return new StringCondition(span, expr, StringConditionType.HAS_LENGTH_GREATER_THAN, null, length, 0, 0, null);
    }
    
    public static StringCondition hasLengthLessThan(SourceSpan span, Expr expr, int length) {
        return new StringCondition(span, expr, StringConditionType.HAS_LENGTH_LESS_THAN, null, length, 0, 0, null);
    }
    
    public static StringCondition hasLengthExactly(SourceSpan span, Expr expr, int length) {
        return new StringCondition(span, expr, StringConditionType.HAS_LENGTH_EXACTLY, null, length, 0, 0, null);
    }
    
    public static StringCondition hasLengthBetween(SourceSpan span, Expr expr, int min, int max) {
        return new StringCondition(span, expr, StringConditionType.HAS_LENGTH_BETWEEN, null, 0, min, max, null);
    }
    
    public static StringCondition isEmpty(SourceSpan span, Expr expr) {
        return new StringCondition(span, expr, StringConditionType.IS_EMPTY, null, 0, 0, 0, null);
    }
    
    public static StringCondition isNotEmpty(SourceSpan span, Expr expr) {
        return new StringCondition(span, expr, StringConditionType.IS_NOT_EMPTY, null, 0, 0, 0, null);
    }
    
    public static StringCondition isBlank(SourceSpan span, Expr expr) {
        return new StringCondition(span, expr, StringConditionType.IS_BLANK, null, 0, 0, 0, null);
    }
    
    public static StringCondition isNotBlank(SourceSpan span, Expr expr) {
        return new StringCondition(span, expr, StringConditionType.IS_NOT_BLANK, null, 0, 0, 0, null);
    }
    
    // Factory methods for format checks
    
    public static StringCondition isValidFormat(SourceSpan span, Expr expr, FormatType format) {
        return new StringCondition(span, expr, StringConditionType.IS_VALID_FORMAT, null, 0, 0, 0, format);
    }
    
    /**
     * Alias for isValidFormat - "has valid X format".
     */
    public static StringCondition hasValidFormat(SourceSpan span, Expr expr, FormatType format) {
        return isValidFormat(span, expr, format);
    }
    
    public static StringCondition isNotValidFormat(SourceSpan span, Expr expr, FormatType format) {
        return new StringCondition(span, expr, StringConditionType.IS_NOT_VALID_FORMAT, null, 0, 0, 0, format);
    }
    
    /**
     * Type of string condition.
     */
    public enum StringConditionType {
        // Content checks
        CONTAINS,
        DOES_NOT_CONTAIN,
        STARTS_WITH,
        ENDS_WITH,
        MATCHES,
        DOES_NOT_MATCH,
        
        // Length checks
        HAS_LENGTH_AT_LEAST,
        HAS_LENGTH_AT_MOST,
        HAS_LENGTH_GREATER_THAN,
        HAS_LENGTH_LESS_THAN,
        HAS_LENGTH_EXACTLY,
        HAS_LENGTH_BETWEEN,
        IS_EMPTY,
        IS_NOT_EMPTY,
        IS_BLANK,
        IS_NOT_BLANK,
        
        // Format checks
        IS_VALID_FORMAT,
        IS_NOT_VALID_FORMAT
    }
    
    /**
     * Format types for validation.
     */
    public enum FormatType {
        EMAIL,
        PHONE_NUMBER,
        URL,
        UUID,
        DATE,
        NUMERIC,
        ALPHANUMERIC
    }
}
