package uet.ndh.ddsl.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.DdslState;
import uet.ndh.ddsl.agent.dto.JudgeErrorReport;
import uet.ndh.ddsl.agent.dto.JudgeArtifact;
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
    private static final int MAX_REPAIR_ATTEMPTS_PER_CHUNK = 3;

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
            updates.put("structuredErrors", List.of(emptyDslError(state)));
            return updates;
        }

        try {
            String jsonResult = validationTool.validateDSL(dsl);
            ValidationResult parsed = parseStructuredResult(jsonResult);

            boolean valid = parsed.valid;
            List<DdslValidationError> errors = parsed.errors;
            updates.put(DdslState.KEY_JUDGE_ARTIFACTS,
                    appendArtifact(state, valid, jsonResult, valid ? "verdict: ACCEPT" : formatCompilerFeedback(errors)));

            if (valid) {
                log.info("JudgeNode: DSL is valid ✓");
                updates.put("isSuccessful", true);
                updates.put("finalDsl", dsl);
                updates.put("errorLogs", List.of());
                updates.put("errorStage", "");
                updates.put("compilerFeedback", "verdict: ACCEPT");
                updates.put("structuredErrors", List.of());
            } else {
                log.info("JudgeNode: {} error(s) found", errors.size());

                List<String> errorLogs = errors.stream()
                        .map(this::formatErrorForLogs)
                        .collect(Collectors.toList());

                String compilerFeedback = formatCompilerFeedback(errors);
                List<Map<String, Object>> structuredErrors = errors.stream()
                        .map(error -> toStructuredError(error, state))
                        .collect(Collectors.toList());

                updates.put("isSuccessful", false);
                updates.put("errorLogs", errorLogs);
                updates.put("errorStage", "judge");
                updates.put("compilerFeedback", compilerFeedback);
                updates.put("structuredErrors", structuredErrors);
            }

            return updates;

        } catch (Exception e) {
            log.error("JudgeNode: validation failed", e);
            updates.put("isSuccessful", false);
            updates.put("errorLogs", List.of("Validation error: " + e.getMessage()));
            updates.put("errorStage", "judge");
            updates.put("compilerFeedback", "verdict: REJECT\nValidation failed with exception: " + e.getMessage());
            updates.put("structuredErrors", List.of(exceptionError(e, state)));
            return updates;
        }
    }

    private Map<String, Object> toStructuredError(DdslValidationError error, DdslState state) {
        int lineNumber = extractLineNumber(error.location());
        String sourceChunk = inferSourceChunk(lineNumber, state);
        String errorType = classifyError(error);
        boolean retryAllowed = retryAllowed(sourceChunk, state);
        String verdict = retryAllowed ? "REJECT" : "ESCALATE";

        return new JudgeErrorReport(
                verdict,
                errorType,
                error.location(),
                lineNumber,
                error.message(),
                fixHint(error, errorType),
                retryAllowed,
                sourceChunk,
                retryAllowed ? "" : state.currentDsl()
        ).toMap();
    }

    private Map<String, Object> emptyDslError(DdslState state) {
        boolean retryAllowed = retryAllowed(state.currentChunkId(), state);
        return new JudgeErrorReport(
                retryAllowed ? "REJECT" : "ESCALATE",
                "SYNTAX_ERROR",
                "unknown",
                0,
                "DSL draft is empty",
                "Return non-empty DDSL code for the current chunk.",
                retryAllowed,
                state.currentChunkId(),
                retryAllowed ? "" : state.currentDsl()
        ).toMap();
    }

    private Map<String, Object> exceptionError(Exception exception, DdslState state) {
        boolean retryAllowed = retryAllowed(state.currentChunkId(), state);
        return new JudgeErrorReport(
                retryAllowed ? "REJECT" : "ESCALATE",
                "SYNTAX_ERROR",
                "unknown",
                0,
                exception.getMessage(),
                "Inspect validator exception and regenerate the affected chunk.",
                retryAllowed,
                state.currentChunkId(),
                retryAllowed ? "" : state.currentDsl()
        ).toMap();
    }

    private String classifyError(DdslValidationError error) {
        String message = error.message() != null ? error.message().toLowerCase() : "";
        if (message.contains("unknown") || message.contains("unexpected construct")
                || message.contains("unknown construct") || message.contains("hallucinated")) {
            return "UNKNOWN_CONSTRUCT";
        }
        if (error.errorCategory() == uet.ndh.ddsl.mcp.ErrorCategory.SYNTAX_ERROR
                || error.errorCategory() == uet.ndh.ddsl.mcp.ErrorCategory.MISSING_CLAUSE
                || error.errorCategory() == uet.ndh.ddsl.mcp.ErrorCategory.INVALID_CONSTRUCT) {
            return "SYNTAX_ERROR";
        }
        if (message.contains("constraint") || message.contains("invariant")
                || message.contains("business rule")) {
            return "CONSTRAINT_VIOLATION";
        }
        if (error.errorCategory() == uet.ndh.ddsl.mcp.ErrorCategory.SEMANTIC_ERROR) {
            return "SEMANTIC_ERROR";
        }
        return "SYNTAX_ERROR";
    }

    private String fixHint(DdslValidationError error, String errorType) {
        if (error.suggestion() != null && !error.suggestion().isBlank()) {
            return error.suggestion();
        }
        return switch (errorType) {
            case "UNKNOWN_CONSTRUCT" -> "Remove hallucinated DSL keywords and use only supported DDSL declarations.";
            case "SEMANTIC_ERROR" -> "Keep syntax unchanged where possible and fix symbol/type references using the provided context.";
            case "CONSTRAINT_VIOLATION" -> "Adjust the invariant or business rule so it matches valid DDSL constraint syntax.";
            default -> "Fix malformed DDSL structure near the reported location.";
        };
    }

    private boolean retryAllowed(String sourceChunk, DdslState state) {
        long attempts = state.repairHistory().stream()
                .filter(entry -> sourceChunk != null && sourceChunk.equals(entry.get("chunkId")))
                .count();
        return attempts < MAX_REPAIR_ATTEMPTS_PER_CHUNK;
    }

    private int extractLineNumber(String location) {
        if (location == null || location.isBlank() || "unknown".equalsIgnoreCase(location)) {
            return 0;
        }
        String[] parts = location.split(":");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String inferSourceChunk(int lineNumber, DdslState state) {
        if (lineNumber <= 0) {
            return state.currentChunkId();
        }
        for (Map.Entry<String, Map<String, Integer>> entry : state.chunkLineMap().entrySet()) {
            int start = entry.getValue().getOrDefault("start", -1);
            int end = entry.getValue().getOrDefault("end", -1);
            if (lineNumber >= start && lineNumber <= end) {
                return entry.getKey();
            }
        }
        return state.currentChunkId();
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
        sb.append("verdict: REJECT\n");
        sb.append("DDSL Compilation Errors (").append(errors.size()).append(" found):\n\n");

        for (int i = 0; i < errors.size(); i++) {
            DdslValidationError error = errors.get(i);
            sb.append("Error ").append(i + 1).append("/").append(errors.size()).append("\n");
            sb.append("  Category: ").append(error.errorCategory()).append("\n");
            sb.append("  Location: ").append(error.location()).append("\n");
            sb.append("  Message: ").append(error.message()).append("\n");
            if (error.suggestion() != null && !error.suggestion().isBlank()) {
                sb.append("  Fix hint: ").append(error.suggestion()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Please fix these errors and regenerate the DDSL code.");
        return sb.toString();
    }

    private List<Map<String, Object>> appendArtifact(
            DdslState state,
            boolean valid,
            String compilerOutput,
            String compilerFeedback
    ) {
        List<Map<String, Object>> artifacts = new ArrayList<>(state.judgeArtifacts());
        artifacts.add(new JudgeArtifact(
                state.currentChunkId(),
                state.repairHistory().size(),
                valid,
                state.currentDsl(),
                compilerOutput,
                compilerFeedback
        ).toMap());
        return artifacts;
    }

    static class ValidationResult {
        public boolean valid = false;
        public List<DdslValidationError> errors = new ArrayList<>();
    }
}
