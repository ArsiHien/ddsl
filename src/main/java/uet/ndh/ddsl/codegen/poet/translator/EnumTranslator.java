package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.*;
import uet.ndh.ddsl.ast.model.statemachine.StateDecl;
import uet.ndh.ddsl.ast.model.statemachine.StateMachineDecl;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.TypeMapper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates DDSL constructs to Java enum TypeSpecs using JavaPoet.
 * 
 * Handles:
 * - State machine states → State enum
 * - Domain enumerations (defined via value objects with constrained values)
 * - Status/type enumerations extracted from field constraints
 * 
 * Example transformation:
 * <pre>
 * DDSL State Machine:
 *   states:
 *     - Pending (initial)
 *     - Confirmed
 *     - Completed (final)
 * 
 * Generated Java:
 *   public enum OrderStatus {
 *       PENDING,
 *       CONFIRMED,
 *       COMPLETED;
 *       
 *       public boolean isInitial() { return this == PENDING; }
 *       public boolean isFinal() { return this == COMPLETED; }
 *   }
 * </pre>
 */
public class EnumTranslator {
    
    private final TypeMapper typeMapper;
    private final String basePackage;
    
    public EnumTranslator(TypeMapper typeMapper, String basePackage) {
        this.typeMapper = typeMapper;
        this.basePackage = basePackage;
    }
    
    // =================================================================================
    // STATE MACHINE → ENUM
    // =================================================================================
    
    /**
     * Generate a state enum from a state machine declaration.
     * 
     * @param stateMachine The state machine declaration
     * @param entityName The owning entity name (for naming convention)
     * @return CodeArtifact containing the generated enum
     */
    public CodeArtifact translateStateMachineToEnum(StateMachineDecl stateMachine, String entityName) {
        String enumName = deriveEnumName(stateMachine, entityName);
        String packageName = basePackage + ".model";
        
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("State enumeration for $L.\n", entityName)
                .addJavadoc("Generated from DDSL state machine: $L\n", 
                        stateMachine.forField() != null ? stateMachine.forField() : "status");
        
        // Track initial and final states
        List<String> initialStates = new ArrayList<>();
        List<String> finalStates = new ArrayList<>();
        
        // Add enum constants
        for (StateDecl state : stateMachine.states()) {
            String constantName = toEnumConstant(state.name());
            
            TypeSpec.Builder constantBuilder = TypeSpec.anonymousClassBuilder("");
            if (state.isInitial() || state.isFinal()) {
                // Add javadoc for special states
                StringBuilder doc = new StringBuilder();
                if (state.isInitial()) {
                    doc.append("Initial state. ");
                    initialStates.add(constantName);
                }
                if (state.isFinal()) {
                    doc.append("Final state.");
                    finalStates.add(constantName);
                }
                // Note: Anonymous class builders don't support javadoc, 
                // so we add it as a field comment later
            }
            
            enumBuilder.addEnumConstant(constantName);
            
            if (state.isInitial()) initialStates.add(constantName);
            if (state.isFinal()) finalStates.add(constantName);
        }
        
        // Add isInitial() method
        if (!initialStates.isEmpty()) {
            enumBuilder.addMethod(generateIsInitialMethod(initialStates));
        }
        
        // Add isFinal() method
        if (!finalStates.isEmpty()) {
            enumBuilder.addMethod(generateIsFinalMethod(finalStates));
        }
        
        // Add canTransitionTo() method stub
        enumBuilder.addMethod(generateCanTransitionToMethod(enumName));
        
        TypeSpec enumSpec = enumBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, enumSpec)
                .indent("    ")
                .build();
        
