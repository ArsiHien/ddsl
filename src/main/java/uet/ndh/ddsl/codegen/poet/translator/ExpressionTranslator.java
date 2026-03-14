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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uet.ndh.ddsl.ast.expr.BinaryExpr.BinaryOperator.EQUALS;
import static uet.ndh.ddsl.ast.expr.BinaryExpr.BinaryOperator.GREATER_THAN_OR_EQUAL;
import static uet.ndh.ddsl.ast.expr.BinaryExpr.BinaryOperator.LESS_THAN_OR_EQUAL;
import static uet.ndh.ddsl.ast.expr.BinaryExpr.BinaryOperator.NOT_EQUALS;
import static uet.ndh.ddsl.ast.expr.TemporalRange.RangeType.BETWEEN;
import static uet.ndh.ddsl.parser.lexer.TokenType.EXCEEDS;

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
    
    // Common type references
    private static final ClassName ILLEGAL_ARGUMENT = ClassName.get(IllegalArgumentException.class);
    private static final ClassName COLLECTORS = ClassName.get(Collectors.class);
    private static final ClassName INSTANT = ClassName.get(Instant.class);
    private static final ClassName CHRONO_UNIT = ClassName.get(ChronoUnit.class);
    private static final ClassName OBJECTS = ClassName.get(Objects.class);
    
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
        }
        
        // Generate method body
        builder.addCode(translateBehaviorBody(behavior));
        
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
            case ThisExpr thisExpr -> CodeBlock.of("this");
            case NullExpr nullExpr -> CodeBlock.of("null");
            case CollectionFilter filter -> visitCollectionFilter(filter);
            case CollectionAggregation agg -> visitCollectionAggregation(agg);
            case CollectionGroupBy groupBy -> visitCollectionGroupBy(groupBy);
            case CollectionFlatten flatten -> visitCollectionFlatten(flatten);
            case MatchExpr match -> visitMatch(match);
            case StringCondition strCond -> visitStringCondition(strCond);
            case StringOperation strOp -> visitStringOperation(strOp);
            case SpecificationCondition specCond -> visitSpecificationCondition(specCond);
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
        
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("switch ($L) {\n", subject);
        
        for (MatchExpr.MatchCase matchCase : match.cases()) {
            CodeBlock bodyCode = translateMatchCaseBody(matchCase.body());
            code.add("    case $L -> $L;\n", 
                matchCase.pattern(), 
                bodyCode);
        }
        
        if (match.defaultCase() != null) {
            CodeBlock defaultBody = translateMatchCaseBody(match.defaultCase().body());
            code.add("    default -> $L;\n", defaultBody);
        }
        
        code.add("}");
        return code.build();
    }
    
    /**
     * Translate a MatchCaseBody to CodeBlock.
     */
    private CodeBlock translateMatchCaseBody(MatchExpr.MatchCaseBody body) {
        return switch (body) {
            case MatchExpr.MatchCaseBody.ExpressionBody exprBody -> 
                translateExpression(exprBody.expression());
            case MatchExpr.MatchCaseBody.StatementBody stmtBody -> 
                CodeBlock.of("/* statement */");
            case MatchExpr.MatchCaseBody.BlockBody blockBody -> 
                CodeBlock.of("/* block */");
        };
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
                String property = condition.propertyPath();
                String getter = "get" + capitalize(property);
                String operator = mapComparisonOperator(condition.comparisonOperator());
                CodeBlock rightValue = condition.rightExpression() != null 
                    ? translateExpression(condition.rightExpression()) 
                    : CodeBlock.of("null");
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
            return CodeBlock.of("this.$N $L $L", left.name(), operator, rightValue);
        }
        
        // Fallback: use propertyPath
        String property = condition.propertyPath();
        if (property == null || property.isEmpty()) {
            property = "unknown";
        }
        return CodeBlock.of("this.$N $L $L", property, operator, rightValue);
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
