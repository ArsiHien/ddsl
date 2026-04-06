package uet.ndh.ddsl.parser;

import lombok.Getter;
import uet.ndh.ddsl.ast.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when parsing fails.
 */
@Getter
public class ParseException extends Exception {

    private final SourceSpan location;
    private final List<DdslParser.ParseError> errors;

    public ParseException(String message, SourceSpan location) {
        super(message + " at " + location);
        this.location = location;
        this.errors = new ArrayList<>();
    }

    public ParseException(String message, SourceSpan location, Throwable cause) {
        super(message + " at " + location, cause);
        this.location = location;
        this.errors = new ArrayList<>();
    }
    
    public ParseException(String message, List<DdslParser.ParseError> errors) {
        super(message);
        this.location = errors.isEmpty() ? null : errors.get(0).location();
        this.errors = new ArrayList<>(errors);
    }
    
    public ParseException(String message) {
        super(message);
        this.location = null;
        this.errors = new ArrayList<>();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (!errors.isEmpty()) {
            sb.append("\n\nErrors:\n");
            for (DdslParser.ParseError error : errors) {
                sb.append("  - ").append(error.toString()).append("\n");
            }
        }
        return sb.toString();
    }
}
