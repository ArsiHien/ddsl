package uet.ndh.ddsl.core.semantic;

import uet.ndh.ddsl.core.SourceLocation;

/**
 * Exception thrown during semantic analysis phase.
 * Used for symbol resolution errors, type mismatches, and semantic constraint violations.
 */
public class SemanticException extends RuntimeException {

    private final SourceLocation location;

    public SemanticException(String message, SourceLocation location) {
        super(message);
        this.location = location;
    }

    public SemanticException(String message, SourceLocation location, Throwable cause) {
        super(message, cause);
        this.location = location;
    }

    public SourceLocation getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("SemanticException at %s: %s", location, getMessage());
    }
}
