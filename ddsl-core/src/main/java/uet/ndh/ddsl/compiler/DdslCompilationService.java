package uet.ndh.ddsl.compiler;

import uet.ndh.ddsl.analysis.resolver.SymbolResolver;
import uet.ndh.ddsl.analysis.resolver.TypeResolver;
import uet.ndh.ddsl.analysis.scope.SymbolTable;
import uet.ndh.ddsl.analysis.validator.BehaviorSemanticValidator;
import uet.ndh.ddsl.analysis.validator.DddValidator;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.compiler.api.CompileResponse;
import uet.ndh.ddsl.parser.DdslParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Core compile pipeline service used by LSP and HTTP API layers.
 */
public class DdslCompilationService {

    /**
     * Compile DDSL source code into Java artifacts.
     *
     * @param ddslCode valid DDSL source
     * @param basePackage target Java base package
     * @return compilation response with full generated artifacts source
     */
    public CompileResponse compile(String ddslCode, String basePackage) {
        if (ddslCode == null || ddslCode.isBlank()) {
            return CompileResponse.failure("DDSL code cannot be empty");
        }

        String normalizedBasePackage = normalizeBasePackage(basePackage);

        DdslParser parser;
        DomainModel model;
        try {
            parser = new DdslParser(ddslCode, "<input>");
            model = parser.parse();
        } catch (Exception e) {
            return CompileResponse.failure("Parse error: " + e.getMessage());
        }

        if (parser.hasErrors()) {
            List<String> errors = parser.getErrors().stream()
                    .map(err -> "line %d:%d — %s".formatted(err.line(), err.column(), err.message()))
                    .toList();
            return CompileResponse.failure(errors);
        }

        List<CompileResponse.DiagnosticMessage> allDiagnostics = new ArrayList<>();

        try {
            SymbolTable symbolTable = new SymbolTable();
            SymbolResolver symbolResolver = new SymbolResolver(symbolTable);
            model.accept(symbolResolver);

            symbolResolver.errors().forEach(err -> allDiagnostics.add(
                    new CompileResponse.DiagnosticMessage(
                            err.message(),
                            "ERROR",
                            err.location() != null ? err.location().startLine() : 0,
                            err.location() != null ? err.location().startColumn() : 0,
                            null
                    )
            ));

            TypeResolver typeResolver = new TypeResolver(symbolTable);
            model.accept(typeResolver);

            typeResolver.errors().forEach(err -> allDiagnostics.add(
                    new CompileResponse.DiagnosticMessage(
                            err.message(),
                            "ERROR",
                            err.location() != null ? err.location().startLine() : 0,
                            err.location() != null ? err.location().startColumn() : 0,
                            null
                    )
            ));

            DddValidator dddValidator = new DddValidator();
            model.accept(dddValidator);

            dddValidator.diagnostics().forEach(d -> allDiagnostics.add(
                    new CompileResponse.DiagnosticMessage(
                            d.message(),
                            d.severity().name(),
                            d.location() != null ? d.location().startLine() : 0,
                            d.location() != null ? d.location().startColumn() : 0,
                            d.ruleId()
                    )
            ));

            BehaviorSemanticValidator behaviorSemanticValidator = new BehaviorSemanticValidator();
            model.accept(behaviorSemanticValidator);

            behaviorSemanticValidator.diagnostics().forEach(d -> allDiagnostics.add(
                    new CompileResponse.DiagnosticMessage(
                            d.message(),
                            d.severity().name(),
                            d.location() != null ? d.location().startLine() : 0,
                            d.location() != null ? d.location().startColumn() : 0,
                            d.ruleId()
                    )
            ));

        } catch (Exception e) {
            return CompileResponse.failure("Analysis error: " + e.getMessage());
        }

        boolean hasErrors = allDiagnostics.stream()
                .anyMatch(d -> "ERROR".equals(d.severity()));

        if (hasErrors) {
            return CompileResponse.analysisFailure(allDiagnostics);
        }

        try {
            var poetModule = new PoetModule(normalizedBasePackage);
            List<CodeArtifact> codeArtifacts = poetModule.generateFromModel(model);

            List<CompileResponse.Artifact> artifacts = codeArtifacts.stream()
                    .map(a -> new CompileResponse.Artifact(
                            a.fileName(),
                            a.packageName(),
                            a.artifactType().name(),
                            a.sourceCode()
                    ))
                    .toList();

            return allDiagnostics.isEmpty()
                    ? CompileResponse.success(artifacts)
                    : CompileResponse.successWithWarnings(artifacts, allDiagnostics);

        } catch (Exception e) {
            return CompileResponse.failure("Codegen error: " + e.getMessage());
        }
    }

    private String normalizeBasePackage(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            return "com.example.domain";
        }

        String normalized = basePackage.trim();
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
