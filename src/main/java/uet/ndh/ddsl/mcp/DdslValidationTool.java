package uet.ndh.ddsl.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.analysis.resolver.SymbolResolver;
import uet.ndh.ddsl.analysis.resolver.TypeResolver;
import uet.ndh.ddsl.analysis.scope.SymbolTable;
import uet.ndh.ddsl.analysis.validator.DddValidator;
import uet.ndh.ddsl.analysis.validator.Diagnostic;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>MCP Tool — validateDSL</b>
 * <p>
 * Exposed as a Spring AI {@code @Tool} and auto-registered with the MCP Server.
 * Performs full-pipeline validation:
 * <ol>
 *   <li><b>Lexing + Parsing</b> — catches syntax errors via the hand-written recursive descent parser</li>
 *   <li><b>Symbol Resolution</b> (Pass 1) — builds the symbol table, detects duplicate declarations</li>
 *   <li><b>Type Resolution</b> (Pass 2) — validates all type references resolve</li>
 *   <li><b>DDD Validation</b> (Pass 3) — enforces DDD invariants (aggregate has root, entity has identity, etc.)</li>
 * </ol>
 *
 * @see DdslParser
 * @see SymbolResolver
 * @see TypeResolver
 * @see DddValidator
 */
@Component
public class DdslValidationTool {

    private static final Logger log = LoggerFactory.getLogger(DdslValidationTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate DDSL source code and return structured diagnostics.
     *
     * @param code the DDSL source code to validate
     * @return JSON string with structure: {@code {"valid": bool, "errors": [...], "warnings": [...], "info": [...]}}
     */
    @Tool(description = """
            Validates DDSL (Domain-Driven Specific Language) source code.
            Performs lexing, parsing, symbol resolution, type checking, and DDD rule validation.
            Returns a JSON object with:
            - "valid": boolean indicating if the code is error-free
            - "errors": array of error messages (syntax errors, undefined types, DDD violations)
            - "warnings": array of warning messages (e.g. value object with no fields)
            - "info": array of informational messages
            - "parseErrors": array of parse-level errors with line/column information
            """)
    public String validateDSL(
            @ToolParam(description = "The DDSL source code to validate") String code
    ) {
        log.info("validateDSL: validating {} chars of DDSL code", code != null ? code.length() : 0);

        var result = new ValidationResult();

        if (code == null || code.isBlank()) {
            result.errors.add("Input code is empty");
            return toJson(result);
        }

        DomainModel model;

        // ── Phase 1: Parse ──────────────────────────────────────────────
        try {
            var parser = new DdslParser(code, "<input>");
            model = parser.parse();

            log.debug("validateDSL: parsed {} bounded contexts",
                    model.boundedContexts().size());

        } catch (ParseException e) {
            // ParseException carries the list of ParseError objects
            for (var parseError : e.getErrors()) {
                String msg = formatParseError(parseError);
                result.parseErrors.add(msg);
                result.errors.add(msg);
            }
            if (result.errors.isEmpty()) {
                result.errors.add("Parse error: " + e.getMessage());
            }
            return toJson(result);
        } catch (Exception e) {
            result.errors.add("Unexpected parse error: " + e.getMessage());
            return toJson(result);
        }

        // ── Phase 2: Symbol Resolution ──────────────────────────────────
        try {
            var symbolTable = new SymbolTable();
            var symbolResolver = new SymbolResolver(symbolTable);
            model.accept(symbolResolver);

            for (var error : symbolResolver.errors()) {
                String msg = formatResolutionError(error);
                result.errors.add(msg);
            }

            // ── Phase 3: Type Resolution ────────────────────────────────
            var typeResolver = new TypeResolver(symbolTable);
            model.accept(typeResolver);

            for (var error : typeResolver.errors()) {
                String msg = formatTypeResolutionError(error);
                result.errors.add(msg);
            }

            // ── Phase 4: DDD Validation ─────────────────────────────────
            var dddValidator = new DddValidator();
            model.accept(dddValidator);

            for (var diagnostic : dddValidator.diagnostics()) {
                classifyDiagnostic(diagnostic, result);
            }

        } catch (Exception e) {
            result.errors.add("Analysis error: " + e.getMessage());
            log.warn("validateDSL: analysis phase failed", e);
        }

        result.valid = result.errors.isEmpty();

        log.info("validateDSL: valid={}, errors={}, warnings={}", result.valid,
                result.errors.size(), result.warnings.size());

        return toJson(result);
    }

    // ── Internals ───────────────────────────────────────────────────────

    private void classifyDiagnostic(Diagnostic diagnostic, ValidationResult result) {
        String message = formatDiagnostic(diagnostic);
        switch (diagnostic.severity()) {
            case ERROR -> result.errors.add(message);
            case WARNING -> result.warnings.add(message);
            case INFO, HINT -> result.info.add(message);
        }
    }

    private String formatDiagnostic(Diagnostic diagnostic) {
        var sb = new StringBuilder();
        sb.append("[").append(diagnostic.ruleId()).append("] ");
        sb.append(diagnostic.message());
        if (diagnostic.location() != null) {
            sb.append(" (line ").append(diagnostic.location().startLine())
                    .append(", col ").append(diagnostic.location().startColumn()).append(")");
        }
        return sb.toString();
    }

    private String formatParseError(Object error) {
        // The parser may return different error types — handle gracefully
        if (error instanceof DdslParser.ParseError pe) {
            return String.format("Parse error at line %d, col %d: %s", pe.line(), pe.column(), pe.message());
        }
        if (error instanceof String s) return s;
        return error.toString();
    }

    private String formatResolutionError(SymbolResolver.ResolutionError error) {
        var sb = new StringBuilder("[RESOLVE] ");
        sb.append(error.message());
        if (error.location() != null) {
            sb.append(" (line ").append(error.location().startLine())
                    .append(", col ").append(error.location().startColumn()).append(")");
        }
        return sb.toString();
    }

    private String formatTypeResolutionError(TypeResolver.TypeResolutionError error) {
        var sb = new StringBuilder("[TYPE] ");
        sb.append(error.message());
        if (error.location() != null) {
            sb.append(" (line ").append(error.location().startLine())
                    .append(", col ").append(error.location().startColumn()).append(")");
        }
        return sb.toString();
    }

    private String toJson(ValidationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Failed to serialize validation result", e);
            return "{\"valid\":false,\"errors\":[\"Internal serialization error\"],\"warnings\":[],\"info\":[]}";
        }
    }

    /**
     * Internal DTO for the validation result.
     */
    static class ValidationResult {
        public boolean valid = false;
        public List<String> errors = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> info = new ArrayList<>();
        public List<String> parseErrors = new ArrayList<>();
    }
}
