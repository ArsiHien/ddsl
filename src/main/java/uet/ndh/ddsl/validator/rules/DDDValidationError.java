package uet.ndh.ddsl.validator.rules;

import lombok.AllArgsConstructor;
import lombok.Data;
import uet.ndh.ddsl.core.SourceLocation;

/**
 * Represents a DDD validation rule violation.
 */
@Data
@AllArgsConstructor
public class DDDValidationError {
    private final String ruleCode;
    private final String ruleName;
    private final String description;
    private final String message;
    private final SourceLocation location;
    private final DDDValidationSeverity severity;

    public DDDValidationError(String ruleCode, String ruleName, String message, SourceLocation location) {
        this(ruleCode, ruleName, "", message, location, DDDValidationSeverity.ERROR);
    }
}
