package uet.ndh.ddsl.analysis;

import uet.ndh.ddsl.analysis.resolver.EmitParameterResolver;
import uet.ndh.ddsl.analysis.resolver.SymbolResolver;
import uet.ndh.ddsl.analysis.resolver.TypeResolver;
import uet.ndh.ddsl.analysis.scope.SymbolTable;
import uet.ndh.ddsl.analysis.validator.BehaviorSemanticValidator;
import uet.ndh.ddsl.analysis.validator.DddValidator;
import uet.ndh.ddsl.ast.model.DomainModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs all semantic analysis passes over a parsed DDSL domain model.
 */
public class SemanticAnalyzer {

    public SemanticAnalysisResult analyze(DomainModel model) {
        if (model == null) {
            return new SemanticAnalysisResult(
                    new SymbolTable(),
                    List.of(AnalysisDiagnostic.error(null, "Domain model cannot be null", null))
            );
        }

        SymbolTable symbolTable = new SymbolTable();
        List<AnalysisDiagnostic> diagnostics = new ArrayList<>();

        SymbolResolver symbolResolver = new SymbolResolver(symbolTable);
        model.accept(symbolResolver);
        symbolResolver.errors().forEach(err -> diagnostics.add(
                AnalysisDiagnostic.error(err.location(), err.message(), null)
        ));

        TypeResolver typeResolver = new TypeResolver(symbolTable);
        model.accept(typeResolver);

        EmitParameterResolver emitResolver = new EmitParameterResolver(symbolTable);
        model.accept(emitResolver);
        emitResolver.errors().forEach(err -> diagnostics.add(
                AnalysisDiagnostic.error(err.location(), err.message(), "SEM301")
        ));

        typeResolver.errors().forEach(err -> diagnostics.add(
                AnalysisDiagnostic.error(err.location(), err.message(), null)
        ));

        DddValidator dddValidator = new DddValidator();
        model.accept(dddValidator);
        dddValidator.diagnostics().forEach(d -> diagnostics.add(
                new AnalysisDiagnostic(d.location(), d.message(), d.severity(), d.ruleId())
        ));

        BehaviorSemanticValidator behaviorSemanticValidator = new BehaviorSemanticValidator();
        model.accept(behaviorSemanticValidator);
        behaviorSemanticValidator.diagnostics().forEach(d -> diagnostics.add(
                new AnalysisDiagnostic(d.location(), d.message(), d.severity(), d.ruleId())
        ));

        return new SemanticAnalysisResult(symbolTable, diagnostics);
    }
}
