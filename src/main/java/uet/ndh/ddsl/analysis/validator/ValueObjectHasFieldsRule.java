package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule: Value objects should have at least one field.
 */
public class ValueObjectHasFieldsRule implements ValidationRule<ValueObjectDecl> {
    
    @Override
    public String ruleId() {
        return "DDD003";
    }
    
    @Override
    public String description() {
        return "Value objects should have at least one field";
    }
    
    @Override
    public Class<ValueObjectDecl> applicableTo() {
        return ValueObjectDecl.class;
    }
    
    @Override
    public List<Diagnostic> validate(ValueObjectDecl node) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        if (node.fields().isEmpty()) {
            diagnostics.add(Diagnostic.warning(
                node.span(),
                "Value object '" + node.name() + "' has no fields",
                ruleId()
            ));
        }
        
        return diagnostics;
    }
}
