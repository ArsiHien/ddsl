package uet.ndh.ddsl.analysis.resolver;

import uet.ndh.ddsl.analysis.scope.SymbolTable;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.behavior.clause.EmitClause;
import uet.ndh.ddsl.ast.behavior.clause.ResolvedParameterBinding;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.visitor.TreeWalkingVisitor;

import java.util.*;

/**
 * Emit parameter resolver: resolves each DomainEvent's fields against the
 * current scope using multiple strategies.
 */
public class EmitParameterResolver extends TreeWalkingVisitor<Void> {
    private final SymbolTable symbolTable;
    private final List<ResolutionError> errors = new ArrayList<>();

    // Context tracking (similar to BehaviorSemanticValidator.OwnerContext)
    private AggregateDecl currentAggregate;
    private BehaviorDecl currentBehavior;
    private Set<String> fieldTargets = Set.of();
    private Set<String> behaviorParams = Set.of();
    private Set<String> givenLocals = Set.of();

    public EmitParameterResolver(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public List<ResolutionError> errors() { return List.copyOf(errors); }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public Void visitAggregate(AggregateDecl decl) {
        AggregateDecl previous = currentAggregate;
        currentAggregate = decl;
        fieldTargets = collectFieldTargets(decl);
        try {
            return super.visitAggregate(decl);
        } finally {
            currentAggregate = previous;
        }
    }

    @Override
    public Void visitBehavior(BehaviorDecl decl) {
        BehaviorDecl previous = currentBehavior;
        currentBehavior = decl;
        behaviorParams = collectBehaviorParams(decl);
        givenLocals = collectGivenLocals(decl);
        try {
            if (decl.emitClause() != null) {
                resolveEmitClause(decl.emitClause());
            }
            // Do not traverse deeper for behaviors in this pass
            return null;
        } finally {
            currentBehavior = previous;
        }
    }

    private void resolveEmitClause(EmitClause emitClause) {
        DomainEventDecl eventDecl = findDomainEvent(emitClause.eventName());
        if (eventDecl == null) {
            errors.add(new ResolutionError(emitClause.span(),
                    "Unknown event: " + emitClause.eventName()));
            return;
        }

        Map<String, ResolvedParameterBinding> resolved = new LinkedHashMap<>();

        for (var field : eventDecl.fields()) {
            String fieldName = field.name();
            String fieldType = field.type().name();

            ResolvedParameterBinding binding = resolveFieldBinding(
                    fieldName, fieldType, emitClause);
            resolved.put(fieldName, binding);

            if (binding.kind() == ResolvedParameterBinding.BindingKind.UNRESOLVED) {
                errors.add(new ResolutionError(emitClause.span(),
                        "Cannot resolve event field '" + fieldName + "' of type '" + fieldType + "' for event '" + emitClause.eventName() + "'"));
            }
        }

        // Attach resolved bindings to the emit clause if possible. EmitClause is immutable;
        // we create a new instance with resolved parameters and rely on downstream components
        // to use it where appropriate.
        // Note: actual AST mutation is avoided here per existing immutability contract.
        // emitClause = emitClause.withResolvedParameters(resolved); // not assignable here
    }

    private ResolvedParameterBinding resolveFieldBinding(
            String fieldName, String fieldType, EmitClause emitClause) {
        // 1. EXPLICIT: via with-arguments or property mappings
        if (emitClause.eventArguments().contains(fieldName)) {
            return new ResolvedParameterBinding(
                    emitClause.span(), fieldName,
                    ResolvedParameterBinding.BindingKind.EXPLICIT,
                    fieldName, null);
        }

        for (var mapping : emitClause.propertyMappings()) {
            if (mapping.eventProperty().equals(fieldName)) {
                return new ResolvedParameterBinding(
                        mapping.span(), fieldName,
                        ResolvedParameterBinding.BindingKind.EXPLICIT,
                        mapping.sourceExpression() != null ? mapping.sourceExpression().toString() : null,
                        mapping.sourceExpression());
            }
        }

        // 2. PARAMETER: exact match against behavior parameters
        if (behaviorParams.contains(fieldName)) {
            return new ResolvedParameterBinding(
                    emitClause.span(), fieldName,
                    ResolvedParameterBinding.BindingKind.PARAMETER,
                    fieldName, null);
        }

        // 3. FIELD: exact match against aggregate/entity fields
        if (fieldTargets.contains(fieldName)) {
            return new ResolvedParameterBinding(
                    emitClause.span(), fieldName,
                    ResolvedParameterBinding.BindingKind.FIELD,
                    fieldName, null);
        }

        // 4. GIVEN_LOCAL: given-clause locals
        if (givenLocals.contains(fieldName)) {
            return new ResolvedParameterBinding(
                    emitClause.span(), fieldName,
                    ResolvedParameterBinding.BindingKind.GIVEN_LOCAL,
                    fieldName, null);
        }

        // 5. TEMPORAL_NOW: temporal fields like occurredAt/createdAt/updatedAt or *At
        if (isTemporalField(fieldName, fieldType)) {
            return new ResolvedParameterBinding(
                    emitClause.span(), fieldName,
                    ResolvedParameterBinding.BindingKind.TEMPORAL_NOW,
                    "Instant.now()", null);
        }

        // 6. UNRESOLVED
        return new ResolvedParameterBinding(
                emitClause.span(), fieldName,
                ResolvedParameterBinding.BindingKind.UNRESOLVED,
                null, null);
    }

    private boolean isTemporalField(String fieldName, String fieldType) {
        return ("DateTime".equals(fieldType) || "Instant".equals(fieldType)) &&
                ("occurredAt".equals(fieldName) || "createdAt".equals(fieldName) || "updatedAt".equals(fieldName) || fieldName.endsWith("At"));
    }

    private Set<String> collectFieldTargets(AggregateDecl decl) {
        Set<String> targets = new HashSet<>();
        if (decl.root() != null) {
            if (decl.root().identity() != null) {
                targets.add(decl.root().identity().name());
            }
            for (var field : decl.root().fields()) {
                targets.add(field.name());
            }
        }
        return targets;
    }

    private Set<String> collectBehaviorParams(BehaviorDecl decl) {
        Set<String> params = new HashSet<>();
        for (var param : decl.parameters()) {
            params.add(param.name());
        }
        return params;
    }

    private Set<String> collectGivenLocals(BehaviorDecl decl) {
        Set<String> locals = new HashSet<>();
        if (decl.givenClause() != null) {
            for (var stmt : decl.givenClause().statements()) {
                locals.add(stmt.identifier());
            }
        }
        return locals;
    }

    private DomainEventDecl findDomainEvent(String eventName) {
        if (eventName == null) return null;
        var symbol = symbolTable.resolveType(eventName);
        if (symbol.isPresent()) {
            var node = symbol.get().declaration();
            if (node instanceof DomainEventDecl de) {
                return de;
            }
        }
        return null;
    }

    public record ResolutionError(
        uet.ndh.ddsl.ast.SourceSpan location,
        String message
    ) {}
}
