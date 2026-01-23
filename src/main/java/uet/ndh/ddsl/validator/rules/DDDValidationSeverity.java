package uet.ndh.ddsl.validator.rules;

/**
 * Severity levels for DDD validation errors.
 */
public enum DDDValidationSeverity {
    ERROR,      // Must be fixed - prevents code generation
    WARNING,    // Should be fixed - indicates potential problems
    INFO        // Nice to have - suggestions for improvement
}
