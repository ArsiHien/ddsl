package uet.ndh.ddsl.validator;

import lombok.Data;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Summary of DDD validation results.
 */
@Data
public class DDDValidationSummary {
    private final List<DDDValidationError> errors;
    private final long errorCount;
    private final long warningCount;
    private final long infoCount;
    private final Map<String, Long> errorsByRule;
    private final boolean hasErrors;

    public DDDValidationSummary(List<DDDValidationError> errors) {
        this.errors = errors;
        this.errorCount = errors.stream()
            .mapToLong(e -> e.getSeverity() == DDDValidationSeverity.ERROR ? 1 : 0)
            .sum();
        this.warningCount = errors.stream()
            .mapToLong(e -> e.getSeverity() == DDDValidationSeverity.WARNING ? 1 : 0)
            .sum();
        this.infoCount = errors.stream()
            .mapToLong(e -> e.getSeverity() == DDDValidationSeverity.INFO ? 1 : 0)
            .sum();
        this.errorsByRule = errors.stream()
            .collect(Collectors.groupingBy(DDDValidationError::getRuleCode, Collectors.counting()));
        this.hasErrors = errorCount > 0;
    }

    public void printSummary() {
        System.out.println("=== DDD Validation Summary ===");
        System.out.println("Total Issues: " + errors.size());
        System.out.println("Errors: " + errorCount);
        System.out.println("Warnings: " + warningCount);
        System.out.println("Info: " + infoCount);

        if (!errorsByRule.isEmpty()) {
            System.out.println("\n--- Issues by Rule ---");
            errorsByRule.forEach((rule, count) ->
                System.out.println(rule + ": " + count));
        }

        if (!errors.isEmpty()) {
            System.out.println("\n--- Detailed Issues ---");
            errors.forEach(error ->
                System.out.printf("[%s] %s: %s at %s%n",
                    error.getSeverity(),
                    error.getRuleCode(),
                    error.getMessage(),
                    error.getLocation()));
        }
    }
}
