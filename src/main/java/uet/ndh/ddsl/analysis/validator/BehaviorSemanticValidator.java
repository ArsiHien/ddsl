package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.behavior.clause.GivenClause;
import uet.ndh.ddsl.ast.behavior.clause.ThenClause;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.visitor.TreeWalkingVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Semantic validator for behavior-clause assignment targets.
 *
 * <p>Valid targets are resolved from the current owner scope:
 * class fields/dependencies, behavior parameters, and given-clause locals.</p>
 */
public class BehaviorSemanticValidator extends TreeWalkingVisitor<Void> {

    private static final String RULE_ID = "SEM001";

    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private OwnerContext currentOwner;

    private record OwnerContext(String kind, String name, Set<String> fieldTargets) {
    }

    public List<Diagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    public List<Diagnostic> errors() {
        return diagnostics.stream().filter(Diagnostic::isError).toList();
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(Diagnostic::isError);
    }

    @Override
    public Void visitAggregate(AggregateDecl decl) {
        Set<String> fieldTargets = new HashSet<>();
        if (decl.root() != null) {
            fieldTargets.addAll(collectEntityFieldTargets(decl.root()));
        }
        OwnerContext previous = currentOwner;
        currentOwner = new OwnerContext("Aggregate", decl.name(), fieldTargets);
        try {
            return super.visitAggregate(decl);
        } finally {
            currentOwner = previous;
        }
    }

    @Override
    public Void visitEntity(EntityDecl decl) {
        OwnerContext previous = currentOwner;
        currentOwner = new OwnerContext("Entity", decl.name(), collectEntityFieldTargets(decl));
        try {
            return super.visitEntity(decl);
        } finally {
            currentOwner = previous;
        }
    }

    @Override
    public Void visitDomainService(DomainServiceDecl decl) {
        Set<String> fieldTargets = new HashSet<>();
        for (FieldDecl dep : decl.dependencies()) {
            if (dep.name() != null && !dep.name().isBlank()) {
                fieldTargets.add(dep.name());
            }
        }

        OwnerContext previous = currentOwner;
        currentOwner = new OwnerContext("DomainService", decl.name(), fieldTargets);
        try {
            return super.visitDomainService(decl);
        } finally {
            currentOwner = previous;
        }
    }

    @Override
    public Void visitBehavior(BehaviorDecl decl) {
        if (currentOwner == null) {
            return super.visitBehavior(decl);
        }

        Set<String> validTargets = new HashSet<>(currentOwner.fieldTargets());

        // Parameters are valid identifiers within behavior scope
        decl.parameters().forEach(p -> {
            if (p.name() != null && !p.name().isBlank()) {
                validTargets.add(p.name());
            }
        });

        // Given-clause introduces local aliases/variables
        if (decl.givenClause() != null) {
            for (GivenClause.GivenStatement given : decl.givenClause().statements()) {
                if (given.identifier() != null && !given.identifier().isBlank()) {
                    validTargets.add(given.identifier());
                }
            }
        }

        for (ThenClause thenClause : decl.thenClauses()) {
            validateThenStatements(thenClause.statements(), validTargets);
        }

        return super.visitBehavior(decl);
    }

    private Set<String> collectEntityFieldTargets(EntityDecl entity) {
        Set<String> names = new HashSet<>();
        if (entity.identity() != null && entity.identity().name() != null && !entity.identity().name().isBlank()) {
            names.add(entity.identity().name());
        }
        for (FieldDecl f : entity.fields()) {
            if (f.name() != null && !f.name().isBlank()) {
                names.add(f.name());
            }
        }
        return names;
    }

    private void validateThenStatements(List<ThenClause.ThenStatement> statements, Set<String> scopeTargets) {
        for (ThenClause.ThenStatement stmt : statements) {
            validateThenStatement(stmt, scopeTargets);
        }
    }

    private void validateThenStatement(ThenClause.ThenStatement stmt, Set<String> scopeTargets) {
        if (requiresTargetResolution(stmt.type())) {
            String target = stmt.target();
            if (target != null && !target.isBlank() && !scopeTargets.contains(target)) {
                diagnostics.add(Diagnostic.error(
                        stmt.span(),
                        "Identifier '%s' not found in scope of %s '%s'."
                                .formatted(target, currentOwner.kind(), currentOwner.name()),
                        RULE_ID
                ));
            }
        }

        // Nested structures
        if (!stmt.nestedStatements().isEmpty()) {
            Set<String> nestedScope = scopeTargets;
            if (stmt.type() == ThenClause.ThenStatement.ThenStatementType.FOR_EACH
                    && stmt.loopVariable() != null && !stmt.loopVariable().isBlank()) {
                nestedScope = new HashSet<>(scopeTargets);
                nestedScope.add(stmt.loopVariable());
            }
            validateThenStatements(stmt.nestedStatements(), nestedScope);
        }

        if (!stmt.elseStatements().isEmpty()) {
            validateThenStatements(stmt.elseStatements(), scopeTargets);
        }

        for (ThenClause.ThenStatement.ElseIfBranch elseIf : stmt.elseIfBranches()) {
            validateThenStatements(elseIf.statements(), scopeTargets);
        }
    }

    private boolean requiresTargetResolution(ThenClause.ThenStatement.ThenStatementType type) {
        return type == ThenClause.ThenStatement.ThenStatementType.SET
                || type == ThenClause.ThenStatement.ThenStatementType.CHANGE
                || type == ThenClause.ThenStatement.ThenStatementType.RECORD
                || type == ThenClause.ThenStatement.ThenStatementType.ADD
                || type == ThenClause.ThenStatement.ThenStatementType.REMOVE;
    }
}
