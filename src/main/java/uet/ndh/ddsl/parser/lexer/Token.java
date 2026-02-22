package uet.ndh.ddsl.parser.lexer;

import java.util.Objects;

/**
 * Represents a single token in the DDSL language.
 * 
 * A token is the smallest unit of meaning in the source code, produced by
 * the lexer/scanner. Each token contains:
 * - The type of token (keyword, identifier, literal, etc.)
 * - The lexeme (the actual text from the source)
 * - Any literal value (for numeric and string literals)
 * - Source location information (line and column)
 * 
 * Following "Crafting Interpreters" Chapter 4 design patterns.
 */
public class Token {
    
    /** The type of this token */
    private final TokenType type;
    
    /** The actual text from the source code */
    private final String lexeme;
    
    /** The literal value (for literals), or null */
    private final Object literal;
    
    /** The line number where this token appears (1-based) */
    private final int line;
    
    /** The column number where this token starts (1-based) */
    private final int column;
    
    /** The offset from the start of the source (0-based) */
    private final int offset;
    
    /**
     * Creates a new token with all fields.
     * 
     * @param type    the token type
     * @param lexeme  the source text
     * @param literal the literal value (may be null)
     * @param line    the line number (1-based)
     * @param column  the column number (1-based)
     * @param offset  the character offset (0-based)
     */
    public Token(TokenType type, String lexeme, Object literal, int line, int column, int offset) {
        this.type = Objects.requireNonNull(type, "Token type cannot be null");
        this.lexeme = Objects.requireNonNull(lexeme, "Lexeme cannot be null");
        this.literal = literal;
        this.line = line;
        this.column = column;
        this.offset = offset;
    }
    
    /**
     * Creates a new token without a literal value.
     * 
     * @param type   the token type
     * @param lexeme the source text
     * @param line   the line number (1-based)
     * @param column the column number (1-based)
     * @param offset the character offset (0-based)
     */
    public Token(TokenType type, String lexeme, int line, int column, int offset) {
        this(type, lexeme, null, line, column, offset);
    }
    
    /**
     * Creates a simple token for testing purposes.
     * 
     * @param type   the token type
     * @param lexeme the source text
     */
    public Token(TokenType type, String lexeme) {
        this(type, lexeme, null, 1, 1, 0);
    }
    
    // ========== Getters ==========
    
    public TokenType getType() {
        return type;
    }
    
    public String getLexeme() {
        return lexeme;
    }
    
    public Object getLiteral() {
        return literal;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public int getOffset() {
        return offset;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Check if this token is of a specific type.
     */
    public boolean is(TokenType type) {
        return this.type == type;
    }
    
    /**
     * Check if this token is one of the specified types.
     */
    public boolean isOneOf(TokenType... types) {
        for (TokenType t : types) {
            if (this.type == t) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if this token is a keyword.
     */
    public boolean isKeyword() {
        return type.isKeyword();
    }
    
    /**
     * Check if this token is a literal.
     */
    public boolean isLiteral() {
        return type.isLiteral();
    }
    
    /**
     * Check if this token is the end of file.
     */
    public boolean isEof() {
        return type == TokenType.EOF;
    }
    
    /**
     * Check if this token is an error token.
     */
    public boolean isError() {
        return type == TokenType.ERROR;
    }
    
    /**
     * Get the string value of the literal (for string literals).
     */
    public String getStringValue() {
        if (literal instanceof String) {
            return (String) literal;
        }
        return lexeme;
    }
    
    /**
     * Get the integer value of the literal (for integer literals).
     */
    public int getIntValue() {
        if (literal instanceof Number) {
            return ((Number) literal).intValue();
        }
        throw new IllegalStateException("Token is not a number: " + this);
    }
    
    /**
     * Get the decimal value of the literal (for decimal literals).
     */
    public double getDoubleValue() {
        if (literal instanceof Number) {
            return ((Number) literal).doubleValue();
        }
        throw new IllegalStateException("Token is not a number: " + this);
    }
    
    /**
     * Get the boolean value of the literal (for boolean literals).
     */
    public boolean getBooleanValue() {
        if (type == TokenType.TRUE) return true;
        if (type == TokenType.FALSE) return false;
        throw new IllegalStateException("Token is not a boolean: " + this);
    }
    
    // ========== Object Methods ==========
    
    @Override
    public String toString() {
        if (literal != null) {
            return String.format("%s '%s' (%s) at %d:%d", 
                type, lexeme, literal, line, column);
        }
        return String.format("%s '%s' at %d:%d", 
            type, lexeme, line, column);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Token token = (Token) obj;
        return type == token.type && 
               Objects.equals(lexeme, token.lexeme) &&
               Objects.equals(literal, token.literal);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, lexeme, literal);
    }
    
    // ========== Factory Methods ==========
    
    /**
     * Create an EOF token.
     */
    public static Token eof(int line, int column, int offset) {
        return new Token(TokenType.EOF, "", null, line, column, offset);
    }
    
    /**
     * Create an error token with a message.
     */
    public static Token error(String message, int line, int column, int offset) {
        return new Token(TokenType.ERROR, message, null, line, column, offset);
    }
    
    /**
     * Create an identifier token.
     */
    public static Token identifier(String name, int line, int column, int offset) {
        return new Token(TokenType.IDENTIFIER, name, null, line, column, offset);
    }
    
    /**
     * Create a string literal token.
     */
    public static Token string(String value, int line, int column, int offset) {
        // Lexeme includes quotes, literal is the unquoted value
        return new Token(TokenType.STRING_LITERAL, "\"" + value + "\"", value, line, column, offset);
    }
    
    /**
     * Create an integer literal token.
     */
    public static Token integer(String lexeme, long value, int line, int column, int offset) {
        return new Token(TokenType.INTEGER_LITERAL, lexeme, value, line, column, offset);
    }
    
    /**
     * Create a decimal literal token.
     */
    public static Token decimal(String lexeme, double value, int line, int column, int offset) {
        return new Token(TokenType.DECIMAL_LITERAL, lexeme, value, line, column, offset);
    }
}
