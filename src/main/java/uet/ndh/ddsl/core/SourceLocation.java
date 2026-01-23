package uet.ndh.ddsl.core;

/**
 * Represents the location in source code where an AST node was defined.
 * Used for error reporting and debugging.
 */
public class SourceLocation {
    private final int line;
    private final int column;
    private final String file;

    public SourceLocation(int line, int column, String file) {
        this.line = line;
        this.column = column;
        this.file = file;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getFile() {
        return file;
    }

    @Override
    public String toString() {
        return String.format("%s:%d:%d", file, line, column);
    }
}
