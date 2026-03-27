package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.behavior.clause.GivenClause;
import uet.ndh.ddsl.ast.behavior.clause.ReturnClause;
import uet.ndh.ddsl.ast.behavior.clause.ThenClause;
import uet.ndh.ddsl.ast.expr.BinaryExpr;
import uet.ndh.ddsl.ast.expr.CollectionAggregation;
import uet.ndh.ddsl.ast.expr.CollectionFilter;
import uet.ndh.ddsl.ast.expr.CollectionFlatten;
import uet.ndh.ddsl.ast.expr.CollectionGroupBy;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.expr.FieldAccessExpr;
import uet.ndh.ddsl.ast.expr.ListExpr;
import uet.ndh.ddsl.ast.expr.MapExpr;
import uet.ndh.ddsl.ast.expr.MatchExpr;
import uet.ndh.ddsl.ast.expr.MethodCallExpr;
import uet.ndh.ddsl.ast.expr.NewInstanceExpr;
import uet.ndh.ddsl.ast.expr.SpecificationCondition;
import uet.ndh.ddsl.ast.expr.StringCondition;
import uet.ndh.ddsl.ast.expr.StringOperation;
import uet.ndh.ddsl.ast.expr.TemporalComparison;
import uet.ndh.ddsl.ast.expr.TemporalRange;
import uet.ndh.ddsl.ast.expr.TemporalRelative;
import uet.ndh.ddsl.ast.expr.TemporalSequence;
import uet.ndh.ddsl.ast.expr.TernaryExpr;
import uet.ndh.ddsl.ast.expr.ThisExpr;
import uet.ndh.ddsl.ast.expr.UnaryExpr;
import uet.ndh.ddsl.ast.expr.VariableExpr;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.visitor.TreeWalkingVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Semantic validator for behavior-clause assignment targets.
 *
 * <p>Valid targets are resolved from the current owner scope:
 * class fields/dependencies, behavior parameters, and given-clause locals.</p>
 */
public class BehaviorSemanticValidator extends TreeWalkingVisitor<Void> {

    private static final String UNDEFINED_TARGET_RULE_ID = "SEM001";
    private static final String UNDEFINED_IDENTIFIER_RULE_ID = "SEM108";
    private static final String UNDEFINED_METHOD_RULE_ID = "SEM202";
    private static final String METHOD_ARITY_MISMATCH_RULE_ID = "SEM204";

    private static final Set<String> RESERVED_WORDS = Set.of(
            "a", "all", "an", "and", "any", "as", "at", "before", "after", "ago",
            "between", "by", "collect", "contains", "count", "create", "created", "default",
            "does", "else", "emit", "end", "ends", "equal", "equals", "error", "errors",
            "exists", "fail", "false", "for", "format", "from", "given", "greater", "group",
            "grouped", "has", "have", "if", "in", "is", "item", "items", "last", "least",
            "length", "less", "match", "matches", "maximum", "minimum", "more", "no", "not",
            "now", "null", "of", "on", "one", "only", "or", "otherwise", "present", "record",
            "remove", "require", "return", "set", "starts", "status", "sum", "than", "that",
            "the", "then", "to", "today", "tomorrow", "true", "until", "valid", "warning",
            "warnings", "when", "where", "with", "within", "yesterday"
    );

    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final Set<String> emittedDiagnosticKeys = new HashSet<>();
    private OwnerContext currentOwner;

        private record OwnerContext(
            String kind,
            String name,
            Set<String> fieldTargets,
            Map<String, Integer> localMethodArities,
            Map<String, Map<String, Integer>> receiverMethodArities
        ) {
    }

        private final Map<String, Map<String, Integer>> boundedContextReceiverMethodArities = new HashMap<>();

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
    public Void visitBoundedContext(uet.ndh.ddsl.ast.model.BoundedContextDecl decl) {
        boundedContextReceiverMethodArities.clear();
        indexReceiverMethods(decl.repositories());
        indexReceiverMethodsForServices(decl.domainServices());

        for (var module : decl.modules()) {
            module.accept(this);
        }
        for (var aggregate : decl.aggregates()) {
            aggregate.accept(this);
        }
        for (var valueObject : decl.valueObjects()) {
            valueObject.accept(this);
        }
        for (var service : decl.domainServices()) {
            service.accept(this);
        }
        for (var appService : decl.applicationServices()) {
            appService.accept(this);
        }
        return null;
    }

