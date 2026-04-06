package uet.ndh.ddsl.compiler;

import uet.ndh.ddsl.compiler.metrics.CompilationMetrics;
import java.util.List;

/**
 * Result of a compilation operation.
 */
public class CompilationResult {
    private final boolean success;
    private final List<String> errors;
    private final List<String> generatedFiles;
    private final CompilationMetrics metrics;

    public CompilationResult(boolean success, List<String> errors, List<String> generatedFiles, CompilationMetrics metrics) {
        this.success = success;
        this.errors = errors;
        this.generatedFiles = generatedFiles;
        this.metrics = metrics;
    }

    // Backward compatibility constructor
    public CompilationResult(boolean success, List<String> errors, List<String> generatedFiles) {
        this(success, errors, generatedFiles, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getGeneratedFiles() {
        return generatedFiles;
    }

    public CompilationMetrics getMetrics() {
        return metrics;
    }

    public boolean hasMetrics() {
        return metrics != null;
    }
}
