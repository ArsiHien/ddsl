package uet.ndh.ddsl.compiler.api;

import java.util.List;

/**
 * Core compilation response shared by LSP and API layers.
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

    /** A single generated Java source file. */
    public record Artifact(
            String fileName,
            String packageName,
            String type,
            String sourceCode
    ) {}

    /** A single semantic analysis diagnostic. */
    public record DiagnosticMessage(
            String message,
            String severity,
            int line,
            int column,
            String ruleId
    ) {}

    public static CompileResponse success(List<Artifact> artifacts) {
        return new CompileResponse(true, artifacts, List.of(), List.of());
    }

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

    public static CompileResponse analysisFailure(List<DiagnosticMessage> diagnostics) {
        return new CompileResponse(false, List.of(), List.of(), diagnostics);
    }
}
