package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.ast.visitor.TreeWalkingVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validator that runs all DDD validation rules on the AST.
 */
public class DddValidator extends TreeWalkingVisitor<Void> {
    
    private final Map<Class<?>, List<ValidationRule<?>>> rulesByType;
    private final List<Diagnostic> diagnostics;
    
    public DddValidator() {
        this.rulesByType = new HashMap<>();
        this.diagnostics = new ArrayList<>();
        registerDefaultRules();
    }
    
    private void registerDefaultRules() {
        register(new AggregateHasOneRootRule());
        register(new EntityHasIdentityRule());
        register(new ValueObjectHasFieldsRule());
    }
    
    public <T extends AstNode> void register(ValidationRule<T> rule) {
        rulesByType.computeIfAbsent(rule.applicableTo(), k -> new ArrayList<>())
                   .add(rule);
    }
    
    public List<Diagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }
    
    public List<Diagnostic> errors() {
        return diagnostics.stream()
            .filter(Diagnostic::isError)
            .toList();
    }
    
    public List<Diagnostic> warnings() {
        return diagnostics.stream()
            .filter(Diagnostic::isWarning)
            .toList();
    }
    
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(Diagnostic::isError);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends AstNode> void runRules(T node) {
        List<ValidationRule<?>> rules = rulesByType.get(node.getClass());
        if (rules != null) {
            for (ValidationRule<?> rule : rules) {
                ValidationRule<T> typedRule = (ValidationRule<T>) rule;
                diagnostics.addAll(typedRule.validate(node));
            }
        }
    }
    
    @Override
    public Void visitAggregate(AggregateDecl decl) {
        runRules(decl);
        return super.visitAggregate(decl);
    }
    
    @Override
    public Void visitEntity(EntityDecl decl) {
        runRules(decl);
        return super.visitEntity(decl);
    }
    
    @Override
    public Void visitValueObject(ValueObjectDecl decl) {
        runRules(decl);
        return super.visitValueObject(decl);
    }
}
