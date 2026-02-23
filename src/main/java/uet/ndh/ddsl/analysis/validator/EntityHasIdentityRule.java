package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.model.entity.EntityDecl;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule: Every entity must have an identity field.
 */
public class EntityHasIdentityRule implements ValidationRule<EntityDecl> {
    
    @Override
    public String ruleId() {
        return "DDD002";
    }
    
    @Override
    public String description() {
        return "Every entity must have an identity field";
    }
    
    @Override
    public Class<EntityDecl> applicableTo() {
        return EntityDecl.class;
    }
    
    @Override
    public List<Diagnostic> validate(EntityDecl node) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        if (node.identity() == null) {
            diagnostics.add(Diagnostic.error(
                node.span(),
                "Entity '" + node.name() + "' must have an identity field",
                ruleId()
            ));
        }
        
        return diagnostics;
    }
}
