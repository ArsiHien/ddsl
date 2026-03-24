package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.BoundedContextDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.statemachine.StateMachineDecl;
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
    public Void visitBoundedContext(BoundedContextDecl decl) {
        for (StateMachineDecl stateMachine : decl.stateMachines()) {
            validateStateMachine(stateMachine);
        }
        return super.visitBoundedContext(decl);
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

    @Override
    public Void visitBehavior(BehaviorDecl decl) {
        if (decl.errorAccumulationClause() != null
            && decl.errorAccumulationClause().type() == uet.ndh.ddsl.ast.behavior.clause.ErrorAccumulationClause.AccumulationType.COLLECT_UP_TO
            && decl.errorAccumulationClause().maxErrors() <= 0) {
            diagnostics.add(Diagnostic.error(
                decl.errorAccumulationClause().span(),
                "collect up to N errors requires N > 0",
                "SEM101"
            ));
        }
        return super.visitBehavior(decl);
    }

    private void validateStateMachine(StateMachineDecl stateMachine) {
        long initialCount = stateMachine.states().stream().filter(s -> s.isInitial()).count();
        if (initialCount == 0) {
            diagnostics.add(Diagnostic.error(
                stateMachine.span(),
                "State machine must declare exactly one initial state",
                "SEM102"
            ));
        } else if (initialCount > 1) {
            diagnostics.add(Diagnostic.error(
                stateMachine.span(),
                "State machine declares multiple initial states",
                "SEM103"
            ));
        }

        var declaredStates = stateMachine.states().stream().map(s -> s.name()).collect(java.util.stream.Collectors.toSet());
        for (var transition : stateMachine.transitions()) {
            if (!"any".equalsIgnoreCase(transition.targetState()) && !declaredStates.contains(transition.targetState())) {
                diagnostics.add(Diagnostic.error(
                    transition.span(),
                    "Undefined target state in transition: " + transition.targetState(),
                    "SEM104"
                ));
            }
            for (String source : transition.sourceStates()) {
                if (!declaredStates.contains(source)) {
                    diagnostics.add(Diagnostic.error(
                        transition.span(),
                        "Undefined source state in transition: " + source,
                        "SEM105"
                    ));
                }
            }
        }

        for (var guard : stateMachine.guards()) {
            if (!declaredStates.contains(guard.fromState())) {
                diagnostics.add(Diagnostic.error(
                    guard.span(),
                    "Undefined guard source state: " + guard.fromState(),
                    "SEM106"
                ));
            }
            if (!declaredStates.contains(guard.toState())) {
                diagnostics.add(Diagnostic.error(
                    guard.span(),
                    "Undefined guard target state: " + guard.toState(),
                    "SEM107"
                ));
            }
        }
    }
}
