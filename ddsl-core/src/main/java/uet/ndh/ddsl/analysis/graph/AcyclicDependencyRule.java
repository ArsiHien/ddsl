package uet.ndh.ddsl.analysis.graph;

import uet.ndh.ddsl.analysis.validator.Diagnostic;
import uet.ndh.ddsl.analysis.validator.ValidationRule;
import uet.ndh.ddsl.ast.model.DomainModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation rule that checks for cycles in the dependency graph.
 * Enforces the Acyclic Dependency Principle (ADP) from Clean Architecture.
 * 
 * Rule ID: GRAPH001
 * Severity: ERROR
 */
public class AcyclicDependencyRule implements ValidationRule<DomainModel> {
    
    private final DepGraph graph;
    private final CycleDetector cycleDetector;
    
    /**
     * Creates a new acyclic dependency rule with the given dependency graph.
     */
    public AcyclicDependencyRule(DepGraph graph) {
        this.graph = graph;
        this.cycleDetector = new CycleDetector();
    }
    
    @Override
    public String ruleId() {
        return "GRAPH001";
    }
    
    @Override
    public String description() {
        return "Acyclic Dependency Principle - No cycles allowed in aggregate/module dependencies";
    }
    
    @Override
    public Class<DomainModel> applicableTo() {
        return DomainModel.class;
    }
    
    @Override
    public List<Diagnostic> validate(DomainModel model) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        CycleDetector.Result result = cycleDetector.detectCycles(graph);
        
        if (result.hasCycles()) {
            for (CycleDetector.Cycle cycle : result.cycles()) {
                String message = String.format(
                    "Cycle detected in dependencies: %s. " +
                    "Acyclic Dependency Principle violation: components at the same abstraction level " +
                    "must not form circular dependencies.",
                    cycle.pathString()
                );
                
                // Use the first node in the cycle as the error location
                DepNode errorNode = cycle.path().get(0);
                
                diagnostics.add(Diagnostic.error(
                    errorNode.span(),
                    message,
                    ruleId()
                ));
            }
        }
        
        // Also check for cycles within each bounded context
        for (String boundedContext : graph.boundedContexts()) {
            CycleDetector.Result contextResult = 
                cycleDetector.detectCyclesInContext(graph, boundedContext);
            
            if (contextResult.hasCycles()) {
                for (CycleDetector.Cycle cycle : contextResult.cycles()) {
                    String message = String.format(
                        "Cycle detected in bounded context '%s': %s",
                        boundedContext,
                        cycle.pathString()
                    );
                    
                    DepNode errorNode = cycle.path().get(0);
                    
                    diagnostics.add(Diagnostic.error(
                        errorNode.span(),
                        message,
                        ruleId()
                    ));
                }
            }
        }
        
        return diagnostics;
    }
}