        return new CodeArtifact(
                enumName,
                packageName,
                javaFile.toString(),
                CodeArtifact.ArtifactType.ENUM
        );
    }
    
    /**
     * Generate enum from a list of allowed values (e.g., from field constraints).
     * 
     * @param enumName Name of the enum
     * @param values List of allowed values
     * @param documentation Optional documentation
     * @return CodeArtifact containing the generated enum
     */
    public CodeArtifact translateValuesToEnum(String enumName, List<String> values, String documentation) {
        String packageName = basePackage + ".model";
        
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumName)
                .addModifiers(Modifier.PUBLIC);
        
        if (documentation != null && !documentation.isBlank()) {
            enumBuilder.addJavadoc("$L\n", documentation);
        }
        
        for (String value : values) {
            String constantName = toEnumConstant(value);
            enumBuilder.addEnumConstant(constantName);
        }

        if (values.isEmpty()) {
            enumBuilder.addEnumConstant("UNSPECIFIED");
        }
        
        // Add fromString factory method
        enumBuilder.addMethod(generateFromStringMethod(enumName, values));
        
        TypeSpec enumSpec = enumBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, enumSpec)
                .indent("    ")
                .build();
        
        return new CodeArtifact(
                enumName,
                packageName,
                javaFile.toString(),
                CodeArtifact.ArtifactType.ENUM
        );
    }
    
    // =================================================================================
    // HELPER METHODS
    // =================================================================================
    
    private String deriveEnumName(StateMachineDecl stateMachine, String entityName) {
        if (stateMachine.name() != null && !stateMachine.name().isBlank()) {
            return stateMachine.name() + "State";
        }
        if (stateMachine.forField() != null) {
            return capitalize(stateMachine.forField());
        }
        return entityName + "Status";
    }
    
    private String toEnumConstant(String name) {
        if (name == null || name.isBlank()) {
            return "UNSPECIFIED";
        }

        String normalized = name.trim().replace(' ', '_').replace('-', '_');
        if (normalized.matches("[A-Z][A-Z0-9_]*")) {
            return normalized;
        }

        // Convert camelCase or PascalCase to SCREAMING_SNAKE_CASE
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toUpperCase(c));
        }
        // Also handle spaces and hyphens
        return result.toString()
                .replaceAll("_+", "_");
    }
    
    private MethodSpec generateIsInitialMethod(List<String> initialStates) {
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.add("return ");
        
        for (int i = 0; i < initialStates.size(); i++) {
            if (i > 0) codeBuilder.add(" || ");
            codeBuilder.add("this == $L", initialStates.get(i));
        }
        codeBuilder.add(";\n");
        
        return MethodSpec.methodBuilder("isInitial")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if this is an initial state.\n")
                .addCode(codeBuilder.build())
                .build();
    }
    
    private MethodSpec generateIsFinalMethod(List<String> finalStates) {
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.add("return ");
        
        for (int i = 0; i < finalStates.size(); i++) {
            if (i > 0) codeBuilder.add(" || ");
            codeBuilder.add("this == $L", finalStates.get(i));
        }
        codeBuilder.add(";\n");
        
        return MethodSpec.methodBuilder("isFinal")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if this is a final state.\n")
                .addCode(codeBuilder.build())
                .build();
    }
    
    private MethodSpec generateCanTransitionToMethod(String enumName) {
        return MethodSpec.methodBuilder("canTransitionTo")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.bestGuess(enumName), "target")
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if transition to target state is allowed.\n")
                .addJavadoc("Override this method to implement transition rules.\n")
                .addStatement("return true")
                .build();
    }
    
    private MethodSpec generateFromStringMethod(String enumName, List<String> values) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("fromString")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String.class, "value")
                .returns(ClassName.bestGuess(enumName))
                .addJavadoc("Parse enum from string value.\n")
                .addJavadoc("@param value The string value to parse\n")
                .addJavadoc("@return The corresponding enum constant\n")
                .addJavadoc("@throws IllegalArgumentException if value is not valid\n");
        
        method.beginControlFlow("if (value == null)");
        method.addStatement("throw new $T($S)", IllegalArgumentException.class, "Value cannot be null");
        method.endControlFlow();
        
        method.beginControlFlow("return switch (value.toUpperCase().replace($S, $S).replace($S, $S))", " ", "_", "-", "_");
        for (String value : values) {
            String constant = toEnumConstant(value);
            method.addStatement("case $S -> $L", constant, constant);
        }
        method.addStatement("default -> throw new $T($S + value)", 
                IllegalArgumentException.class, "Unknown value: ");
        method.endControlFlow();
        
        return method.build();
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
