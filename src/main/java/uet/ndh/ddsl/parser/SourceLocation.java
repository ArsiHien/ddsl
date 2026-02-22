package uet.ndh.ddsl.parser;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Source location for parser error reporting.
 * This is a compatibility class that bridges the parser with the AST's SourceSpan.
 */
public record SourceLocation(
    String sourceName,
    int line,
    int column
) {
    /**
     * Convert to SourceSpan (single point location).
     */
    public SourceSpan toSourceSpan() {
        return new SourceSpan(sourceName, line, column, line, column);
    }
    
    /**
     * Create a SourceSpan that spans from this location to the end location.
     */
    public SourceSpan toSourceSpan(SourceLocation end) {
        return new SourceSpan(sourceName, line, column, end.line(), end.column());
    }
    
    @Override
    public String toString() {
        return sourceName + ":" + line + ":" + column;
    }
}
