package uet.ndh.ddsl.parser;

import uet.ndh.ddsl.core.SourceLocation;

/**
 * Exception thrown when YAML parsing fails.
 */
public class ParseException extends Exception {

    private final SourceLocation location;

    public ParseException(String message, SourceLocation location) {
        super(message + " at " + location);
        this.location = location;
    }

    public ParseException(String message, SourceLocation location, Throwable cause) {
        super(message + " at " + location, cause);
        this.location = location;
    }

    public SourceLocation getLocation() {
        return location;
    }
}
