package uet.ndh.ddsl.controller.dto;

import java.util.List;

/**
 * Response body for DDSL → Java compilation.
 *
 * @param success   whether compilation succeeded
 * @param artifacts generated Java source files (empty on failure)
 * @param errors    parser or codegen error messages (empty on success)
 */
public record CompileResponse(
        boolean success,
        List<Artifact> artifacts,
        List<String> errors
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

    public static CompileResponse success(List<Artifact> artifacts) {
        return new CompileResponse(true, artifacts, List.of());
    }

    public static CompileResponse failure(String error) {
        return new CompileResponse(false, List.of(), List.of(error));
    }

    public static CompileResponse failure(List<String> errors) {
        return new CompileResponse(false, List.of(), errors);
    }
}
