package uet.ndh.ddsl.analysis.graph;

import uet.ndh.ddsl.analysis.validator.Diagnostic;
import uet.ndh.ddsl.analysis.validator.ValidationRule;
import uet.ndh.ddsl.ast.model.DomainModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation rule that checks bounded context isolation constraints.
 * Ensures cross-context communication only happens through domain events.
 * 
 * Rule IDs: GRAPH002 (ERROR), GRAPH003 (WARNING), GRAPH004 (WARNING)
 */
public class BoundedContextIsolationRule implements ValidationRule<DomainModel> {
    
    private final DepGraph graph;
    private final BoundedContextIsolationValidator isolationValidator;
    
    /**
     * Creates a new bounded context isolation rule with the given dependency graph.
     */
    public BoundedContextIsolationRule(DepGraph graph) {
        this.graph = graph;
        this.isolationValidator = new BoundedContextIsolationValidator();
    }
    
    @Override
    public String ruleId() {
        return "GRAPH002-004";
    }
    
    @Override
    public String description() {
        return "Bounded Context Isolation - Cross-context communication must go through domain events";
    }
    
    @Override
    public Class<DomainModel> applicableTo() {
        return DomainModel.class;
    }
    
    @Override
    public List<Diagnostic> validate(DomainModel model) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        List<BoundedContextIsolationValidator.IsolationViolation> violations = 
            isolationValidator.validate(graph);
        
        for (BoundedContextIsolationValidator.IsolationViolation violation : violations) {
            Diagnostic diagnostic = createDiagnostic(violation);
            if (diagnostic != null) {
                diagnostics.add(diagnostic);
            }
        }
        
        return diagnostics;
    }
    
    private Diagnostic createDiagnostic(BoundedContextIsolationValidator.IsolationViolation violation) {
        switch (violation.violationType()) {
            case DIRECT_CROSS_CONTEXT_REFERENCE:
                // GRAPH002: ERROR
                return Diagnostic.error(
                    violation.edge().referenceSpan(),
                    violation.message(),
                    "GRAPH002"
                );
                
            case CONTEXT_CYCLE:
                // GRAPH001: ERROR (handled by AcyclicDependencyRule, but report here too)
                return Diagnostic.error(
                    violation.source() != null ? violation.source().span() : 
                        uet.ndh.ddsl.ast.SourceSpan.unknown(),
                    violation.message(),
                    "GRAPH001"
                );
                
            case ORPHANED_EVENT_HANDLER:
                // GRAPH003: WARNING
                return Diagnostic.warning(
                    violation.edge().referenceSpan(),
                    violation.message(),
                    "GRAPH003"
                );
                
            case UNHANDLED_EVENT:
                // GRAPH004: WARNING
                return Diagnostic.warning(
                    violation.edge().referenceSpan(),
                    violation.message(),
                    "GRAPH004"
                );
                
            default:
                return null;
        }
    }
}
