package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule: Every aggregate must have exactly one root entity.
 */
public class AggregateHasOneRootRule implements ValidationRule<AggregateDecl> {
    
    @Override
    public String ruleId() {
        return "DDD001";
    }
    
    @Override
    public String description() {
        return "Every aggregate must have exactly one root entity";
    }
    
    @Override
    public Class<AggregateDecl> applicableTo() {
        return AggregateDecl.class;
    }
    
    @Override
    public List<Diagnostic> validate(AggregateDecl node) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        if (node.root() == null) {
            diagnostics.add(Diagnostic.error(
                node.span(),
                "Aggregate '" + node.name() + "' must have a root entity",
                ruleId()
            ));
        }
        
        return diagnostics;
    }
}
