package uet.ndh.ddsl.core.building.logic;

import lombok.Data;
import uet.ndh.ddsl.core.building.expression.*;
import uet.ndh.ddsl.core.building.statement.*;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;

import java.util.*;

/**
 * Generates business logic implementations for domain methods.
 */
@Data
public class BusinessLogicGenerator {

    private final Map<String, LogicTemplate> templates = new HashMap<>();

    public BusinessLogicGenerator() {
        initializeTemplates();
    }

    /**
     * Generate business logic for a method based on its name and context.
     */
    public List<Statement> generateMethodLogic(String methodName, String returnType,
                                              List<String> parameterNames, Entity context) {

        // Analyze method name to determine logic pattern
        LogicPattern pattern = analyzeMethodName(methodName);

        switch (pattern) {
            case SLUG_GENERATOR:
                return generateSlugLogic(parameterNames, returnType);
            case VALIDATOR:
                return generateValidationLogic(parameterNames, returnType, context);
            case CALCULATOR:
                return generateCalculationLogic(methodName, parameterNames, returnType, context);
            case STATUS_CHECKER:
                return generateStatusCheckLogic(methodName, parameterNames, returnType, context);
            case COLLECTION_PROCESSOR:
                return generateCollectionLogic(methodName, parameterNames, returnType, context);
            case BUSINESS_RULE:
                return generateBusinessRuleLogic(methodName, parameterNames, returnType, context);
            default:
                return generateDefaultLogic(methodName, parameterNames, returnType);
        }
    }

    /**
     * Generate slug creation logic (like for PostSlugGenerator).
     */
    private List<Statement> generateSlugLogic(List<String> parameters, String returnType) {
        List<Statement> statements = new ArrayList<>();

        // Assume first parameter is the title/name to slugify
        String titleParam = parameters.isEmpty() ? "title" : parameters.get(0);

        // Get the title value
        statements.add(new VariableDeclarationStatement(
            "String",
            "titleValue",
            new MethodCallExpression(
                new VariableExpression(titleParam, "PostTitle"),
                "getValue",
                Collections.emptyList(),
                "String"
            ),
            false
        ));

        // Null check
        statements.add(new IfStatement(
            new BinaryExpression(
                new VariableExpression("titleValue", "String"),
                BinaryOperator.EQ,
                new LiteralExpression(null, "String"),
                "boolean"
            ),
                List.of(new ReturnStatement(new LiteralExpression("", "String"))),
            null
        ));

        // Convert to lowercase and replace spaces with hyphens
        statements.add(new VariableDeclarationStatement(
            "String",
            "slug",
            new MethodCallExpression(
                new MethodCallExpression(
                    new MethodCallExpression(
                        new VariableExpression("titleValue", "String"),
                        "toLowerCase",
                        Collections.emptyList(),
                        "String"
                    ),
                    "trim",
                    Collections.emptyList(),
                    "String"
                ),
                "replaceAll",
                Arrays.asList(
                    new LiteralExpression("\\s+", "String"),
                    new LiteralExpression("-", "String")
                ),
                "String"
            ),
            false
        ));

        // Remove special characters
        statements.add(new AssignmentStatement(
            "slug",
            new MethodCallExpression(
                new VariableExpression("slug", "String"),
                "replaceAll",
                Arrays.asList(
                    new LiteralExpression("[^a-z0-9\\-]", "String"),
                    new LiteralExpression("", "String")
                ),
                "String"
            )
        ));

        // Return the slug
        statements.add(new ReturnStatement(new VariableExpression("slug", "String")));

        return statements;
    }

    /**
     * Generate validation logic.
     */
    private List<Statement> generateValidationLogic(List<String> parameters, String returnType, Entity context) {
        List<Statement> statements = new ArrayList<>();

        // Create validation result list
        statements.add(new VariableDeclarationStatement(
            "List<String>",
            "errors",
            new MethodCallExpression(
                null,
                "new ArrayList<>",
                Collections.emptyList(),
                "List<String>"
            ),
            false
        ));

        // Generate validation checks for each field
        if (context != null) {
            for (var field : context.getFields()) {
                statements.addAll(generateFieldValidation(field.getName(), field.getType().getSimpleName()));
            }
        }

        // Return validation result
        if ("boolean".equals(returnType)) {
            statements.add(new ReturnStatement(
                new MethodCallExpression(
                    new VariableExpression("errors", "List<String>"),
                    "isEmpty",
                    Collections.emptyList(),
                    "boolean"
                )
            ));
        } else {
            statements.add(new ReturnStatement(new VariableExpression("errors", "List<String>")));
        }

        return statements;
    }

