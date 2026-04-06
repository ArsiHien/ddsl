package uet.ndh.ddsl.controller.dto;

import java.util.List;

/**
 * Response body for DDSL → Java compilation.
 *
 * @param success     whether compilation succeeded
 * @param artifacts   generated Java source files (empty on failure)
 * @param errors      parser or codegen error messages (empty on success)
 * @param diagnostics semantic analysis diagnostics with severity and location
 */
public record CompileResponse(
        boolean success,
        List<Artifact> artifacts,
        List<String> errors,
        List<DiagnosticMessage> diagnostics
) {
    /**
     * A single generated Java source file.
     *
     * @param fileName    e.g. "Order.java"
     * @param packageName e.g. "com.example.domain"
     * @param type        artifact type (AGGREGATE, ENTITY, VALUE_OBJECT, etc.)
     * @param sourceCode  the full Java source
     */
    public record Artifact(
            String fileName,
            String packageName,
            String type,
            String sourceCode
    ) {}

    /**
     * A single semantic analysis diagnostic (from symbol resolution,
     * type resolution, or DDD validation).
     *
     * @param message  human-readable description
     * @param severity ERROR | WARNING | INFO | HINT
     * @param line     1-based source line (0 if unknown)
     * @param column   1-based source column (0 if unknown)
     * @param ruleId   rule identifier, e.g. "DDD001" (null for resolver errors)
     */
    public record DiagnosticMessage(
            String message,
            String severity,
            int line,
            int column,
            String ruleId
    ) {}

    /** Successful compilation with no warnings. */
    public static CompileResponse success(List<Artifact> artifacts) {
        return new CompileResponse(true, artifacts, List.of(), List.of());
    }

    /** Successful compilation that produced analysis warnings. */
    public static CompileResponse successWithWarnings(
            List<Artifact> artifacts, List<DiagnosticMessage> diagnostics) {
        return new CompileResponse(true, artifacts, List.of(), diagnostics);
    }

    public static CompileResponse failure(String error) {
        return new CompileResponse(false, List.of(), List.of(error), List.of());
    }

    public static CompileResponse failure(List<String> errors) {
        return new CompileResponse(false, List.of(), errors, List.of());
    }

    /** Compilation failed due to analysis errors (may also include warnings). */
    public static CompileResponse analysisFailure(List<DiagnosticMessage> diagnostics) {
        return new CompileResponse(false, List.of(), List.of(), diagnostics);
    }
}
