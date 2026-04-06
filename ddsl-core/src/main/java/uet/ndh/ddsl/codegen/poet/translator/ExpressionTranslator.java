package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.*;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;
import uet.ndh.ddsl.ast.behavior.clause.*;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.expr.*;
import uet.ndh.ddsl.ast.member.InvariantDecl;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.visitor.BaseAstVisitor;
import uet.ndh.ddsl.codegen.poet.TypeMapper;

import javax.lang.model.element.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Translates DDSL expressions and behaviors to JavaPoet CodeBlocks.
 * 
 * This is the core expression visitor that handles:
 * - Collection operations → Java Stream API
 * - Temporal expressions → java.time API
 * - Behavior clauses → Method bodies
 * - Invariant validation → Guard clauses
 * 
 * Uses the Visitor pattern (AstVisitor<CodeBlock>) for type-safe code generation.
 * 
 * Example transformations:
 * <pre>
 * DDSL: items where status is Active
 * Java: items.stream().filter(item -> item.getStatus() == Status.ACTIVE).collect(Collectors.toList())
 * 
 * DDSL: sum of orders total amounts where status is Confirmed
 * Java: orders.stream().filter(o -> o.getStatus() == Status.CONFIRMED).mapToDouble(Order::getTotalAmount).sum()
 * 
 * DDSL: require that: items is not empty
 * Java: if (items.isEmpty()) { throw new IllegalArgumentException("..."); }
 * </pre>
 */
public class ExpressionTranslator extends BaseAstVisitor<CodeBlock> {
    
    private final TypeMapper typeMapper;
    private boolean guardUnresolvedResultTarget;
    private Set<String> declaredFieldTargets = Set.of();
    private Map<String, TypeName> currentBehaviorParamTypes = Map.of();
    
    // Common type references
    private static final ClassName ILLEGAL_ARGUMENT = ClassName.get(IllegalArgumentException.class);
    private static final ClassName COLLECTORS = ClassName.get(Collectors.class);
    private static final ClassName INSTANT = ClassName.get(Instant.class);
    private static final ClassName CHRONO_UNIT = ClassName.get(ChronoUnit.class);
    private static final ClassName BIG_DECIMAL = ClassName.get(BigDecimal.class);
    
    public ExpressionTranslator(TypeMapper typeMapper) {
        this.typeMapper = typeMapper;
        this.guardUnresolvedResultTarget = false;
    }

    /**
     * Enable defensive guard for unresolved `result` assignment targets.
     */
    public ExpressionTranslator withUnresolvedResultGuard(Set<String> fieldTargets) {
        this.guardUnresolvedResultTarget = true;
        this.declaredFieldTargets = fieldTargets != null ? Set.copyOf(fieldTargets) : Set.of();
        return this;
    }

    /**
     * Disable defensive guard for unresolved `result` assignment targets.
     */
    public ExpressionTranslator clearUnresolvedResultGuard() {
        this.guardUnresolvedResultTarget = false;
        this.declaredFieldTargets = Collections.emptySet();
        return this;
    }
    
    /**
     * Implementation of visitTypeRef from AstVisitor interface.
     * Returns null as TypeRef nodes are not directly translated to CodeBlock.
     */
    @Override
    public CodeBlock visitTypeRef(TypeRef typeRef) {
        // TypeRef is handled by TypeMapper, not translated to CodeBlock directly
        return null;
    }
    
    /**
     * Translate a BehaviorDecl into a complete MethodSpec.
     */
    public MethodSpec translateBehavior(BehaviorDecl behavior) {
        // behavior.getName() already returns proper camelCase via NaturalLanguagePhrase.toMethodName()
        String methodName = behavior.getName();
        TypeName returnType = determineReturnType(behavior);
        
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .returns(returnType);
        
        // Add Javadoc from behavior description
        if (behavior.getName() != null) {
            builder.addJavadoc("Behavior: $L\n", behavior.getName());
        }
        
        Map<String, TypeName> paramTypes = new HashMap<>();

        // Add parameters with domain type resolution
        for (ParameterDecl param : behavior.parameters()) {
            TypeName paramType = typeMapper.mapType(param.type());
            // If the param type is Object (untyped in DDSL), try to resolve from domain types
            if ("Object".equals(param.type().name())) {
                TypeName resolved = typeMapper.tryResolveParamType(param.name());
                if (resolved != null) {
                    paramType = resolved;
                }
            }
            builder.addParameter(paramType, param.name());
            paramTypes.put(param.name(), paramType);
        }

        currentBehaviorParamTypes = Map.copyOf(paramTypes);
        
        // Generate method body
        builder.addCode(translateBehaviorBody(behavior));

        currentBehaviorParamTypes = Map.of();
        
        return builder.build();
    }
    
    /**
     * Translate the complete behavior body from all clauses.
     */
    private CodeBlock translateBehaviorBody(BehaviorDecl behavior) {
        CodeBlock.Builder code = CodeBlock.builder();
        
        // 1. Generate guard clauses from require
        if (behavior.requireClause() != null) {
            code.add(translateRequireClause(behavior.requireClause()));
        }

        // 1b. Generate validation-accumulation blocks
        if (behavior.errorAccumulationClause() != null) {
            code.add(translateErrorAccumulationClause(behavior.errorAccumulationClause()));
        }
        
        // 2. Generate local variables from given
        if (behavior.givenClause() != null) {
            code.add(translateGivenClause(behavior.givenClause()));
        }
        
        // 3. Generate statements from then
        for (ThenClause thenClause : behavior.thenClauses()) {
            code.add(translateThenClause(thenClause));
        }
        
        // 4. Generate event emission from emit
        if (behavior.emitClause() != null) {
            code.add(translateEmitClause(behavior.emitClause()));
        }
        
        // 5. Generate return statement
        if (behavior.returnClause() != null) {
            code.add(translateReturnClause(behavior.returnClause()));
        }
        
        return code.build();
    }

    /**
     * Translate error accumulation clause to ValidationResult pattern.
     */
    private CodeBlock translateErrorAccumulationClause(ErrorAccumulationClause clause) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("// Error accumulation\n");
        
        String validationsPkg = typeMapper.getBasePackage() + ".validation";
        ClassName validationResult = ClassName.get(validationsPkg, "ValidationResult");
        ClassName validationException = ClassName.get(validationsPkg, "ValidationException");
        ClassName validationError = ClassName.get(validationsPkg, "ValidationError");
        ClassName groupedValidationResult = ClassName.get(validationsPkg, "GroupedValidationResult");
        ClassName arrayList = ClassName.get("java.util", "ArrayList");
        ClassName hashMap = ClassName.get("java.util", "HashMap");
        ClassName map = ClassName.get("java.util", "Map");
        TypeName errorListType = ParameterizedTypeName.get(ClassName.get(List.class), validationError);
        
        if (clause.type() == ErrorAccumulationClause.AccumulationType.COLLECT_BY_GROUP) {
            code.addStatement("$T<String, $T> errorGroups = new $T<>()", map, errorListType, hashMap);
            for (ErrorAccumulationClause.GroupedValidation group : clause.groupedValidations()) {
                String groupName = group.groupName();
                code.addStatement("$T groupErrors_$N = new $T<>()", errorListType, sanitizeIdentifier(groupName), arrayList);
                for (ErrorAccumulationClause.ValidationRule rule : group.validations()) {
                    CodeBlock condition = translateCondition(rule.condition());
                    String message = rule.message() != null ? rule.message() : "Validation failed";
                    code.beginControlFlow("if (!($L))", condition)
                        .addStatement("groupErrors_$N.add(new $T($S, $S))", sanitizeIdentifier(groupName), validationError, inferErrorField(rule.condition()), message)
                        .endControlFlow();
                }
                code.addStatement("errorGroups.put($S, groupErrors_$N)", groupName, sanitizeIdentifier(groupName));
            }
            code.addStatement("$T result = $T.of(errorGroups)", groupedValidationResult, groupedValidationResult);
            if (clause.returnType() == ErrorAccumulationClause.ReturnErrorsType.FAIL_IF_ANY_ERRORS
                || clause.returnType() == ErrorAccumulationClause.ReturnErrorsType.FAIL_IF_CRITICAL) {
                code.addStatement("result.throwIfInvalid()");
            }
        } else {
            code.addStatement("$T errors = new $T<>()", errorListType, arrayList);
            List<ErrorAccumulationClause.ValidationRule> rules = clause.validationRules();
            for (ErrorAccumulationClause.ValidationRule rule : rules) {
                CodeBlock condition = translateCondition(rule.condition());
                String message = rule.message() != null ? rule.message() : "Validation failed";
                code.beginControlFlow("if (!($L))", condition)
                    .addStatement("errors.add(new $T($S, $S))", validationError, inferErrorField(rule.condition()), message)
                    .endControlFlow();
            }
            code.addStatement("$T result = $T.ofErrors(errors)", validationResult, validationResult);

            if (clause.returnType() == ErrorAccumulationClause.ReturnErrorsType.FAIL_IF_ANY_ERRORS
                || clause.returnType() == ErrorAccumulationClause.ReturnErrorsType.FAIL_IF_CRITICAL) {
                code.beginControlFlow("if (result.hasErrors())")
                    .addStatement("throw new $T(result.getErrors())", validationException)
                    .endControlFlow();
            } else if (clause.returnType() == ErrorAccumulationClause.ReturnErrorsType.RETURN_ALL_ERRORS
                || clause.returnType() == ErrorAccumulationClause.ReturnErrorsType.RETURN_ERRORS_IF_ANY) {
                code.addStatement("return result");
            }
        }
        
