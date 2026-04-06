package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Represents a validation diagnostic - either an error or warning.
 */
public record Diagnostic(
    SourceSpan location,
    String message,
    Severity severity,
    String ruleId
) {
    
    public enum Severity {
        ERROR,
        WARNING,
        INFO,
        HINT
    }
    
    public static Diagnostic error(SourceSpan location, String message, String ruleId) {
        return new Diagnostic(location, message, Severity.ERROR, ruleId);
    }
    
    public static Diagnostic warning(SourceSpan location, String message, String ruleId) {
        return new Diagnostic(location, message, Severity.WARNING, ruleId);
    }
    
    public static Diagnostic info(SourceSpan location, String message, String ruleId) {
        return new Diagnostic(location, message, Severity.INFO, ruleId);
    }
    
    public boolean isError() {
        return severity == Severity.ERROR;
    }
    
    public boolean isWarning() {
        return severity == Severity.WARNING;
    }
}
