package uet.ndh.ddsl.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.analysis.validator.BehaviorSemanticValidator;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Import new error model
/**
 * <b>MCP Tool — validateDSL</b>
 * <p>
 * Exposed as a Spring AI {@code @Tool} and auto-registered with the MCP Server.
 * Performs syntax validation and lightweight semantic validation
 * (behavior-scoped identifier checks).
 *
 * @see DdslParser
 */
@Component
@Slf4j
public class DdslValidationTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate DDSL source code using the DDSL parser.
     *
     * @param code the DDSL source code to validate
     * @return JSON string: {@code {"valid": bool, "errors": [...]}}
     */
    @Tool(description = """
            Validates DDSL (Domain-Driven Specific Language) source code by parsing it.
            Returns a JSON object with:
            - "valid": boolean indicating if the code parses without errors
            - "errors": array of parse error messages with line/column information
            """)
    public String validateDSL(
            @ToolParam(description = "The DDSL source code to validate") String code
    ) {
        log.info("validateDSL: parsing {} chars of DDSL code", code != null ? code.length() : 0);

        var result = new ValidationResult();

        if (code == null || code.isBlank()) {
            result.errors.add(new DdslValidationError(ErrorCategory.SYNTAX_ERROR, "unknown", "Input code is empty", "Provide non-empty DDSL code"));
            return toJson(result);
        }

        // ── Parse phase (syntax validation) ─────────────────────────────
        try {
            var parser = new DdslParser(code, "<input>");
            var model = parser.parse();

            // Check for collected errors (parser may not throw but still record errors)
            if (parser.hasErrors()) {
                for (var error : parser.getErrors()) {
                    if (error instanceof DdslParser.ParseError pe) {
                        result.errors.add(makeSyntaxError(pe));
                    } else {
                        // Fallback generic error string
                        result.errors.add(new DdslValidationError(ErrorCategory.SYNTAX_ERROR, "unknown", error.toString(), null));
                    }
                }
            } else {
                // Semantic pass: undefined identifiers in behavior scopes.
                var behaviorSemanticValidator = new BehaviorSemanticValidator();
                model.accept(behaviorSemanticValidator);
                behaviorSemanticValidator.errors().forEach(d ->
                        result.errors.add(makeSemanticError(d))
                );
            }
        } catch (ParseException e) {
            for (Object parseError : e.getErrors()) {
                if (parseError instanceof DdslParser.ParseError pe) {
                    result.errors.add(makeSyntaxError(pe));
                } else {
                    result.errors.add(new DdslValidationError(ErrorCategory.SYNTAX_ERROR, "unknown", parseError.toString(), null));
                }
            }
            if (result.errors.isEmpty()) {
                result.errors.add(new DdslValidationError(ErrorCategory.SYNTAX_ERROR, "unknown", "Parse error: " + e.getMessage(), null));
            }
        } catch (Exception e) {
            result.errors.add(new DdslValidationError(ErrorCategory.SYNTAX_ERROR, "unknown", "Unexpected parse error: " + e.getMessage(), null));
        }

        result.valid = result.errors.isEmpty();

        log.info("validateDSL: valid={}, errors={}", result.valid, result.errors.size());

        return toJson(result);
    }

    // ── Internals ───────────────────────────────────────────────────────

    private DdslValidationError makeSyntaxError(DdslParser.ParseError pe) {
        String location = (pe.line() > 0 && pe.column() > 0) ? String.format("%d:%d", pe.line(), pe.column()) : "unknown";
        String message = pe.message();
        String suggestion = generateSyntaxSuggestion(pe);
        return new DdslValidationError(ErrorCategory.SYNTAX_ERROR, location, message, suggestion);
    }

    private String generateSyntaxSuggestion(DdslParser.ParseError pe) {
        // Basic heuristic: point at location; add generic tip
        if (pe == null) return null;
        String token = null;
        String m = pe.message();
        // Try to extract a token from quotes if present in message
        int idx = m.indexOf('\'');
        if (idx != -1) {
            int end = m.indexOf('\'', idx + 1);
            if (end > idx) token = m.substring(idx + 1, end);
        }
        if (token != null) {
            return "Check surrounding token '" + token + "' for correctness.";
        }
        return "Review syntax around the reported location.";
    }

    private DdslValidationError makeSemanticError(uet.ndh.ddsl.analysis.validator.Diagnostic diagnostic) {
        if (diagnostic == null) {
            return new DdslValidationError(ErrorCategory.SEMANTIC_ERROR, "unknown", "Semantic error", null);
        }
        int line = diagnostic.location() != null ? diagnostic.location().startLine() : 0;
        int col = diagnostic.location() != null ? diagnostic.location().startColumn() : 0;
        String location = (line > 0 && col > 0) ? String.format("%d:%d", line, col) : "unknown";
        String ruleId = diagnostic.ruleId() != null ? diagnostic.ruleId() : "SEM";
        String message = diagnostic.message();
        String suggestion = "Review semantic rule " + ruleId + ".";
        return new DdslValidationError(ErrorCategory.SEMANTIC_ERROR, location, message, suggestion);
    }

    private String formatSemanticError(uet.ndh.ddsl.analysis.validator.Diagnostic diagnostic) {
        if (diagnostic == null) {
            return "Semantic error";
        }
        int line = diagnostic.location() != null ? diagnostic.location().startLine() : 0;
        int col = diagnostic.location() != null ? diagnostic.location().startColumn() : 0;
        String ruleId = diagnostic.ruleId() != null ? diagnostic.ruleId() : "SEM";
        return String.format("Semantic error [%s] at line %d, col %d: %s",
                ruleId, line, col, diagnostic.message());
    }

    private String toJson(ValidationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Failed to serialize validation result", e);
            return "{\"valid\":false,\"errors\":[\"Internal serialization error\"]}";
        }
    }

    /**
     * Internal DTO for the validation result.
     */
    static class ValidationResult {
        public boolean valid = false;
        public List<DdslValidationError> errors = new ArrayList<>();
    }
}
