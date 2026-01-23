package uet.ndh.ddsl.core;

/**
 * Represents a validation error found during AST validation.
 */
public record ValidationError(String message, SourceLocation location, Severity severity) {
    public enum Severity {
        ERROR, WARNING, INFO
    }

    public ValidationError(String message, SourceLocation location) {
        this(message, location, Severity.ERROR);
    }

    // Explicit getters for better IDE support
    public String getMessage() {
        return message;
    }

    public SourceLocation getLocation() {
        return location;
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return String.format("%s: %s at %s", severity, message, location);
    }
}