    @Override
    public Void visitAggregate(AggregateDecl decl) {
        Set<String> fieldTargets = new HashSet<>();
        if (decl.root() != null) {
            fieldTargets.addAll(collectEntityFieldTargets(decl.root()));
        }

        Map<String, Integer> localMethods = new HashMap<>();
        if (decl.root() != null) {
            collectBehaviorArities(decl.root().behaviors(), localMethods);
        }
        collectBehaviorArities(decl.behaviors(), localMethods);

        OwnerContext previous = currentOwner;
        currentOwner = new OwnerContext(
                "Aggregate",
                decl.name(),
                fieldTargets,
                Map.copyOf(localMethods),
                copyReceiverMethodIndex()
        );
        try {
            return super.visitAggregate(decl);
        } finally {
            currentOwner = previous;
        }
    }

    @Override
    public Void visitEntity(EntityDecl decl) {
        OwnerContext previous = currentOwner;
        Map<String, Integer> localMethods = new HashMap<>();
        collectBehaviorArities(decl.behaviors(), localMethods);
        currentOwner = new OwnerContext(
                "Entity",
                decl.name(),
                collectEntityFieldTargets(decl),
                Map.copyOf(localMethods),
                copyReceiverMethodIndex()
        );
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

        Map<String, Integer> localMethods = new HashMap<>();
        collectBehaviorArities(decl.behaviors(), localMethods);
        for (var method : decl.methods()) {
            if (method != null && method.name() != null && !method.name().isBlank()) {
                localMethods.put(method.name(), method.parameters() != null ? method.parameters().size() : 0);
            }
        }

        OwnerContext previous = currentOwner;
        currentOwner = new OwnerContext(
                "DomainService",
                decl.name(),
                fieldTargets,
                Map.copyOf(localMethods),
                copyReceiverMethodIndex()
        );
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

        validateBehaviorParameters(decl.parameters(), validTargets);

        if (decl.requireClause() != null) {
            for (var requireCondition : decl.requireClause().conditions()) {
                if (requireCondition != null) {
                    validateConditionReferences(
                            requireCondition.condition(),
                            validTargets,
                            requireCondition.span(),
                            "require condition"
                    );
                }
            }
        }

        if (decl.errorAccumulationClause() != null) {
            for (var rule : decl.errorAccumulationClause().validationRules()) {
                validateConditionReferences(rule.condition(), validTargets, rule.span(), "validation rule");
            }
            for (var group : decl.errorAccumulationClause().groupedValidations()) {
                for (var rule : group.validations()) {
                    validateConditionReferences(rule.condition(), validTargets, rule.span(), "validation rule");
                }
            }
        }

        // Given-clause introduces local aliases/variables (in declaration order)
        if (decl.givenClause() != null) {
            for (GivenClause.GivenStatement given : decl.givenClause().statements()) {
                validateExpressionReferences(given.expression(), validTargets, given.span(), "given expression");

                for (String serviceArgument : given.serviceArguments()) {
                    validateIdentifierText(serviceArgument, validTargets, given.span(), "given service argument");
                }

                if (given.identifier() != null && !given.identifier().isBlank()) {
                    validTargets.add(given.identifier());
                }
            }
        }

        for (ThenClause thenClause : decl.thenClauses()) {
            validateThenStatements(thenClause.statements(), validTargets);
        }

        if (decl.emitClause() != null) {
            for (String eventArg : decl.emitClause().eventArguments()) {
                validateIdentifierText(eventArg, validTargets, decl.emitClause().span(), "emit argument");
            }
            for (var mapping : decl.emitClause().propertyMappings()) {
                validateExpressionReferences(mapping.sourceExpression(), validTargets, mapping.span(), "emit mapping");
            }
        }

        if (decl.returnClause() != null) {
            validateReturnClause(decl.returnClause(), validTargets);
        }

        return super.visitBehavior(decl);
    }

