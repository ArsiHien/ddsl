package uet.ndh.ddsl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uet.ndh.ddsl.analysis.resolver.SymbolResolver;
import uet.ndh.ddsl.analysis.resolver.TypeResolver;
import uet.ndh.ddsl.analysis.scope.SymbolTable;
import uet.ndh.ddsl.analysis.validator.BehaviorSemanticValidator;
import uet.ndh.ddsl.analysis.validator.DddValidator;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.controller.dto.CompileResponse;
import uet.ndh.ddsl.parser.DdslParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that orchestrates the DDSL → Java compilation pipeline:
 * <ol>
 *   <li>Parse DDSL source into a {@link DomainModel} AST</li>
 *   <li>Run semantic analysis: symbol resolution, type resolution, DDD validation</li>
 *   <li>Run JavaPoet-based code generation via {@link PoetModule}</li>
 *   <li>Return the generated artifacts as {@link CompileResponse}</li>
 * </ol>
 */
@Service
public class DdslCompilationService {

    private static final Logger log = LoggerFactory.getLogger(DdslCompilationService.class);

    /**
     * Compile DDSL source code into Java artifacts.
     *
     * @param ddslCode    valid DDSL source
     * @param basePackage target Java base package
     * @return compilation response with artifacts or errors
     */
    public CompileResponse compile(String ddslCode, String basePackage) {
        String normalizedBasePackage = normalizeBasePackage(basePackage);
        log.info("Compiling DDSL ({} chars) → Java (package: {})",
                ddslCode.length(), normalizedBasePackage);

        // 1. Parse
        DdslParser parser;
        DomainModel model;
        try {
            parser = new DdslParser(ddslCode, "<input>");
            model = parser.parse();
        } catch (Exception e) {
            log.error("DDSL parse failed", e);
            return CompileResponse.failure("Parse error: " + e.getMessage());
        }

        if (parser.hasErrors()) {
            List<String> errors = parser.getErrors().stream()
                    .map(err -> "line %d:%d — %s".formatted(err.line(), err.column(), err.message()))
                    .toList();
            log.warn("DDSL parse produced {} errors", errors.size());
            return CompileResponse.failure(errors);
        }

        // 2. Semantic analysis
        List<CompileResponse.DiagnosticMessage> allDiagnostics = new ArrayList<>();

        try {
            // Pass 1: Symbol resolution — build the symbol table
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

            // Pass 2: Type resolution — check all TypeRef usages
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

            // Pass 3: DDD validation — enforce DDD rules
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

            // Pass 4: Behavior semantic validation — resolve assignment targets in clauses
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
            log.error("Semantic analysis failed", e);
            return CompileResponse.failure("Analysis error: " + e.getMessage());
        }

        boolean hasErrors = allDiagnostics.stream()
                .anyMatch(d -> "ERROR".equals(d.severity()));

        if (hasErrors) {
            long errorCount = allDiagnostics.stream()
                    .filter(d -> "ERROR".equals(d.severity())).count();
            log.warn("Semantic analysis produced {} error(s), aborting codegen", errorCount);
            return CompileResponse.analysisFailure(allDiagnostics);
        }

        if (!allDiagnostics.isEmpty()) {
            log.warn("Semantic analysis produced {} warning(s), proceeding with codegen",
                    allDiagnostics.size());
        }

        // 3. Generate Java code
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

            log.info("Code generation produced {} artifacts", artifacts.size());

            return allDiagnostics.isEmpty()
                    ? CompileResponse.success(artifacts)
                    : CompileResponse.successWithWarnings(artifacts, allDiagnostics);

        } catch (Exception e) {
            log.error("Code generation failed", e);
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
