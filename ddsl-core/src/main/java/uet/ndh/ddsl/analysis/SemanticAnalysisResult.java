package uet.ndh.ddsl.analysis;

import uet.ndh.ddsl.analysis.scope.SymbolTable;

import java.util.List;

/**
 * Result of running the semantic analysis pipeline over a parsed domain model.
 */
public record SemanticAnalysisResult(
        SymbolTable symbolTable,
        List<AnalysisDiagnostic> diagnostics
) {

    public SemanticAnalysisResult {
        diagnostics = diagnostics != null ? List.copyOf(diagnostics) : List.of();
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(AnalysisDiagnostic::isError);
    }

    public List<AnalysisDiagnostic> errors() {
        return diagnostics.stream()
                .filter(AnalysisDiagnostic::isError)
                .toList();
    }
}