    private void validateBehaviorParameters(List<ParameterDecl> parameters, Set<String> validTargets) {
        for (ParameterDecl parameter : parameters) {
            if (parameter == null || parameter.name() == null || parameter.name().isBlank()) {
                continue;
            }

            boolean explicitlyTyped = parameter.type() != null
                    && parameter.type().name() != null
                    && !parameter.type().name().isBlank()
                    && !"Object".equals(parameter.type().name());

            if (explicitlyTyped || currentOwner.fieldTargets().contains(parameter.name())) {
                validTargets.add(parameter.name());
            } else {
                addDiagnosticOnce(
                        parameter.span(),
                        "Identifier '%s' not found in scope of %s '%s'."
                                .formatted(parameter.name(), currentOwner.kind(), currentOwner.name()),
                        UNDEFINED_IDENTIFIER_RULE_ID
                );
            }
        }
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
        if (stmt.type() == ThenClause.ThenStatement.ThenStatementType.FOR_EACH
            && stmt.target() != null && !stmt.target().isBlank()) {
            validateIdentifierText(stmt.target(), scopeTargets, stmt.span(), "for-each collection");
        }

        if (requiresTargetResolution(stmt.type())) {
            String target = stmt.target();
            if (target != null && !target.isBlank() && !scopeTargets.contains(target)) {
            addDiagnosticOnce(
                stmt.span(),
                "Identifier '%s' not found in scope of %s '%s'."
                    .formatted(target, currentOwner.kind(), currentOwner.name()),
                UNDEFINED_TARGET_RULE_ID
            );
            }
        }

        validateExpressionReferences(stmt.expression(), scopeTargets, stmt.span(), "then expression");
        validateMethodCallSemantics(stmt.expression(), scopeTargets, stmt.span());
        validateConditionReferences(stmt.condition(), scopeTargets, stmt.span(), "then condition");

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

    private void validateReturnClause(ReturnClause returnClause, Set<String> scopeTargets) {
        validateExpressionReferences(returnClause.expression(), scopeTargets, returnClause.span(), "return expression");
        for (ReturnClause.PropertyInitialization property : returnClause.properties()) {
            validateExpressionReferences(property.valueExpression(), scopeTargets, property.span(), "return property initialization");
        }
    }

    private void validateConditionReferences(
            uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition condition,
            Set<String> scopeTargets,
            uet.ndh.ddsl.ast.SourceSpan location,
            String context
    ) {
        if (condition == null) {
            return;
        }

        switch (condition.type()) {
            case IS_EMPTY, IS_NOT_EMPTY, EXISTS_IN_SYSTEM, DOES_NOT_EXIST ->
                    validateIdentifierText(condition.quantifierTarget(), scopeTargets, location, context);
            case UNIVERSAL, EXISTENTIAL, NEGATED_EXISTENTIAL ->
                    // Validate the quantified collection only; nested property checks
                    // are usually evaluated against collection elements.
                    validateIdentifierText(condition.quantifierTarget(), scopeTargets, location, context);
            default -> {
                validateExpressionReferences(condition.leftExpression(), scopeTargets, location, context);
                validateExpressionReferences(condition.rightExpression(), scopeTargets, location, context);
            }
        }
    }

    private void validateExpressionReferences(
            Expr expression,
            Set<String> scopeTargets,
            uet.ndh.ddsl.ast.SourceSpan location,
            String context
    ) {
        if (expression == null) {
            return;
        }

        if (expression instanceof VariableExpr variableExpr) {
            validateIdentifierText(variableExpr.name(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof BinaryExpr binaryExpr) {
            validateExpressionReferences(binaryExpr.left(), scopeTargets, location, context);
            validateExpressionReferences(binaryExpr.right(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof UnaryExpr unaryExpr) {
            validateExpressionReferences(unaryExpr.operand(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof TernaryExpr ternaryExpr) {
            validateExpressionReferences(ternaryExpr.condition(), scopeTargets, location, context);
            validateExpressionReferences(ternaryExpr.thenExpr(), scopeTargets, location, context);
            validateExpressionReferences(ternaryExpr.elseExpr(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof MethodCallExpr methodCallExpr) {
            validateExpressionReferences(methodCallExpr.receiver(), scopeTargets, location, context);
            for (Expr argument : methodCallExpr.arguments()) {
                validateExpressionReferences(argument, scopeTargets, location, context);
            }
            return;
        }

        if (expression instanceof FieldAccessExpr fieldAccessExpr) {
            validateExpressionReferences(fieldAccessExpr.object(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof NewInstanceExpr newInstanceExpr) {
            for (Expr argument : newInstanceExpr.arguments()) {
                validateExpressionReferences(argument, scopeTargets, location, context);
            }
            return;
        }

        if (expression instanceof ListExpr listExpr) {
            for (Expr element : listExpr.elements()) {
                validateExpressionReferences(element, scopeTargets, location, context);
            }
            return;
        }

        if (expression instanceof MapExpr mapExpr) {
            mapExpr.entries().forEach((k, v) -> {
                validateExpressionReferences(k, scopeTargets, location, context);
                validateExpressionReferences(v, scopeTargets, location, context);
            });
            return;
        }

        if (expression instanceof StringCondition stringCondition) {
            validateExpressionReferences(stringCondition.stringExpr(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof StringOperation stringOperation) {
            validateExpressionReferences(stringOperation.stringExpr(), scopeTargets, location, context);
            validateExpressionReferences(stringOperation.otherStringExpr(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof TemporalComparison temporalComparison) {
            validateExpressionReferences(temporalComparison.dateTimeExpr(), scopeTargets, location, context);
            if (temporalComparison.anchor() != null && temporalComparison.anchor().expression() != null) {
                validateExpressionReferences(temporalComparison.anchor().expression(), scopeTargets, location, context);
            }
            return;
        }

        if (expression instanceof TemporalRange temporalRange) {
            validateExpressionReferences(temporalRange.dateTimeExpr(), scopeTargets, location, context);
            validateExpressionReferences(temporalRange.startExpr(), scopeTargets, location, context);
            validateExpressionReferences(temporalRange.endExpr(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof TemporalRelative temporalRelative) {
            validateExpressionReferences(temporalRelative.dateTimeExpr(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof TemporalSequence temporalSequence) {
            validateExpressionReferences(temporalSequence.dateTimeExpr(), scopeTargets, location, context);
            validateExpressionReferences(temporalSequence.otherDateTimeExpr(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof CollectionAggregation aggregation) {
            // Validate only root collection reference; inner where clauses are
            // typically evaluated against collection element context.
            validateExpressionReferences(aggregation.collection(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof CollectionFilter filter) {
            validateExpressionReferences(filter.collection(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof CollectionFlatten flatten) {
            validateExpressionReferences(flatten.collection(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof CollectionGroupBy groupBy) {
            validateExpressionReferences(groupBy.collection(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof SpecificationCondition specificationCondition) {
            validateExpressionReferences(specificationCondition.subject(), scopeTargets, location, context);
            validateSpecificationRef(specificationCondition.specificationRef(), scopeTargets, location, context);
            return;
        }

        if (expression instanceof MatchExpr matchExpr) {
            validateExpressionReferences(matchExpr.matchTarget(), scopeTargets, location, context);
            for (MatchExpr.MatchCase matchCase : matchExpr.cases()) {
                validateConditionReferences(matchCase.guard(), scopeTargets, location, context);
                if (matchCase.body() instanceof MatchExpr.MatchCaseBody.ExpressionBody expressionBody) {
                    validateExpressionReferences(expressionBody.expression(), scopeTargets, location, context);
                }
            }
            if (matchExpr.defaultCase() != null
                    && matchExpr.defaultCase().body() instanceof MatchExpr.MatchCaseBody.ExpressionBody expressionBody) {
                validateExpressionReferences(expressionBody.expression(), scopeTargets, location, context);
            }
        }
    }

    private void validateSpecificationRef(
            SpecificationCondition.SpecificationRef specificationRef,
            Set<String> scopeTargets,
            uet.ndh.ddsl.ast.SourceSpan location,
            String context
    ) {
        if (specificationRef == null) {
            return;
        }

        if (specificationRef instanceof SpecificationCondition.SpecificationRef.Parameterized parameterized) {
            for (Expr argument : parameterized.arguments()) {
                validateExpressionReferences(argument, scopeTargets, location, context);
            }
            return;
        }

        if (specificationRef instanceof SpecificationCondition.SpecificationRef.Composite composite) {
            validateSpecificationRef(composite.left(), scopeTargets, location, context);
            validateSpecificationRef(composite.right(), scopeTargets, location, context);
            return;
        }

        if (specificationRef instanceof SpecificationCondition.SpecificationRef.Negation negation) {
            validateSpecificationRef(negation.inner(), scopeTargets, location, context);
        }
    }

    private void validateIdentifierText(
            String text,
            Set<String> scopeTargets,
            uet.ndh.ddsl.ast.SourceSpan location,
            String context
    ) {
        if (text == null || text.isBlank()) {
            return;
        }

        String trimmed = text.trim();
        if (scopeTargets.contains(trimmed) || isEquivalentToScopedName(trimmed, scopeTargets)) {
            return;
        }

        if (currentOwner != null && resolveReceiverMethods(trimmed) != null) {
            return;
        }

        for (String identifier : extractIdentifierCandidates(trimmed)) {
            if (!scopeTargets.contains(identifier) && !isEquivalentToScopedName(identifier, scopeTargets)) {
                addDiagnosticOnce(
                        location,
                        "Identifier '%s' not found in scope of %s '%s'."
                                .formatted(identifier, currentOwner.kind(), currentOwner.name()),
                        UNDEFINED_IDENTIFIER_RULE_ID
                );
            }
        }
    }

    private Set<String> extractIdentifierCandidates(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(text
                        .replace("->", " ")
                        .replace(".", " ")
                        .split("[^A-Za-z0-9_]+"))
                .filter(token -> token != null && !token.isBlank())
                .filter(token -> !Character.isDigit(token.charAt(0)))
                .filter(token -> !Character.isUpperCase(token.charAt(0)))
                .filter(token -> !RESERVED_WORDS.contains(token.toLowerCase()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isEquivalentToScopedName(String candidate, Set<String> scopeTargets) {
        String normalizedCandidate = normalizeName(candidate);
        if (normalizedCandidate.isBlank()) {
            return false;
        }
        for (String scopedName : scopeTargets) {
            if (normalizeName(scopedName).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private void addDiagnosticOnce(uet.ndh.ddsl.ast.SourceSpan location, String message, String ruleId) {
        int line = location != null ? location.startLine() : 0;
        int column = location != null ? location.startColumn() : 0;
        String dedupeKey = ruleId + "|" + line + "|" + column + "|" + message;

        if (emittedDiagnosticKeys.add(dedupeKey)) {
            diagnostics.add(Diagnostic.error(location, message, ruleId));
        }
    }

    private void validateMethodCallSemantics(Expr expression, Set<String> scopeTargets, uet.ndh.ddsl.ast.SourceSpan location) {
        if (!(expression instanceof MethodCallExpr callExpr) || currentOwner == null) {
            return;
        }

        int providedArgCount = callExpr.arguments() != null ? callExpr.arguments().size() : 0;

        if (!callExpr.hasReceiver()) {
            Integer expectedArity = currentOwner.localMethodArities().get(callExpr.methodName());
            if (expectedArity == null) {
                addDiagnosticOnce(
                        location,
                        "Method '%s' not found in scope of %s '%s'."
                                .formatted(callExpr.methodName(), currentOwner.kind(), currentOwner.name()),
                        UNDEFINED_METHOD_RULE_ID
                );
                return;
            }

            if (!expectedArity.equals(providedArgCount)) {
                addDiagnosticOnce(
                        location,
                        "Method '%s' expects %d argument(s), got %d."
                                .formatted(callExpr.methodName(), expectedArity, providedArgCount),
                        METHOD_ARITY_MISMATCH_RULE_ID
                );
            }
            return;
        }

        if (callExpr.receiver() instanceof ThisExpr) {
            Integer expectedArity = currentOwner.localMethodArities().get(callExpr.methodName());
            if (expectedArity == null) {
                addDiagnosticOnce(
                        location,
                        "Method '%s' not found in scope of %s '%s'."
                                .formatted(callExpr.methodName(), currentOwner.kind(), currentOwner.name()),
                        UNDEFINED_METHOD_RULE_ID
                );
                return;
            }

            if (!expectedArity.equals(providedArgCount)) {
                addDiagnosticOnce(
                        location,
                        "Method '%s' expects %d argument(s), got %d."
                                .formatted(callExpr.methodName(), expectedArity, providedArgCount),
                        METHOD_ARITY_MISMATCH_RULE_ID
                );
            }
            return;
        }

        if (callExpr.receiver() instanceof VariableExpr receiverVar) {
            String receiverName = receiverVar.name();
            Map<String, Integer> methods = resolveReceiverMethods(receiverName);
            if (methods == null) {
                return; // receiver scope is validated elsewhere by identifier checks
            }

            Integer expectedArity = methods.get(callExpr.methodName());
            if (expectedArity == null) {
                addDiagnosticOnce(
                        location,
                        "Method '%s' not found on receiver '%s'."
                                .formatted(callExpr.methodName(), receiverName),
                        UNDEFINED_METHOD_RULE_ID
                );
                return;
            }

            if (!expectedArity.equals(providedArgCount)) {
                addDiagnosticOnce(
                        location,
                        "Method '%s' on '%s' expects %d argument(s), got %d."
                                .formatted(callExpr.methodName(), receiverName, expectedArity, providedArgCount),
                        METHOD_ARITY_MISMATCH_RULE_ID
                );
            }
        }
    }

    private void collectBehaviorArities(List<BehaviorDecl> behaviors, Map<String, Integer> sink) {
        for (BehaviorDecl behavior : behaviors) {
            if (behavior != null && behavior.getName() != null && !behavior.getName().isBlank()) {
                sink.put(behavior.getName(), behavior.parameters() != null ? behavior.parameters().size() : 0);
            }
        }
    }

    private void indexReceiverMethods(List<RepositoryDecl> repositories) {
        for (RepositoryDecl repository : repositories) {
            if (repository == null || repository.name() == null || repository.name().isBlank()) {
                continue;
            }
            Map<String, Integer> methodArities = new HashMap<>();
            for (var method : repository.methods()) {
                if (method != null && method.name() != null && !method.name().isBlank()) {
                    methodArities.put(method.name(), method.parameters() != null ? method.parameters().size() : 0);
                }
            }

            registerReceiverMethodArities(repository.name(), methodArities);
            registerReceiverMethodArities(decapitalize(repository.name()), methodArities);
        }
    }

    private void indexReceiverMethodsForServices(List<DomainServiceDecl> services) {
        for (DomainServiceDecl service : services) {
            if (service == null || service.name() == null || service.name().isBlank()) {
                continue;
            }

            Map<String, Integer> methodArities = new HashMap<>();
            collectBehaviorArities(service.behaviors(), methodArities);
            for (var method : service.methods()) {
                if (method != null && method.name() != null && !method.name().isBlank()) {
                    methodArities.put(method.name(), method.parameters() != null ? method.parameters().size() : 0);
                }
            }

            registerReceiverMethodArities(service.name(), methodArities);
            registerReceiverMethodArities(decapitalize(service.name()), methodArities);
        }
    }

    private void registerReceiverMethodArities(String receiverName, Map<String, Integer> methods) {
        if (receiverName == null || receiverName.isBlank()) {
            return;
        }
        boundedContextReceiverMethodArities.put(receiverName, Map.copyOf(methods));
    }

    private Map<String, Map<String, Integer>> copyReceiverMethodIndex() {
        Map<String, Map<String, Integer>> copy = new HashMap<>();
        for (var entry : boundedContextReceiverMethodArities.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    private Map<String, Integer> resolveReceiverMethods(String receiverName) {
        if (receiverName == null || receiverName.isBlank()) {
            return null;
        }

        Map<String, Integer> methods = currentOwner.receiverMethodArities().get(receiverName);
        if (methods != null) {
            return methods;
        }

        String normalized = normalizeName(receiverName);
        for (var entry : currentOwner.receiverMethodArities().entrySet()) {
            if (normalizeName(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String decapitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toLowerCase();
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private boolean requiresTargetResolution(ThenClause.ThenStatement.ThenStatementType type) {
        return type == ThenClause.ThenStatement.ThenStatementType.SET
                || type == ThenClause.ThenStatement.ThenStatementType.CHANGE
                || type == ThenClause.ThenStatement.ThenStatementType.RECORD
                || type == ThenClause.ThenStatement.ThenStatementType.ADD
                || type == ThenClause.ThenStatement.ThenStatementType.REMOVE;
    }
}
