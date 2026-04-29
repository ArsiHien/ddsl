package uet.ndh.ddsl.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.DdslState;
import uet.ndh.ddsl.mcp.DdslValidationError;
import uet.ndh.ddsl.mcp.DdslValidationTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * <b>Judge Agent</b> — stateless validation gate.
 * <p>
 * Validates DDSL syntax using the parser. Stateless - no retries.
 * <p>
 * Returns validation result with error logs for synthesizer retry loop.
 */
@Component
public class JudgeNode implements NodeAction<DdslState> {

    private static final Logger log = LoggerFactory.getLogger(JudgeNode.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DdslValidationTool validationTool;

    public JudgeNode(DdslValidationTool validationTool) {
        this.validationTool = validationTool;
    }

    @Override
    public Map<String, Object> apply(DdslState state) {
        log.info("JudgeNode: validating DSL");

        String dsl = state.currentDsl();
        Map<String, Object> updates = new HashMap<>();

        if (dsl == null || dsl.isBlank()) {
            updates.put("errorLogs", List.of("DSL draft is empty"));
            updates.put("isSuccessful", false);
            updates.put("errorStage", "judge");
            updates.put("compilerFeedback", "Error: DSL draft is empty. Please provide valid DDSL code.");
            return updates;
        }

        try {
            String jsonResult = validationTool.validateDSL(dsl);
            ValidationResult parsed = parseStructuredResult(jsonResult);

            boolean valid = parsed.valid;
            List<DdslValidationError> errors = parsed.errors;

            if (valid) {
                log.info("JudgeNode: DSL is valid ✓");
                updates.put("isSuccessful", true);
                updates.put("finalDsl", dsl);
                updates.put("errorLogs", List.of());
                updates.put("errorStage", "");
                updates.put("compilerFeedback", "");
            } else {
                log.info("JudgeNode: {} error(s) found", errors.size());

                List<String> errorLogs = errors.stream()
                        .map(this::formatErrorForLogs)
                        .collect(Collectors.toList());

                String compilerFeedback = formatCompilerFeedback(errors);

                updates.put("isSuccessful", false);
                updates.put("errorLogs", errorLogs);
                updates.put("errorStage", "judge");
                updates.put("compilerFeedback", compilerFeedback);
            }

            return updates;

        } catch (Exception e) {
            log.error("JudgeNode: validation failed", e);
            updates.put("isSuccessful", false);
            updates.put("errorLogs", List.of("Validation error: " + e.getMessage()));
            updates.put("errorStage", "judge");
            updates.put("compilerFeedback", "Validation failed with exception: " + e.getMessage());
            return updates;
        }
    }

    private ValidationResult parseStructuredResult(String json) {
        try {
            return mapper.readValue(json, ValidationResult.class);
        } catch (Exception e) {
            log.warn("JudgeNode: failed to parse validation JSON", e);
            ValidationResult result = new ValidationResult();
            result.valid = false;
            result.errors = List.of(
                    new DdslValidationError(
                            uet.ndh.ddsl.mcp.ErrorCategory.SYNTAX_ERROR,
                            "unknown",
                            "Failed to parse validation output: " + e.getMessage(),
                            "Check the validation tool output format"
                    )
            );
            return result;
        }
    }

    private String formatErrorForLogs(DdslValidationError error) {
        return String.format("[%s] %s: %s (%s)",
                error.errorCategory(),
                error.location(),
                error.message(),
                error.suggestion());
    }

    private String formatCompilerFeedback(List<DdslValidationError> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("DDSL Compilation Errors (").append(errors.size()).append(" found):\n\n");

        for (int i = 0; i < errors.size(); i++) {
            DdslValidationError error = errors.get(i);
            sb.append("Error ").append(i + 1).append("/").append(errors.size()).append("\n");
            sb.append("  Category: ").append(error.errorCategory()).append("\n");
            sb.append("  Location: ").append(error.location()).append("\n");
            sb.append("  Message: ").append(error.message()).append("\n");
            if (error.suggestion() != null && !error.suggestion().isBlank()) {
                sb.append("  Suggestion: ").append(error.suggestion()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Please fix these errors and regenerate the DDSL code.");
        return sb.toString();
    }

    static class ValidationResult {
        public boolean valid = false;
        public List<DdslValidationError> errors = new ArrayList<>();
    }
}