    /**
     * Generate calculation logic (totals, averages, etc.).
     */
    private List<Statement> generateCalculationLogic(String methodName, List<String> parameters,
                                                    String returnType, Entity context) {
        List<Statement> statements = new ArrayList<>();

        if (methodName.contains("total") || methodName.contains("sum")) {
            // Generate sum calculation
            statements.add(new VariableDeclarationStatement(
                "double",
                "total",
                new LiteralExpression(0.0, "double"),
                false
            ));

            // If we have a collection field, sum it
            if (context != null) {
                for (var field : context.getFields()) {
                    if (isCollectionType(field.getType().getSimpleName())) {
                        statements.add(new ForEachStatement(
                            extractElementType(field.getType().getSimpleName()),
                            "item",
                            new FieldAccessExpression(
                                new VariableExpression("this", context.getName()),
                                field.getName(),
                                field.getType().getSimpleName()
                            ),
                            Arrays.asList(new AssignmentStatement(
                                "total",
                                new BinaryExpression(
                                    new VariableExpression("total", "double"),
                                    BinaryOperator.ADD,
                                    new MethodCallExpression(
                                        new VariableExpression("item", extractElementType(field.getType().getSimpleName())),
                                        "getAmount", // Assume amount field
                                        Collections.emptyList(),
                                        "double"
                                    ),
                                    "double"
                                )
                            ))
                        ));
                        break; // Just use first collection found
                    }
                }
            }

            statements.add(new ReturnStatement(new VariableExpression("total", "double")));
        } else if (methodName.contains("count")) {
            // Generate count logic
            if (context != null) {
                for (var field : context.getFields()) {
                    if (isCollectionType(field.getType().getSimpleName())) {
                        statements.add(new ReturnStatement(
                            new MethodCallExpression(
                                new FieldAccessExpression(
                                    new VariableExpression("this", context.getName()),
                                    field.getName(),
                                    field.getType().getSimpleName()
                                ),
                                "size",
                                Collections.emptyList(),
                                "int"
                            )
                        ));
                        break;
                    }
                }
            }
        }

        return statements;
    }

    /**
     * Generate status checking logic.
     */
    private List<Statement> generateStatusCheckLogic(String methodName, List<String> parameters,
                                                    String returnType, Entity context) {
        List<Statement> statements = new ArrayList<>();

        // Find status field
        String statusField = findStatusField(context);
        if (statusField != null) {
            Expression statusAccess = new FieldAccessExpression(
                new VariableExpression("this", context.getName()),
                statusField,
                "Status"
            );

            if (methodName.contains("active") || methodName.contains("Active")) {
                statements.add(new ReturnStatement(
                    new BinaryExpression(
                        statusAccess,
                        BinaryOperator.EQ,
                        new FieldAccessExpression(
                            new VariableExpression("Status", "Status"),
                            "ACTIVE",
                            "Status"
                        ),
                        "boolean"
                    )
                ));
            } else if (methodName.contains("pending") || methodName.contains("Pending")) {
                statements.add(new ReturnStatement(
                    new BinaryExpression(
                        statusAccess,
                        BinaryOperator.EQ,
                        new FieldAccessExpression(
                            new VariableExpression("Status", "Status"),
                            "PENDING",
                            "Status"
                        ),
                        "boolean"
                    )
                ));
            }
        } else {
            // Default boolean return
            statements.add(new ReturnStatement(new LiteralExpression(true, "boolean")));
        }

        return statements;
    }

    /**
     * Generate collection processing logic.
     */
    private List<Statement> generateCollectionLogic(String methodName, List<String> parameters,
                                                   String returnType, Entity context) {
        List<Statement> statements = new ArrayList<>();

        if (methodName.contains("filter") || methodName.contains("find")) {
            // Generate filter logic
            statements.add(new VariableDeclarationStatement(
                returnType,
                "result",
                new MethodCallExpression(
                    null,
                    "new ArrayList<>",
                    Collections.emptyList(),
                    returnType
                ),
                false
            ));

            // Find collection field to filter
            if (context != null) {
                for (var field : context.getFields()) {
                    if (isCollectionType(field.getType().getSimpleName())) {
                        statements.add(new ForEachStatement(
                            extractElementType(field.getType().getSimpleName()),
                            "item",
                            new FieldAccessExpression(
                                new VariableExpression("this", context.getName()),
                                field.getName(),
                                field.getType().getSimpleName()
                            ),
                            Arrays.asList(new IfStatement(
                                generateFilterCondition(methodName, "item"),
                                Arrays.asList(new ExpressionStatement(
                                    new MethodCallExpression(
                                        new VariableExpression("result", returnType),
                                        "add",
                                        Arrays.asList(new VariableExpression("item", extractElementType(field.getType().getSimpleName()))),
                                        "boolean"
                                    )
                                )),
                                null
                            ))
                        ));
                        break;
                    }
                }
            }

            statements.add(new ReturnStatement(new VariableExpression("result", returnType)));
        }

        return statements;
    }