        return code.build();
    }

    private String inferErrorField(NaturalLanguageCondition condition) {
        if (condition == null) {
            return "unknown";
        }
        if (condition.leftExpression() instanceof VariableExpr var && var.name() != null && !var.name().isBlank()) {
            return var.name();
        }
        if (condition.quantifierTarget() != null && !condition.quantifierTarget().isBlank()) {
            return condition.quantifierTarget();
        }
        if (condition.propertyPath() != null && !condition.propertyPath().isBlank()) {
            return condition.propertyPath();
        }
        return "unknown";
    }

    private String sanitizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "group";
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9_]", "_");
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "g_" + sanitized;
        }
        return sanitized;
    }
    
    /**
     * Translate invariant to validation code.
     */
    public CodeBlock translateInvariantValidation(InvariantDecl invariant) {
        CodeBlock.Builder code = CodeBlock.builder();
        
        // Generate validation condition - invariant.condition() is Expr type
        CodeBlock condition = translateExpression(invariant.condition());
        String message = invariant.errorMessage() != null 
            ? invariant.errorMessage() 
            : "Invariant violated: " + invariant.name();
        
        code.beginControlFlow("if (!($L))", condition)
            .addStatement("throw new $T($S)", ILLEGAL_ARGUMENT, message)
            .endControlFlow();
        
        return code.build();
    }
    
    // ========== Clause Translation ==========
    
    /**
     * Translate require clause to guard statements.
     */
    private CodeBlock translateRequireClause(RequireClause requireClause) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("// Preconditions\n");
        
        for (RequireClause.RequireCondition condition : requireClause.conditions()) {
            CodeBlock guardCondition = translateGuardCondition(condition.condition());
            String errorMessage = condition.errorMessage() != null 
                ? condition.errorMessage() 
                : "Precondition failed: " + condition.condition().rawText();
            
            code.beginControlFlow("if ($L)", guardCondition)
                .addStatement("throw new $T($S)", ILLEGAL_ARGUMENT, errorMessage)
                .endControlFlow();
        }
        code.add("\n");
        
        return code.build();
    }
    
    /**
     * Translate a condition into its NEGATED (guard/failure) form for precondition checks.
     * This avoids ugly double negation like if (!(!x.isEmpty())).
     * 
     * "checkIn is not empty" → guard fires when checkIn IS empty → checkIn.isEmpty()
     * "reservationStatus is PENDING" → guard fires when status != PENDING → !status.equals("PENDING")
     */
    private CodeBlock translateGuardCondition(NaturalLanguageCondition condition) {
        if (condition == null) {
            return CodeBlock.of("false");
        }
        
        return switch (condition.type()) {
            // "X is not empty" → guard: X.isEmpty()
            case IS_NOT_EMPTY -> CodeBlock.of("$N.isEmpty()", condition.quantifierTarget());
            // "X is empty" → guard: !X.isEmpty()
            case IS_EMPTY -> CodeBlock.of("!$N.isEmpty()", condition.quantifierTarget());
            
            // "X exists in system" → guard: !repo.existsById(...)
            case EXISTS_IN_SYSTEM -> CodeBlock.of("!$NRepository.existsById($NId)", 
                condition.quantifierTarget(), condition.quantifierTarget());
            // "X does not exist" → guard: repo.existsById(...)
            case DOES_NOT_EXIST -> CodeBlock.of("$NRepository.existsById($NId)", 
                condition.quantifierTarget(), condition.quantifierTarget());
            
            // "X has been Y" → guard: !X.hasBeenProcessed()
            case HAS_BEEN -> CodeBlock.of("!$N.hasBeenProcessed()", condition.quantifierTarget());
            
            // For comparison/state: negate the whole expression
            case COMPARISON -> {
                CodeBlock positive = translateComparisonCondition(condition);
                yield CodeBlock.of("!($L)", positive);
            }
            case STATE_IS -> {
                CodeBlock positive = translateStateCondition(condition);
                yield CodeBlock.of("!($L)", positive);
            }
            case STATE_ONE_OF -> {
                CodeBlock positive = translateStateOneOfCondition(condition);
                yield CodeBlock.of("!$L", positive);
            }
            
            // Quantifiers: negate
            case UNIVERSAL -> {
                CodeBlock positive = translateUniversalCondition(condition);
                yield CodeBlock.of("!$L", positive);
            }
            case EXISTENTIAL -> {
                CodeBlock positive = translateExistentialCondition(condition);
                yield CodeBlock.of("!$L", positive);
            }
            case NEGATED_EXISTENTIAL -> {
                CodeBlock positive = translateNegatedExistentialCondition(condition);
                yield CodeBlock.of("!$L", positive);
            }
            
            // Fallback: negate the positive form
            default -> {
                CodeBlock positive = translateCondition(condition);
                yield CodeBlock.of("!($L)", positive);
            }
        };
    }
    
    /**
     * Translate given clause to local variable declarations.
     */
    private CodeBlock translateGivenClause(GivenClause givenClause) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("// Local variables\n");
        
        for (GivenClause.GivenStatement statement : givenClause.statements()) {
            CodeBlock valueCode = translateExpression(statement.expression());
            
            // Use var for type inference
            code.addStatement("var $N = $L", statement.identifier(), valueCode);
        }
        code.add("\n");
        
        return code.build();
    }
    
    /**
     * Translate then clause to mutation statements.
     */
    private CodeBlock translateThenClause(ThenClause thenClause) {
        CodeBlock.Builder code = CodeBlock.builder();
        
        for (ThenClause.ThenStatement statement : thenClause.statements()) {
            code.add(translateThenStatement(statement));
        }
        
        return code.build();
    }
    
    /**
     * Translate a single then statement.
     */
    private CodeBlock translateThenStatement(ThenClause.ThenStatement statement) {
        return switch (statement.type()) {
            case SET -> translateSetStatement(statement);
            case CHANGE -> translateSetStatement(statement); // Same as SET
            case RECORD -> translateRecordStatement(statement);
            case CALCULATE -> translateCalculateStatement(statement);
            case ADD -> translateAddStatement(statement);
            case REMOVE -> translateRemoveStatement(statement);
            case ENABLE -> CodeBlock.of("this.$N = true;\n", statement.target());
            case DISABLE -> CodeBlock.of("this.$N = false;\n", statement.target());
            case IF -> translateIfStatement(statement);
            case FOR_EACH -> translateForEachStatement(statement);
            case METHOD_CALL -> translateInvokeStatement(statement);
            default -> CodeBlock.of("// TODO: $L\n", statement.type());
        };
    }
    
    private CodeBlock translateSetStatement(ThenClause.ThenStatement statement) {
        if (guardUnresolvedResultTarget
                && "result".equals(statement.target())
                && !declaredFieldTargets.contains("result")) {
            return CodeBlock.of("// WARN: unresolved target 'result'\n");
        }

        CodeBlock value = translateExpression(statement.expression());
        return CodeBlock.of("this.$N = $L;\n", statement.target(), value);
    }
    
    private CodeBlock translateRecordStatement(ThenClause.ThenStatement statement) {
        CodeBlock value = translateExpression(statement.expression());
        return CodeBlock.of("this.$N = $L;\n", statement.target(), value);
    }
    
    private CodeBlock translateCalculateStatement(ThenClause.ThenStatement statement) {
        CodeBlock value = translateExpression(statement.expression());
        return CodeBlock.of("var $N = $L;\n", statement.target(), value);
    }
    
    private CodeBlock translateAddStatement(ThenClause.ThenStatement statement) {
        CodeBlock value = translateExpression(statement.expression());
        return CodeBlock.of("this.$N.add($L);\n", statement.target(), value);
    }
    
    private CodeBlock translateRemoveStatement(ThenClause.ThenStatement statement) {
        CodeBlock value = translateExpression(statement.expression());
        return CodeBlock.of("this.$N.remove($L);\n", statement.target(), value);
    }
    
    private CodeBlock translateIfStatement(ThenClause.ThenStatement statement) {
        CodeBlock.Builder code = CodeBlock.builder();
        CodeBlock condition = translateCondition(statement.condition());
        
        code.beginControlFlow("if ($L)", condition);
        for (ThenClause.ThenStatement nested : statement.nestedStatements()) {
            code.add(translateThenStatement(nested));
        }
        
        // Handle else-if branches
        for (ThenClause.ThenStatement.ElseIfBranch elseIf : statement.elseIfBranches()) {
            CodeBlock elseIfCondition = translateCondition(elseIf.condition());
            code.nextControlFlow("else if ($L)", elseIfCondition);
            for (ThenClause.ThenStatement nested : elseIf.statements()) {
                code.add(translateThenStatement(nested));
            }
        }
        
        // Handle else
        if (!statement.elseStatements().isEmpty()) {
            code.nextControlFlow("else");
            for (ThenClause.ThenStatement nested : statement.elseStatements()) {
                code.add(translateThenStatement(nested));
            }
        }
        
        code.endControlFlow();
        return code.build();
    }
    
    private CodeBlock translateForEachStatement(ThenClause.ThenStatement statement) {
        CodeBlock.Builder code = CodeBlock.builder();
        String loopVar = statement.loopVariable() != null ? statement.loopVariable() : "item";
        CodeBlock collection = translateExpression(statement.expression());
        
        code.beginControlFlow("for (var $N : $L)", loopVar, collection);
        for (ThenClause.ThenStatement nested : statement.nestedStatements()) {
            code.add(translateThenStatement(nested));
        }
        code.endControlFlow();
        
        return code.build();
    }
    
    private CodeBlock translateInvokeStatement(ThenClause.ThenStatement statement) {
        // Legacy AST fallback for parsed forms like: save order to orderRepository
        // (older parser versions represented repository in expression and entity in target)
        if (statement.expression() instanceof VariableExpr repositoryVar
                && statement.target() != null
                && !statement.target().isBlank()) {
            return CodeBlock.of("$N.save($N);\n", repositoryVar.name(), statement.target());
        }

        if (statement.expression() == null) {
            if (statement.target() != null && !statement.target().isBlank()) {
                return CodeBlock.of("$N();\n", statement.target());
            }
            return CodeBlock.of("// TODO: unresolved method call\n");
        }

        CodeBlock expression = translateExpression(statement.expression());
        return CodeBlock.of("$L;\n", expression);
    }
    
    /**
     * Translate emit clause to event registration.
     */
    private CodeBlock translateEmitClause(EmitClause emitClause) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("// Emit domain events\n");
        
        // EmitClause has eventName and eventArguments (simple strings)
        String eventType = emitClause.eventName();
        ClassName eventClass = typeMapper.resolveDomainClassName(eventType);
        
        // Build event constructor arguments from argument names
        if (!emitClause.eventArguments().isEmpty()) {
            String args = String.join(", ", emitClause.eventArguments());
            code.addStatement("registerEvent($T.now($L))", eventClass, args);
        } else {
            code.addStatement("registerEvent($T.now())", eventClass);
        }
        code.add("\n");
        
        return code.build();
    }
    
    /**
     * Translate return clause.
     */
    private CodeBlock translateReturnClause(ReturnClause returnClause) {
        if (returnClause.expression() != null) {
            CodeBlock value = translateExpression(returnClause.expression());
            return CodeBlock.of("return $L;\n", value);
        }
        return CodeBlock.of("return;\n");
    }
    
    // ========== Expression Translation (Visitor Methods) ==========
    
    /**
     * Main entry point for translating expressions.
     */
    public CodeBlock translateExpression(Expr expr) {
        if (expr == null) {
            return CodeBlock.of("null");
        }
        
        return switch (expr) {
            case LiteralExpr lit -> visitLiteralExpr(lit);
            case VariableExpr var -> visitVariableExpr(var);
            case BinaryExpr bin -> visitBinaryExpr(bin);
            case UnaryExpr unary -> visitUnaryExpr(unary);
            case MethodCallExpr call -> visitMethodCallExpr(call);
            case FieldAccessExpr access -> visitFieldAccessExpr(access);
            case NewInstanceExpr newInst -> visitNewInstanceExpr(newInst);
            case ListExpr list -> visitListExpr(list);
            case TernaryExpr ternary -> visitTernaryExpr(ternary);
            case ThisExpr thisExpr -> {
                Objects.requireNonNull(thisExpr);
                yield CodeBlock.of("this");
            }
            case NullExpr nullExpr -> {
                Objects.requireNonNull(nullExpr);
                yield CodeBlock.of("null");
            }
            case CollectionFilter filter -> visitCollectionFilter(filter);
            case CollectionAggregation agg -> visitCollectionAggregation(agg);
            case CollectionGroupBy groupBy -> visitCollectionGroupBy(groupBy);
            case CollectionFlatten flatten -> visitCollectionFlatten(flatten);
            case MatchExpr match -> visitMatch(match);
            case StringCondition strCond -> visitStringCondition(strCond);
            case StringOperation strOp -> visitStringOperation(strOp);
            case SpecificationCondition specCond -> visitSpecificationCondition(specCond);
            case TemporalComparison temporalComparison -> visitTemporalComparison(temporalComparison);
            case TemporalRange temporalRange -> visitTemporalRange(temporalRange);
            case TemporalRelative temporalRelative -> visitTemporalRelative(temporalRelative);
            case TemporalSequence temporalSequence -> visitTemporalSequence(temporalSequence);
            default -> CodeBlock.of("/* unsupported: $L */", expr.getClass().getSimpleName());
        };
    }
    
    @Override
    public CodeBlock visitLiteralExpr(LiteralExpr expr) {
        Object value = expr.value();
        if (value instanceof String) {
            return CodeBlock.of("$S", value);
        } else if (value instanceof Boolean) {
            return CodeBlock.of("$L", value);
        } else if (value instanceof Number) {
            return CodeBlock.of("$L", value);
        }
        return CodeBlock.of("$L", value);
    }
    
    @Override
    public CodeBlock visitVariableExpr(VariableExpr expr) {
        return CodeBlock.of("$N", expr.name());
    }
    
    @Override
    public CodeBlock visitBinaryExpr(BinaryExpr expr) {
        CodeBlock left = translateExpression(expr.left());
        CodeBlock right = translateExpression(expr.right());
        String operator = mapBinaryOperator(expr.operator());
        return CodeBlock.of("($L $L $L)", left, operator, right);
    }
    
    @Override
    public CodeBlock visitUnaryExpr(UnaryExpr expr) {
        CodeBlock operand = translateExpression(expr.operand());
        String operator = mapUnaryOperator(expr.operator());
        return CodeBlock.of("$L$L", operator, operand);
    }
    
    @Override
    public CodeBlock visitMethodCallExpr(MethodCallExpr expr) {
        CodeBlock.Builder code = CodeBlock.builder();
        
        if (expr.receiver() != null) {
            CodeBlock receiver = translateExpression(expr.receiver());
            code.add("$L.", receiver);
        }
        
        code.add("$N(", expr.methodName());
        
        for (int i = 0; i < expr.arguments().size(); i++) {
            if (i > 0) code.add(", ");
            code.add(translateExpression(expr.arguments().get(i)));
        }
        
        code.add(")");
        return code.build();
    }
    
    @Override
    public CodeBlock visitFieldAccessExpr(FieldAccessExpr expr) {
        CodeBlock object = translateExpression(expr.object());
        // Use getter method for field access
        String getter = "get" + capitalize(expr.fieldName());
        return CodeBlock.of("$L.$N()", object, getter);
    }
    
    @Override
    public CodeBlock visitNewInstanceExpr(NewInstanceExpr expr) {
        TypeName type = typeMapper.mapType(expr.type());
        
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("new $T(", type);
        
        for (int i = 0; i < expr.arguments().size(); i++) {
            if (i > 0) code.add(", ");
            code.add(translateExpression(expr.arguments().get(i)));
        }
        
        code.add(")");
        return code.build();
    }
    
    @Override
    public CodeBlock visitListExpr(ListExpr expr) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("$T.of(", ClassName.get(List.class));
        
        for (int i = 0; i < expr.elements().size(); i++) {
            if (i > 0) code.add(", ");
            code.add(translateExpression(expr.elements().get(i)));
        }
        
        code.add(")");
        return code.build();
    }
    
    @Override
    public CodeBlock visitTernaryExpr(TernaryExpr expr) {
        CodeBlock condition = translateExpression(expr.condition());
        CodeBlock thenExpr = translateExpression(expr.thenExpr());
        CodeBlock elseExpr = translateExpression(expr.elseExpr());
        return CodeBlock.of("($L ? $L : $L)", condition, thenExpr, elseExpr);
    }
    
    // ========== Collection Operations → Stream API ==========
    
    /**
     * Translate collection filter to Stream.filter().
     * 
     * Example:
     *   items where status is Active
     *   → items.stream().filter(item -> item.getStatus() == Status.ACTIVE).collect(Collectors.toList())
     */
    public CodeBlock visitCollectionFilter(CollectionFilter filter) {
        CodeBlock collection = translateExpression(filter.collection());
        
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("$L.stream()", collection);
        
        // Add filter for each where condition
        for (NaturalLanguageCondition where : filter.whereConditions()) {
            CodeBlock predicate = translateConditionAsLambda(where, "item");
            code.add("\n    .filter($L)", predicate);
        }
        
        // Handle "of their" property extraction
        if (filter.ofTheirProperty() != null) {
            String getter = "get" + capitalize(filter.ofTheirProperty());
            code.add("\n    .map(item -> item.$N())", getter);
        }
        
        code.add("\n    .collect($T.toList())", COLLECTORS);
        
        return code.build();
    }
    
    /**
     * Translate collection aggregation to Stream operations.
     * 
     * Example:
     *   sum of orders total amounts where status is Confirmed
     *   → orders.stream().filter(o -> o.getStatus() == Status.CONFIRMED)
     *                    .mapToDouble(Order::getTotalAmount).sum()
     */
    public CodeBlock visitCollectionAggregation(CollectionAggregation agg) {
        CodeBlock collection = translateExpression(agg.collection());
        
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("$L.stream()", collection);
        
        // Add filter if where condition exists
        if (agg.whereCondition() != null) {
            CodeBlock predicate = translateConditionAsLambda(agg.whereCondition(), "item");
            code.add("\n    .filter($L)", predicate);
        }
        
        // Generate aggregation based on type
        switch (agg.aggregationType()) {
            case COUNT -> code.add("\n    .count()");
            
            case SUM -> {
                String getter = "get" + capitalize(agg.propertyPath());
                code.add("\n    .mapToDouble(item -> item.$N())", getter);
                code.add("\n    .sum()");
            }
            
            case MAXIMUM -> {
                String getter = "get" + capitalize(agg.propertyPath());
                code.add("\n    .mapToDouble(item -> item.$N())", getter);
                code.add("\n    .max()");
                code.add("\n    .orElse(0.0)");
            }
            
            case MINIMUM -> {
                String getter = "get" + capitalize(agg.propertyPath());
                code.add("\n    .mapToDouble(item -> item.$N())", getter);
                code.add("\n    .min()");
                code.add("\n    .orElse(0.0)");
            }
            
            case AVERAGE -> {
                String getter = "get" + capitalize(agg.propertyPath());
                code.add("\n    .mapToDouble(item -> item.$N())", getter);
                code.add("\n    .average()");
                code.add("\n    .orElse(0.0)");
            }
        }
        
        return code.build();
    }
    
    /**
     * Translate collection group by to Stream.collect(Collectors.groupingBy()).
     */
    public CodeBlock visitCollectionGroupBy(CollectionGroupBy groupBy) {
        if (groupBy.collection() instanceof CollectionAggregation agg
            && agg.aggregationType() == CollectionAggregation.AggregationType.SUM
            && agg.propertyPath() != null && !agg.propertyPath().isBlank()) {
            CodeBlock baseCollection = translateExpression(agg.collection());
            String groupGetter = "get" + capitalize(groupBy.groupByProperty());
            String valueGetter = "get" + capitalize(agg.propertyPath());
            return CodeBlock.of(
                "$L.stream()\n    .collect($T.groupingBy(item -> item.$N(), $T.summingDouble(item -> item.$N())))",
                baseCollection, COLLECTORS, groupGetter, COLLECTORS, valueGetter
            );
        }

        CodeBlock collection = translateExpression(groupBy.collection());
        String keyGetter = "get" + capitalize(groupBy.groupByProperty());
        
        return CodeBlock.of(
            "$L.stream()\n    .collect($T.groupingBy(item -> item.$N()))",
            collection, COLLECTORS, keyGetter
        );
    }
    
    /**
     * Translate collection flatten to Stream.flatMap().
     */
    public CodeBlock visitCollectionFlatten(CollectionFlatten flatten) {
        CodeBlock collection = translateExpression(flatten.collection());
        String nestedGetter = "get" + capitalize(flatten.propertyPath());
        
        return CodeBlock.of(
            "$L.stream()\n    .flatMap(item -> item.$N().stream())\n    .collect($T.toList())",
            collection, nestedGetter, COLLECTORS
        );
    }
    
    /**
     * Translate match expression to switch expression.
     */
    public CodeBlock visitMatch(MatchExpr match) {
        CodeBlock subject = translateExpression(match.matchTarget());
        TypeName subjectType = resolveExpressionType(match.matchTarget());
        boolean stringSubject = isStringType(subjectType);
        
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("switch ($L) {\n", subject);
        
        for (MatchExpr.MatchCase matchCase : match.cases()) {
            String casePattern = renderMatchCasePattern(matchCase.pattern(), stringSubject);
            CodeBlock bodyCode = translateMatchCaseBody(matchCase.body());
            if (matchCase.hasGuard()) {
                CodeBlock guard = translateCondition(matchCase.guard());
                code.add("    case $L when $L -> $L;\n", casePattern, guard, bodyCode);
            } else {
                code.add("    case $L -> $L;\n", casePattern, bodyCode);
            }
        }
        
        if (match.defaultCase() != null) {
            CodeBlock defaultBody = translateMatchCaseBody(match.defaultCase().body());
            code.add("    default -> $L;\n", defaultBody);
        }
        
        code.add("}");
        return code.build();
    }

    private String renderMatchCasePattern(MatchExpr.CasePattern pattern, boolean stringSubject) {
        if (pattern instanceof MatchExpr.CasePattern.SingleValue single) {
            return renderSingleValuePattern(single, stringSubject);
        }
        if (pattern instanceof MatchExpr.CasePattern.MultipleValues many) {
            return many.values().stream()
                .map(value -> renderRawCaseValue(value, stringSubject))
                .collect(Collectors.joining(", "));
        }
        if (pattern instanceof MatchExpr.CasePattern.NullValue) {
            return "null";
        }
        return "default";
    }

    private String renderSingleValuePattern(MatchExpr.CasePattern.SingleValue single, boolean stringSubject) {
        if (single.expression() != null) {
            return translateExpression(single.expression()).toString();
        }
        if (single.value() == null) {
            return "null";
        }
        return renderRawCaseValue(single.value(), stringSubject);
    }

    private String renderRawCaseValue(String value, boolean stringSubject) {
        if (value == null || value.isBlank()) {
            return "null";
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed;
        }
        if (trimmed.matches("-?\\d+(\\.\\d+)?")) {
            return trimmed;
        }
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return trimmed.toLowerCase();
        }

        if (stringSubject) {
            return "\"" + escapeJavaString(trimmed) + "\"";
        }

        // Heuristic: TitleCase tokens in DSL usually denote enum constants.
        if (Character.isUpperCase(trimmed.charAt(0))) {
            return trimmed.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        }

        // Fallback to a String literal case label.
        return "\"" + trimmed + "\"";
    }

    private TypeName resolveExpressionType(Expr expr) {
        if (expr instanceof VariableExpr var) {
            TypeName paramType = currentBehaviorParamTypes.get(var.name());
            if (paramType != null) {
                return paramType;
            }
            return typeMapper.tryResolveParamType(var.name());
        }
        if (expr instanceof FieldAccessExpr access && access.fieldName() != null) {
            return typeMapper.tryResolveParamType(access.fieldName());
        }
        if (expr instanceof LiteralExpr literal && literal.value() instanceof String) {
            return ClassName.get(String.class);
        }
        return null;
    }

    private boolean isStringType(TypeName typeName) {
        return typeName != null && typeName.equals(ClassName.get(String.class));
    }

    private String escapeJavaString(String raw) {
        return raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
    
    /**
     * Translate a MatchCaseBody to CodeBlock.
     */
    private CodeBlock translateMatchCaseBody(MatchExpr.MatchCaseBody body) {
        return switch (body) {
            case MatchExpr.MatchCaseBody.ExpressionBody exprBody -> 
                translateExpression(exprBody.expression());
            case MatchExpr.MatchCaseBody.StatementBody stmtBody ->
                CodeBlock.of("/* statement: $L */", stmtBody.statement().getClass().getSimpleName());
            case MatchExpr.MatchCaseBody.BlockBody blockBody ->
                CodeBlock.of("/* block statements: $L */", blockBody.statements().size());
        };
    }

    private CodeBlock visitTemporalComparison(TemporalComparison expr) {
        CodeBlock left = translateExpression(expr.dateTimeExpr());
        CodeBlock anchor = translateTemporalAnchor(expr.anchor());
        ClassName helpers = ClassName.get(typeMapper.getBasePackage() + ".temporal", "TemporalPredicates");
        
        return switch (expr.operator()) {
            case IS_BEFORE -> CodeBlock.of("$T.isBefore($L, $L)", helpers, left, anchor);
            case IS_AFTER -> CodeBlock.of("$T.isAfter($L, $L)", helpers, left, anchor);
            case IS_ON -> CodeBlock.of("$L.equals($L)", left, anchor);
            case IS_NOT_BEFORE -> CodeBlock.of("!$T.isBefore($L, $L)", helpers, left, anchor);
            case IS_NOT_AFTER -> CodeBlock.of("!$T.isAfter($L, $L)", helpers, left, anchor);
        };
    }

    private CodeBlock visitTemporalRange(TemporalRange expr) {
        CodeBlock dateExpr = translateExpression(expr.dateTimeExpr());
        ClassName helpers = ClassName.get(typeMapper.getBasePackage() + ".temporal", "TemporalPredicates");

        return switch (expr.rangeType()) {
            case WITHIN -> {
                CodeBlock duration = translateDuration(expr.duration());
                yield CodeBlock.of("$T.isWithinNext($L, $L)", helpers, dateExpr, duration);
            }
            case WITHIN_NEXT -> {
                CodeBlock duration = translateDuration(expr.duration());
                yield CodeBlock.of("$T.isWithinNext($L, $L)", helpers, dateExpr, duration);
            }
            case WITHIN_LAST -> {
                CodeBlock duration = translateDuration(expr.duration());
                yield CodeBlock.of("$T.isWithinLast($L, $L)", helpers, dateExpr, duration);
            }
            case BETWEEN -> {
                CodeBlock start = expr.startExpr() != null ? translateExpression(expr.startExpr()) : CodeBlock.of("$T.MIN", INSTANT);
                CodeBlock end = expr.endExpr() != null ? translateExpression(expr.endExpr()) : CodeBlock.of("$T.MAX", INSTANT);
                yield CodeBlock.of("$T.isBetween($L, $L, $L)", helpers, dateExpr, start, end);
            }
        };
    }

    private CodeBlock visitTemporalRelative(TemporalRelative expr) {
        CodeBlock left = translateExpression(expr.dateTimeExpr());
        CodeBlock duration = translateDuration(expr.duration());
        ClassName helpers = ClassName.get(typeMapper.getBasePackage() + ".temporal", "TemporalPredicates");
        
        if (expr.direction() == TemporalRelative.RelativeDirection.AGO) {
             return switch (expr.operator()) {
                 case MORE_THAN -> CodeBlock.of("$T.isMoreThanAgo($L, $L)", helpers, left, duration);
                 case LESS_THAN -> CodeBlock.of("$T.isLessThanAgo($L, $L)", helpers, left, duration);
                 case AT_LEAST -> CodeBlock.of("!$T.isLessThanAgo($L, $L)", helpers, left, duration); 
                 case AT_MOST -> CodeBlock.of("!$T.isMoreThanAgo($L, $L)", helpers, left, duration);
                 case EXACTLY -> CodeBlock.of("$L.equals($T.now().minus($L))", left, INSTANT, duration);
             };
        } else {
             CodeBlock base = CodeBlock.of("$T.now().plus($L)", INSTANT, duration);
             return switch (expr.operator()) {
                case MORE_THAN -> CodeBlock.of("$T.isAfter($L, $L)", helpers, left, base);
                case LESS_THAN -> CodeBlock.of("$T.isBefore($L, $L)", helpers, left, base);
                case AT_LEAST -> CodeBlock.of("!$T.isBefore($L, $L)", helpers, left, base);
                case AT_MOST -> CodeBlock.of("!$T.isAfter($L, $L)", helpers, left, base);
                case EXACTLY -> CodeBlock.of("$L.equals($L)", left, base);
             };
        }
    }

    private CodeBlock visitTemporalSequence(TemporalSequence expr) {
        CodeBlock left = translateExpression(expr.dateTimeExpr());
        CodeBlock right = translateExpression(expr.otherDateTimeExpr());
        ClassName helpers = ClassName.get(typeMapper.getBasePackage() + ".temporal", "TemporalPredicates");

        return switch (expr.operator()) {
            case IS_BEFORE, OCCURRED_BEFORE -> CodeBlock.of("$T.isBefore($L, $L)", helpers, left, right);
            case IS_AFTER, OCCURRED_AFTER -> CodeBlock.of("$T.isAfter($L, $L)", helpers, left, right);
        };
    }

    private CodeBlock translateTemporalAnchor(uet.ndh.ddsl.ast.expr.temporal.TemporalAnchor anchor) {
        if (anchor == null) {
            return CodeBlock.of("$T.now()", INSTANT);
        }
        return switch (anchor.type()) {
            case NOW -> CodeBlock.of("$T.now()", INSTANT);
            case TODAY -> CodeBlock.of("$T.now()", INSTANT);
            case YESTERDAY -> CodeBlock.of("$T.now().minus(1, $T.DAYS)", INSTANT, CHRONO_UNIT);
            case TOMORROW -> CodeBlock.of("$T.now().plus(1, $T.DAYS)", INSTANT, CHRONO_UNIT);
            case AGO -> CodeBlock.of("$T.now().minus($L)", INSTANT, translateDuration(anchor.relativeDuration()));
            case FROM_NOW -> CodeBlock.of("$T.now().plus($L)", INSTANT, translateDuration(anchor.relativeDuration()));
            case EXPRESSION -> translateExpression(anchor.expression());
            default -> CodeBlock.of("$T.now()", INSTANT);
        };
    }

    private CodeBlock translateDuration(uet.ndh.ddsl.ast.expr.temporal.Duration duration) {
        if (duration == null) {
            return CodeBlock.of("0, $T.SECONDS", CHRONO_UNIT);
        }

        String unit = duration.unit().toChronoUnit();
        return CodeBlock.of("$L, $T.$L", duration.amount(), CHRONO_UNIT, unit);
    }
    
    // ========== Condition Translation ==========
    
    /**
     * Translate a natural language condition to a CodeBlock.
     */
    public CodeBlock translateCondition(NaturalLanguageCondition condition) {
        if (condition == null) {
            return CodeBlock.of("true");
        }
        
        return switch (condition.type()) {
            case IS_EMPTY -> CodeBlock.of("$N.isEmpty()", condition.quantifierTarget());
            case IS_NOT_EMPTY -> CodeBlock.of("!$N.isEmpty()", condition.quantifierTarget());
            
            case UNIVERSAL -> translateUniversalCondition(condition);
            case EXISTENTIAL -> translateExistentialCondition(condition);
            case NEGATED_EXISTENTIAL -> translateNegatedExistentialCondition(condition);
            
            case EXISTS_IN_SYSTEM -> CodeBlock.of("$NRepository.existsById($NId)", 
                condition.quantifierTarget(), condition.quantifierTarget());
            case DOES_NOT_EXIST -> CodeBlock.of("!$NRepository.existsById($NId)", 
                condition.quantifierTarget(), condition.quantifierTarget());
            
            case STATE_IS -> translateStateCondition(condition);
            case STATE_ONE_OF -> translateStateOneOfCondition(condition);
            case HAS_BEEN -> CodeBlock.of("$N.hasBeenProcessed()", condition.quantifierTarget());
            
            case COMPARISON -> translateComparisonCondition(condition);
            
            case SIMPLE -> translateSimpleCondition(condition);
            
            case CONTAINS -> {
                 CodeBlock left = translateExpression(condition.leftExpression());
                 CodeBlock right = translateExpression(condition.rightExpression());
                 yield CodeBlock.of("$L.contains($L)", left, right);
            }
            
            default -> CodeBlock.of("/* $L */ true", condition.rawText());
        };
    }
    
    /**
     * Translate a SIMPLE condition by analyzing the raw text as a best-effort fallback.
     * Tries common patterns before generating a placeholder.
     */
    private CodeBlock translateSimpleCondition(NaturalLanguageCondition condition) {
        String raw = condition.rawText();
        if (raw == null || raw.isBlank()) {
            return CodeBlock.of("true");
        }

        // Try built-in format validation phrase: "email is valid EMAIL format"
        if (raw.matches(".+ is valid [A-Z_ ]+ format")) {
            String[] sides = raw.split(" is valid ", 2);
            String targetName = sides[0].trim();
            String formatLabel = sides[1].replace(" format", "").trim().replace(' ', '_');
            try {
                StringCondition.FormatType formatType = StringCondition.FormatType.valueOf(formatLabel);
                return generateFormatValidation(CodeBlock.of("this.$N", targetName), formatType);
            } catch (IllegalArgumentException ignored) {
                // Fall through to other best-effort patterns
            }
        }

        // Try negative built-in format phrase: "email is not valid EMAIL format"
        if (raw.matches(".+ is not valid [A-Z_ ]+ format")) {
            String[] sides = raw.split(" is not valid ", 2);
            String targetName = sides[0].trim();
            String formatLabel = sides[1].replace(" format", "").trim().replace(' ', '_');
            try {
                StringCondition.FormatType formatType = StringCondition.FormatType.valueOf(formatLabel);
                return CodeBlock.of("!($L)", generateFormatValidation(CodeBlock.of("this.$N", targetName), formatType));
            } catch (IllegalArgumentException ignored) {
                // Fall through to other best-effort patterns
            }
        }

        // Try symbolic comparison: "amount > 0", "total <= 100.5"
        if (raw.matches("[a-zA-Z_][a-zA-Z0-9_]*\\s*(==|!=|>=|<=|>|<)\\s*.+")) {
            String[] parts = raw.split("\\s*(==|!=|>=|<=|>|<)\\s*", 2);
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[a-zA-Z_][a-zA-Z0-9_]*\\s*(==|!=|>=|<=|>|<)\\s*.+")
                .matcher(raw);
            if (parts.length == 2 && matcher.matches()) {
                String left = parts[0].trim();
                String op = matcher.group(1);
                String rightRaw = parts[1].trim();

                TypeName leftType = resolveTypeByName(left);
                if (isBigDecimalType(leftType)) {
                    CodeBlock rightOperand = toBigDecimalOperandFromRaw(rightRaw);
                    return switch (op) {
                        case ">" -> CodeBlock.of("this.$N.compareTo($L) > 0", left, rightOperand);
                        case "<" -> CodeBlock.of("this.$N.compareTo($L) < 0", left, rightOperand);
                        case ">=" -> CodeBlock.of("this.$N.compareTo($L) >= 0", left, rightOperand);
                        case "<=" -> CodeBlock.of("this.$N.compareTo($L) <= 0", left, rightOperand);
                        case "==" -> CodeBlock.of("this.$N.compareTo($L) == 0", left, rightOperand);
                        case "!=" -> CodeBlock.of("this.$N.compareTo($L) != 0", left, rightOperand);
                        default -> CodeBlock.of("true");
                    };
                }

                CodeBlock rightOperand = renderSimpleOperand(rightRaw);
                return CodeBlock.of("this.$N $L $L", left, op, rightOperand);
            }
        }
        
        // Try "X is not null" pattern
        if (raw.matches("\\w+ is not null")) {
            String var = raw.split(" ")[0];
            return CodeBlock.of("this.$N != null", var);
        }
        // Try "X is null" pattern
        if (raw.matches("\\w+ is null")) {
            String var = raw.split(" ")[0];
            return CodeBlock.of("this.$N == null", var);
        }
        // Try "X is present" pattern
        if (raw.matches(".+ is present")) {
            String var = raw.replaceAll(" is present$", "").trim();
            return CodeBlock.of("this.$N != null", var);
        }
        // Try "X is not present" pattern
        if (raw.matches(".+ is not present")) {
            String var = raw.replaceAll(" is not present$", "").trim();
            return CodeBlock.of("this.$N == null", var);
        }
        // Try "X is not empty" pattern (backup if parser didn't catch it)
        if (raw.matches(".+ is not empty")) {
            String var = raw.replaceAll(" is not empty$", "").trim();
            return CodeBlock.of("!$N.isEmpty()", var);
        }
        // Try "X is empty" pattern (backup)
        if (raw.matches(".+ is empty")) {
            String var = raw.replaceAll(" is empty$", "").trim();
            return CodeBlock.of("$N.isEmpty()", var);
        }
        // String operations
        if (raw.contains(" matches ")) {
            String[] parts = raw.split(" matches ", 2);
            String target = parts[0].trim();
            String candidate = parts[1].trim();
            if (isQuotedLiteral(candidate)) {
                return CodeBlock.of("$N.matches($S)", target, unquote(candidate));
            }
            return CodeBlock.of("$N.matches($L)", target, candidate);
        }
        if (raw.contains(" contains ")) {
            String[] parts = raw.split(" contains ", 2);
            return CodeBlock.of("$N.contains($L)", parts[0].trim(), parts[1].trim()); 
        }
        if (raw.contains(" starts with ")) {
            String[] parts = raw.split(" starts with ", 2);
            return CodeBlock.of("$N.startsWith($L)", parts[0].trim(), parts[1].trim());
        }
        if (raw.contains(" ends with ")) {
             String[] parts = raw.split(" ends with ", 2);
             return CodeBlock.of("$N.endsWith($L)", parts[0].trim(), parts[1].trim());
        }
        if (raw.matches(".+ has length between \\d+ and \\d+")) {
            String[] sides = raw.split(" has length between ", 2);
            String var = sides[0].trim();
            String[] bounds = sides[1].trim().split(" and ", 2);
            if (bounds.length == 2) {
                return CodeBlock.of("$N.length() >= $L && $N.length() <= $L",
                    var, bounds[0].trim(), var, bounds[1].trim());
            }
        }

        // If the condition has an expression, translate it
        if (condition.leftExpression() != null) {
            return translateExpression(condition.leftExpression());
        }
        
        return CodeBlock.of("/* $L */ true", raw);
    }
    
    /**
     * Translate condition as a lambda predicate.
     */
    private CodeBlock translateConditionAsLambda(NaturalLanguageCondition condition, String itemVar) {
        // For simple conditions, generate lambda
        return switch (condition.type()) {
            case IS_EMPTY -> CodeBlock.of("$N -> $N.isEmpty()", itemVar, itemVar);
            case IS_NOT_EMPTY -> CodeBlock.of("$N -> !$N.isEmpty()", itemVar, itemVar);
            case COMPARISON -> {
                String property = condition.leftExpression() instanceof VariableExpr left
                    ? left.name()
                    : condition.propertyPath();
                String getter = "get" + capitalize(property);
                String operator = mapComparisonOperator(condition.comparisonOperator());
                CodeBlock rightValue = condition.rightExpression() != null 
                    ? translateExpression(condition.rightExpression()) 
                    : CodeBlock.of("null");
                if (condition.comparisonOperator() == NaturalLanguageCondition.ComparisonOperator.EQUAL
                    || condition.comparisonOperator() == NaturalLanguageCondition.ComparisonOperator.NOT_EQUAL) {
                    if (condition.rightExpression() instanceof LiteralExpr lit
                        && lit.type() == LiteralExpr.LiteralType.STRING) {
                        if (condition.comparisonOperator() == NaturalLanguageCondition.ComparisonOperator.EQUAL) {
                            yield CodeBlock.of("$N -> $S.equalsIgnoreCase(String.valueOf($N.$N()))",
                                itemVar, lit.value(), itemVar, getter);
                        }
                        yield CodeBlock.of("$N -> !$S.equalsIgnoreCase(String.valueOf($N.$N()))",
                            itemVar, lit.value(), itemVar, getter);
                    }
                    if (isLikelySymbolicConstant(condition.rightExpression())) {
                        String symbolic = ((VariableExpr) condition.rightExpression()).name();
                        if (condition.comparisonOperator() == NaturalLanguageCondition.ComparisonOperator.EQUAL) {
                            yield CodeBlock.of("$N -> $S.equalsIgnoreCase(String.valueOf($N.$N()))",
                                itemVar, symbolic, itemVar, getter);
                        }
                        yield CodeBlock.of("$N -> !$S.equalsIgnoreCase(String.valueOf($N.$N()))",
                            itemVar, symbolic, itemVar, getter);
                    }
                }
                yield CodeBlock.of("$N -> $N.$N() $L $L", 
                    itemVar, itemVar, getter, operator, rightValue);
            }
            case STATE_IS -> {
                String[] parts = condition.rawText().split(" is ");
                if (parts.length == 2) {
                    String property = parts[0].trim();
                    String state = parts[1].trim().toUpperCase();
                    String getter = "get" + capitalize(property);
                    yield CodeBlock.of("$N -> $N.$N() == Status.$L", itemVar, itemVar, getter, state);
                }
                yield CodeBlock.of("$N -> true /* $L */", itemVar, condition.rawText());
            }
            default -> CodeBlock.of("$N -> true /* $L */", itemVar, condition.rawText());
        };
    }
    
    private CodeBlock translateUniversalCondition(NaturalLanguageCondition condition) {
        String collection = condition.quantifierTarget();
        String property = condition.propertyPath();
        String getter = "get" + capitalize(property);
        
        return CodeBlock.of("$N.stream().allMatch(item -> item.$N())", collection, getter);
    }
    
    private CodeBlock translateExistentialCondition(NaturalLanguageCondition condition) {
        String collection = condition.quantifierTarget();
        String property = condition.propertyPath();
        String getter = "get" + capitalize(property);
        
        return CodeBlock.of("$N.stream().anyMatch(item -> item.$N())", collection, getter);
    }
    
    private CodeBlock translateNegatedExistentialCondition(NaturalLanguageCondition condition) {
        String collection = condition.quantifierTarget();
        String property = condition.propertyPath();
        String getter = "get" + capitalize(property);
        
        return CodeBlock.of("$N.stream().noneMatch(item -> item.$N())", collection, getter);
    }
    
    private CodeBlock translateStateCondition(NaturalLanguageCondition condition) {
        // Use structured expression data from parser
        if (condition.leftExpression() instanceof VariableExpr left 
            && condition.rightExpression() != null) {
            CodeBlock rightValue = translateExpression(condition.rightExpression());
            // For string literal comparison: this.property.equals("value")
            if (condition.rightExpression() instanceof LiteralExpr lit 
                && lit.type() == LiteralExpr.LiteralType.STRING) {
                return CodeBlock.of("$L.equals(this.$N)", rightValue, left.name());
            }
            return CodeBlock.of("this.$N == $L", left.name(), rightValue);
        }
        // Fallback: parse raw text
        String rawText = condition.rawText();
        String[] parts = rawText.split(" is ");
        if (parts.length == 2) {
            String property = parts[0].trim();
            String state = parts[1].trim();
            // String literal check
            if (state.startsWith("\"") && state.endsWith("\"")) {
                return CodeBlock.of("$S.equals(this.$N)", state.substring(1, state.length() - 1), property);
            }
            return CodeBlock.of("this.$N == Status.$L", property, state.toUpperCase());
        }
        return CodeBlock.of("/* $L */ true", rawText);
    }
    
    private CodeBlock translateStateOneOfCondition(NaturalLanguageCondition condition) {
        List<String> states = condition.allowedValues();
        if (states == null || states.isEmpty()) {
            return CodeBlock.of("true");
        }
        
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("(");
        for (int i = 0; i < states.size(); i++) {
            if (i > 0) code.add(" || ");
            code.add("this.status == Status.$L", states.get(i).toUpperCase());
        }
        code.add(")");
        
        return code.build();
    }
    
    private CodeBlock translateComparisonCondition(NaturalLanguageCondition condition) {
        String rawText = condition.rawText();
        if (rawText != null && rawText.matches(".+ is valid [A-Z_ ]+ format")) {
            String[] sides = rawText.split(" is valid ", 2);
            String targetName = sides[0].trim();
            String formatLabel = sides[1].replace(" format", "").trim().replace(' ', '_');
            try {
                StringCondition.FormatType formatType = StringCondition.FormatType.valueOf(formatLabel);
                return generateFormatValidation(CodeBlock.of("this.$N", targetName), formatType);
            } catch (IllegalArgumentException ignored) {
                // Fall through to standard comparison handling
            }
        }

        if (rawText != null && rawText.matches(".+ is not valid [A-Z_ ]+ format")) {
            String[] sides = rawText.split(" is not valid ", 2);
            String targetName = sides[0].trim();
            String formatLabel = sides[1].replace(" format", "").trim().replace(' ', '_');
            try {
                StringCondition.FormatType formatType = StringCondition.FormatType.valueOf(formatLabel);
                return CodeBlock.of("!($L)", generateFormatValidation(CodeBlock.of("this.$N", targetName), formatType));
            } catch (IllegalArgumentException ignored) {
                // Fall through to standard comparison handling
            }
        }

        String operator = mapComparisonOperator(condition.comparisonOperator());
        CodeBlock rightValue = condition.rightExpression() != null 
            ? translateExpression(condition.rightExpression()) 
            : CodeBlock.of("null");
        
        // Use structured expression data when available
        if (condition.leftExpression() instanceof VariableExpr left) {
            // For NOT_EQUAL with string values: !"value".equals(this.property)
            if (condition.comparisonOperator() == NaturalLanguageCondition.ComparisonOperator.NOT_EQUAL
                && condition.rightExpression() instanceof LiteralExpr lit 
                && lit.type() == LiteralExpr.LiteralType.STRING) {
                return CodeBlock.of("!$L.equals(this.$N)", rightValue, left.name());
            }
            // For EQUAL with string values: "value".equals(this.property)
            if (condition.comparisonOperator() == NaturalLanguageCondition.ComparisonOperator.EQUAL
                && condition.rightExpression() instanceof LiteralExpr lit 
                && lit.type() == LiteralExpr.LiteralType.STRING) {
                return CodeBlock.of("$L.equals(this.$N)", rightValue, left.name());
            }
            if (condition.rightExpression() instanceof NullExpr) {
                return CodeBlock.of("this.$N $L null", left.name(), operator);
            }

            TypeName leftType = resolveTypeByName(left.name());
            if (isBigDecimalType(leftType) && supportsOrderedComparison(condition.comparisonOperator())) {
                CodeBlock rightOperand = toBigDecimalOperand(condition.rightExpression(), rightValue);
                return switch (condition.comparisonOperator()) {
                    case GREATER_THAN -> CodeBlock.of("this.$N.compareTo($L) > 0", left.name(), rightOperand);
                    case LESS_THAN -> CodeBlock.of("this.$N.compareTo($L) < 0", left.name(), rightOperand);
                    case AT_LEAST -> CodeBlock.of("this.$N.compareTo($L) >= 0", left.name(), rightOperand);
                    case AT_MOST -> CodeBlock.of("this.$N.compareTo($L) <= 0", left.name(), rightOperand);
                    case EQUAL -> CodeBlock.of("this.$N.compareTo($L) == 0", left.name(), rightOperand);
                    case NOT_EQUAL -> CodeBlock.of("this.$N.compareTo($L) != 0", left.name(), rightOperand);
                    default -> CodeBlock.of("this.$N $L $L", left.name(), operator, rightValue);
                };
            }

            if (isLikelySymbolicConstant(condition.rightExpression())) {
                String symbolic = ((VariableExpr) condition.rightExpression()).name();
                if (condition.comparisonOperator() == NaturalLanguageCondition.ComparisonOperator.EQUAL) {
                    return CodeBlock.of("$S.equalsIgnoreCase(String.valueOf(this.$N))", symbolic, left.name());
                }
                if (condition.comparisonOperator() == NaturalLanguageCondition.ComparisonOperator.NOT_EQUAL) {
                    return CodeBlock.of("!$S.equalsIgnoreCase(String.valueOf(this.$N))", symbolic, left.name());
                }
            }
            return CodeBlock.of("this.$N $L $L", left.name(), operator, rightValue);
        }
        
        // Fallback: use propertyPath
        String property = condition.propertyPath();
        if (property == null || property.isEmpty()) {
            property = "unknown";
        }
        return CodeBlock.of("this.$N $L $L", property, operator, rightValue);
    }

    private boolean isLikelySymbolicConstant(Expr expr) {
        if (!(expr instanceof VariableExpr variable) || variable.name() == null || variable.name().isBlank()) {
            return false;
        }
        String name = variable.name();
        return Character.isUpperCase(name.charAt(0)) || name.equals(name.toUpperCase());
    }
    
    // ========== Helper Methods ==========
    
    private TypeName determineReturnType(BehaviorDecl behavior) {
        if (behavior.returnClause() != null) {
            // Check if there's an expression we can infer type from
            if (behavior.returnClause().expression() != null) {
                // For now, return Object - proper type inference would need more context
                return ClassName.get(Object.class);
            }
            // Check return type enum
            ReturnClause.ReturnType returnType = behavior.returnClause().returnType();
            if (returnType == ReturnClause.ReturnType.SUCCESS || 
                returnType == ReturnClause.ReturnType.SUCCESS_WITH) {
                return ClassName.get(Object.class); // Or a Result type
            }
        }
        return TypeName.VOID;
    }
    
    @SuppressWarnings("unused")
    private String toMethodName(String behaviorName) {
        if (behaviorName == null) return "execute";
        
        // Convert natural language to camelCase method name
        // "placing an order" → "placingAnOrder"
        String[] words = behaviorName.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            result.append(capitalize(words[i]));
        }
        return result.toString();
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    
    private String mapBinaryOperator(BinaryExpr.BinaryOperator operator) {
        return switch (operator) {
            case PLUS, ADD -> "+";
            case MINUS, SUBTRACT -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            case MODULO -> "%";
            case EQUALS -> "==";
            case NOT_EQUALS -> "!=";
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            case LESS_THAN_OR_EQUAL -> "<=";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case AND -> "&&";
            case OR -> "||";
            case CONCAT -> "+";
            case IN -> "/* in */ ==";
        };
    }
    
    private String mapUnaryOperator(UnaryExpr.UnaryOperator operator) {
        return switch (operator) {
            case NEGATE -> "-";
            case NOT -> "!";
        };
    }
    
    private String mapComparisonOperator(NaturalLanguageCondition.ComparisonOperator operator) {
        if (operator == null) return "==";
        return switch (operator) {
            case EQUAL -> "==";
            case NOT_EQUAL -> "!=";
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            case AT_LEAST -> ">=";
            case AT_MOST -> "<=";
            case WITHIN -> "/* within */";
        };
    }

    private boolean supportsOrderedComparison(NaturalLanguageCondition.ComparisonOperator operator) {
        return operator == NaturalLanguageCondition.ComparisonOperator.GREATER_THAN
            || operator == NaturalLanguageCondition.ComparisonOperator.LESS_THAN
            || operator == NaturalLanguageCondition.ComparisonOperator.AT_LEAST
            || operator == NaturalLanguageCondition.ComparisonOperator.AT_MOST
            || operator == NaturalLanguageCondition.ComparisonOperator.EQUAL
            || operator == NaturalLanguageCondition.ComparisonOperator.NOT_EQUAL;
    }

    private TypeName resolveTypeByName(String symbolName) {
        if (symbolName == null || symbolName.isBlank()) {
            return null;
        }
        TypeName paramType = currentBehaviorParamTypes.get(symbolName);
        if (paramType != null) {
            return paramType;
        }
        return typeMapper.tryResolveParamType(symbolName);
    }

    private boolean isBigDecimalType(TypeName typeName) {
        return typeName != null && typeName.equals(BIG_DECIMAL);
    }

    private CodeBlock toBigDecimalOperand(Expr rawExpr, CodeBlock translatedExpr) {
        if (rawExpr instanceof LiteralExpr literal && literal.value() instanceof Number number) {
            return CodeBlock.of("new $T($S)", BIG_DECIMAL, number.toString());
        }
        if (rawExpr != null && isBigDecimalType(resolveExpressionType(rawExpr))) {
            return translatedExpr;
        }
        return CodeBlock.of("new $T(String.valueOf($L))", BIG_DECIMAL, translatedExpr);
    }

    private CodeBlock toBigDecimalOperandFromRaw(String raw) {
        String candidate = raw != null ? raw.trim() : "0";
        if (candidate.matches("-?\\d+(\\.\\d+)?")) {
            return CodeBlock.of("new $T($S)", BIG_DECIMAL, candidate);
        }
        if (isQuotedLiteral(candidate)) {
            return CodeBlock.of("new $T($S)", BIG_DECIMAL, unquote(candidate));
        }
        return CodeBlock.of("new $T(String.valueOf($N))", BIG_DECIMAL, candidate);
    }

    private CodeBlock renderSimpleOperand(String raw) {
        String candidate = raw != null ? raw.trim() : "null";
        if (isQuotedLiteral(candidate)) {
            return CodeBlock.of("$S", unquote(candidate));
        }
        if (candidate.matches("-?\\d+(\\.\\d+)?")
            || "true".equalsIgnoreCase(candidate)
            || "false".equalsIgnoreCase(candidate)
            || "null".equalsIgnoreCase(candidate)) {
            return CodeBlock.of("$L", candidate);
        }
        return CodeBlock.of("$N", candidate);
    }

    private boolean isQuotedLiteral(String value) {
        return value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"");
    }

    private String unquote(String value) {
        if (!isQuotedLiteral(value)) {
            return value;
        }
        return value.substring(1, value.length() - 1);
    }
    
    // =================================================================================
    // STRING CONDITIONS & OPERATIONS
    // =================================================================================
    
    /**
     * Translates StringCondition expressions to Java boolean expressions.
     * <pre>
     * DDSL: email contains "@"
     * Java: email.contains("@")
     * 
     * DDSL: name is valid EMAIL format
     * Java: Pattern.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", email)
     * </pre>
     */
    private CodeBlock visitStringCondition(StringCondition condition) {
        CodeBlock target = translateExpression(condition.stringExpr());
        
        return switch (condition.type()) {
            case CONTAINS -> CodeBlock.of("$L.contains($S)", target, condition.literal());
            case DOES_NOT_CONTAIN -> CodeBlock.of("!$L.contains($S)", target, condition.literal());
            case STARTS_WITH -> CodeBlock.of("$L.startsWith($S)", target, condition.literal());
            case ENDS_WITH -> CodeBlock.of("$L.endsWith($S)", target, condition.literal());
            case MATCHES -> CodeBlock.of("$L.matches($S)", target, condition.literal());
            case DOES_NOT_MATCH -> CodeBlock.of("!$L.matches($S)", target, condition.literal());
            case IS_EMPTY -> CodeBlock.of("$L.isEmpty()", target);
            case IS_NOT_EMPTY -> CodeBlock.of("!$L.isEmpty()", target);
            case IS_BLANK -> CodeBlock.of("$L.isBlank()", target);
            case IS_NOT_BLANK -> CodeBlock.of("!$L.isBlank()", target);
            case HAS_LENGTH_EXACTLY -> CodeBlock.of("$L.length() == $L", target, condition.lengthValue());
            case HAS_LENGTH_AT_LEAST -> CodeBlock.of("$L.length() >= $L", target, condition.lengthValue());
            case HAS_LENGTH_AT_MOST -> CodeBlock.of("$L.length() <= $L", target, condition.lengthValue());
            case HAS_LENGTH_GREATER_THAN -> CodeBlock.of("$L.length() > $L", target, condition.lengthValue());
            case HAS_LENGTH_LESS_THAN -> CodeBlock.of("$L.length() < $L", target, condition.lengthValue());
            case HAS_LENGTH_BETWEEN -> CodeBlock.of("$L.length() >= $L && $L.length() <= $L", 
                    target, condition.lengthMin(), target, condition.lengthMax());
            case IS_VALID_FORMAT -> generateFormatValidation(target, condition.formatType());
            case IS_NOT_VALID_FORMAT -> CodeBlock.of("!($L)", generateFormatValidation(target, condition.formatType()));
        };
    }
    
    /**
     * Generates regex pattern matching for format validation.
     */
    private CodeBlock generateFormatValidation(CodeBlock target, StringCondition.FormatType formatType) {
        if (formatType == null) {
            return CodeBlock.of("/* unknown format */");
        }
        
        String pattern = switch (formatType) {
            case EMAIL -> "^[\\\\w.-]+@[\\\\w.-]+\\\\.[a-zA-Z]{2,}$";
            case PHONE_NUMBER -> "^\\\\+?[0-9]{10,15}$";
            case URL -> "^https?://[\\\\w.-]+(:\\\\d+)?(/.*)?$";
            case UUID -> "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
            case DATE -> "^\\\\d{4}-\\\\d{2}-\\\\d{2}$";
            case ALPHANUMERIC -> "^[a-zA-Z0-9]+$";
            case NUMERIC -> "^[0-9]+$";
        };
        
        return CodeBlock.of("$T.matches($S, $L)", Pattern.class, pattern, target);
    }
    
    /**
     * Translates StringOperation expressions to Java string transformations.
     * <pre>
     * DDSL: name to uppercase
     * Java: name.toUpperCase()
     * 
     * DDSL: description truncated to 100
     * Java: description.substring(0, Math.min(description.length(), 100))
     * </pre>
     */
    private CodeBlock visitStringOperation(StringOperation operation) {
        CodeBlock target = translateExpression(operation.stringExpr());
        
        return switch (operation.type()) {
            case TO_UPPERCASE -> CodeBlock.of("$L.toUpperCase()", target);
            case TO_LOWERCASE -> CodeBlock.of("$L.toLowerCase()", target);
            case TRIMMED -> CodeBlock.of("$L.trim()", target);
            case TRUNCATED_TO -> CodeBlock.of("$L.substring(0, $T.min($L.length(), $L))", 
                    target, Math.class, target, operation.lengthValue());
            case CONCATENATED_WITH -> {
                if (operation.otherStringExpr() != null) {
                    CodeBlock other = translateExpression(operation.otherStringExpr());
                    yield CodeBlock.of("$L + $L", target, other);
                } else if (operation.literal() != null) {
                    yield CodeBlock.of("$L + $S", target, operation.literal());
                }
                yield CodeBlock.of("/* missing concatenation argument */");
            }
            case WITHOUT -> CodeBlock.of("$L.replace($S, \"\")", target, operation.literal());
            case FIRST_N_CHARACTERS -> CodeBlock.of("$L.substring(0, $T.min($L.length(), $L))", 
                    target, Math.class, target, operation.lengthValue());
            case LAST_N_CHARACTERS -> CodeBlock.of("$L.substring($T.max(0, $L.length() - $L))", 
                    target, Math.class, target, operation.lengthValue());
            case REPLACED -> CodeBlock.of("$L.replace($S, $S)", 
                    target, operation.literal(), operation.replacement());
        };
    }
    
    // =================================================================================
    // SPECIFICATION CONDITIONS
    // =================================================================================
    
    /**
     * Translates SpecificationCondition to Specification pattern invocation.
     * <pre>
     * DDSL: order satisfies PremiumOrderSpecification
     * Java: new PremiumOrderSpecification().isSatisfiedBy(order)
     * 
     * DDSL: order does not satisfy ExpiredOrderSpecification
     * Java: !new ExpiredOrderSpecification().isSatisfiedBy(order)
     * 
     * DDSL: customer satisfies ActiveCustomer and VIPCustomer
     * Java: new ActiveCustomer().isSatisfiedBy(customer) && new VIPCustomer().isSatisfiedBy(customer)
     * </pre>
     */
    private CodeBlock visitSpecificationCondition(SpecificationCondition condition) {
        CodeBlock subject = translateExpression(condition.subject());
        SpecificationCondition.SpecificationRef specRef = condition.specificationRef();
        
        boolean negated = condition.conditionType() == SpecificationCondition.SpecificationConditionType.DOES_NOT_SATISFY;
        
        CodeBlock specCheck = translateSpecificationRef(specRef, subject);
        
        if (negated) {
            return CodeBlock.of("!($L)", specCheck);
        }
        return specCheck;
    }
    
    /**
     * Recursively translates SpecificationRef (handles composite specs).
     */
    private CodeBlock translateSpecificationRef(SpecificationCondition.SpecificationRef ref, CodeBlock subject) {
        return switch (ref) {
            case SpecificationCondition.SpecificationRef.Simple simple -> {
                ClassName specClassName = typeMapper.resolveDomainClassName(simple.name());
                yield CodeBlock.of("new $T().isSatisfiedBy($L)", specClassName, subject);
            }
            case SpecificationCondition.SpecificationRef.Parameterized param -> {
                ClassName specClassName = typeMapper.resolveDomainClassName(param.name());
                CodeBlock.Builder args = CodeBlock.builder();
                for (int i = 0; i < param.arguments().size(); i++) {
                    if (i > 0) args.add(", ");
                    args.add(translateExpression(param.arguments().get(i)));
                }
                yield CodeBlock.of("new $T($L).isSatisfiedBy($L)", specClassName, args.build(), subject);
            }
            case SpecificationCondition.SpecificationRef.Composite composite -> {
                CodeBlock left = translateSpecificationRef(composite.left(), subject);
                CodeBlock right = translateSpecificationRef(composite.right(), subject);
                String operator = composite.compositeType() == 
                    SpecificationCondition.SpecificationRef.Composite.CompositeType.AND ? "&&" : "||";
                yield CodeBlock.of("($L $L $L)", left, operator, right);
            }
            case SpecificationCondition.SpecificationRef.Negation negation -> {
                CodeBlock inner = translateSpecificationRef(negation.inner(), subject);
                yield CodeBlock.of("!($L)", inner);
            }
        };
    }
}
