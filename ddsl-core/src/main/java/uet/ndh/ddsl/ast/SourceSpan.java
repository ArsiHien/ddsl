package uet.ndh.ddsl.ast;

/**
 * Represents a span of source code for error reporting.
 * Immutable record containing line/column information.
 */
public record SourceSpan(
    String fileName,
    int startLine,
    int startColumn,
    int endLine,
    int endColumn
) {
    /**
     * Create a span for a single position (start = end).
     */
    public static SourceSpan at(String fileName, int line, int column) {
        return new SourceSpan(fileName, line, column, line, column);
    }
    
    /**
     * Create a span from start to end positions.
     */
    public static SourceSpan between(SourceSpan start, SourceSpan end) {
        return new SourceSpan(
            start.fileName,
            start.startLine,
            start.startColumn,
            end.endLine,
            end.endColumn
        );
    }
    
    /**
     * Unknown/synthetic location.
     */
    public static SourceSpan unknown() {
        return new SourceSpan("<unknown>", 0, 0, 0, 0);
    }
    
    @Override
    public String toString() {
        if (startLine == endLine) {
            return String.format("%s:%d:%d", fileName, startLine, startColumn);
        }
        return String.format("%s:%d:%d-%d:%d", fileName, startLine, startColumn, endLine, endColumn);
    }
}
