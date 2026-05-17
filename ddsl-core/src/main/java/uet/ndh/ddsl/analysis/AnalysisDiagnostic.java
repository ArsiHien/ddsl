package uet.ndh.ddsl.analysis;

import uet.ndh.ddsl.analysis.validator.Diagnostic;
import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Diagnostic emitted by the semantic analysis pipeline.
 */
public record AnalysisDiagnostic(
        SourceSpan location,
        String message,
        Diagnostic.Severity severity,
        String ruleId
) {

    public static AnalysisDiagnostic error(SourceSpan location, String message, String ruleId) {
        return new AnalysisDiagnostic(location, message, Diagnostic.Severity.ERROR, ruleId);
    }

    public boolean isError() {
        return severity == Diagnostic.Severity.ERROR;
    }
}
