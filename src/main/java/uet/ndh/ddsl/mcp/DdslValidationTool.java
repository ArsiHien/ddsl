package uet.ndh.ddsl.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>MCP Tool — validateDSL</b>
 * <p>
 * Exposed as a Spring AI {@code @Tool} and auto-registered with the MCP Server.
 * Performs <b>parser-only</b> validation — catches syntax errors via the
 * hand-written recursive descent parser.
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
            result.errors.add("Input code is empty");
            return toJson(result);
        }

        // ── Parse phase (syntax validation) ─────────────────────────────
        try {
            var parser = new DdslParser(code, "<input>");
            parser.parse();

            // Check for collected errors (parser may not throw but still record errors)
            if (parser.hasErrors()) {
                for (var error : parser.getErrors()) {
                    result.errors.add(formatParseError(error));
                }
            }
        } catch (ParseException e) {
            for (var parseError : e.getErrors()) {
                result.errors.add(formatParseError(parseError));
            }
            if (result.errors.isEmpty()) {
                result.errors.add("Parse error: " + e.getMessage());
            }
        } catch (Exception e) {
            result.errors.add("Unexpected parse error: " + e.getMessage());
        }

        result.valid = result.errors.isEmpty();

        log.info("validateDSL: valid={}, errors={}", result.valid, result.errors.size());

        return toJson(result);
    }

    // ── Internals ───────────────────────────────────────────────────────

    private String formatParseError(Object error) {
        if (error instanceof DdslParser.ParseError pe) {
            return String.format("Parse error at line %d, col %d: %s",
                    pe.line(), pe.column(), pe.message());
        }
        if (error instanceof String s) return s;
        return error.toString();
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
        public List<String> errors = new ArrayList<>();
    }
}