    /**
     * Generate generic business rule logic.
     */
    private List<Statement> generateBusinessRuleLogic(String methodName, List<String> parameters,
                                                     String returnType, Entity context) {
        List<Statement> statements = new ArrayList<>();

        // Create basic business rule structure
        if ("boolean".equals(returnType)) {
            // Boolean business rule
            statements.add(new ReturnStatement(new LiteralExpression(true, "boolean")));
        } else if ("void".equals(returnType)) {
            // Action business rule
            statements.add(new ExpressionStatement(
                new MethodCallExpression(
                    null,
                    "// Business rule implementation needed",
                    Collections.emptyList(),
                    "void"
                )
            ));
        } else {
            // Return type business rule
            statements.add(new ReturnStatement(new LiteralExpression(null, returnType)));
        }

        return statements;
    }

    /**
     * Generate default implementation.
     */
    private List<Statement> generateDefaultLogic(String methodName, List<String> parameters, String returnType) {
        List<Statement> statements = new ArrayList<>();

        if ("void".equals(returnType)) {
            // Empty method body for void methods
            statements.add(new ExpressionStatement(
                new LiteralExpression("// Method implementation", "void")
            ));
        } else {
            // Return appropriate default value
            Object defaultValue = getDefaultValue(returnType);
            statements.add(new ReturnStatement(new LiteralExpression(defaultValue, returnType)));
        }

        return statements;
    }

    // Helper methods

    private LogicPattern analyzeMethodName(String methodName) {
        String lower = methodName.toLowerCase();

        if (lower.contains("slug")) return LogicPattern.SLUG_GENERATOR;
        if (lower.contains("validate") || lower.contains("check")) return LogicPattern.VALIDATOR;
        if (lower.contains("calculate") || lower.contains("total") || lower.contains("sum") || lower.contains("count"))
            return LogicPattern.CALCULATOR;
        if (lower.contains("active") || lower.contains("pending") || lower.contains("status"))
            return LogicPattern.STATUS_CHECKER;
        if (lower.contains("filter") || lower.contains("find") || lower.contains("get") && lower.contains("by"))
            return LogicPattern.COLLECTION_PROCESSOR;

        return LogicPattern.BUSINESS_RULE;
    }

    private List<Statement> generateFieldValidation(String fieldName, String fieldType) {
        List<Statement> statements = new ArrayList<>();

        // Null check for required fields
        statements.add(new IfStatement(
            new BinaryExpression(
                new FieldAccessExpression(
                    new VariableExpression("this", "Entity"),
                    fieldName,
                    fieldType
                ),
                BinaryOperator.EQ,
                new LiteralExpression(null, fieldType),
                "boolean"
            ),
            Arrays.asList(new ExpressionStatement(
                new MethodCallExpression(
                    new VariableExpression("errors", "List<String>"),
                    "add",
                    Arrays.asList(new LiteralExpression(fieldName + " is required", "String")),
                    "boolean"
                )
            )),
            null
        ));

        return statements;
    }

    private boolean isCollectionType(String typeName) {
        return typeName.startsWith("List<") || typeName.startsWith("Set<") || typeName.startsWith("Collection<");
    }

    private String extractElementType(String collectionType) {
        if (collectionType.contains("<") && collectionType.contains(">")) {
            int start = collectionType.indexOf('<') + 1;
            int end = collectionType.lastIndexOf('>');
            return collectionType.substring(start, end);
        }
        return "Object";
    }

    private String findStatusField(Entity entity) {
        if (entity == null) return null;

        return entity.getFields().stream()
            .filter(f -> f.getName().toLowerCase().contains("status"))
            .map(f -> f.getName())
            .findFirst()
            .orElse(null);
    }

    private Expression generateFilterCondition(String methodName, String itemVariable) {
        // Generate filter condition based on method name
        if (methodName.toLowerCase().contains("active")) {
            return new MethodCallExpression(
                new VariableExpression(itemVariable, "Object"),
                "isActive",
                Collections.emptyList(),
                "boolean"
            );
        }

        // Default condition
        return new LiteralExpression(true, "boolean");
    }

    private Object getDefaultValue(String type) {
        switch (type) {
            case "boolean": return false;
            case "int": case "Integer": return 0;
            case "long": case "Long": return 0L;
            case "double": case "Double": return 0.0;
            case "String": return null;
            default: return null;
        }
    }

    private void initializeTemplates() {
        // Initialize common logic templates
        // This could be expanded with more sophisticated templates
    }

    private enum LogicPattern {
        SLUG_GENERATOR,
        VALIDATOR,
        CALCULATOR,
        STATUS_CHECKER,
        COLLECTION_PROCESSOR,
        BUSINESS_RULE
    }

    private static class LogicTemplate {
        // Template for reusable logic patterns
    }
}
