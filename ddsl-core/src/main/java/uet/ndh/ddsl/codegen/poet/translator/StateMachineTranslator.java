package uet.ndh.ddsl.codegen.poet.translator;

import com.palantir.javapoet.*;
import uet.ndh.ddsl.ast.model.statemachine.*;
import uet.ndh.ddsl.ast.stmt.Stmt;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.TypeMapper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Translates DDSL StateMachineDecl to JavaPoet TypeSpecs.
 * 
 * Generates:
 * - State enum with initial/final markers
 * - Transition methods with guard validation
 * - On-entry and on-exit hooks
 * - State validation logic
 * 
 * Example transformation:
 * <pre>
 * DDSL:
 *   state machine for status {
 *       states:
 *           - Pending (initial)
 *           - Confirmed
 *           - Completed (final)
 *       transitions:
 *           - Pending -> Confirmed: when payment received
 *           - Confirmed -> Completed: always
 *       guards:
 *           - cannot transition from Completed to any: always
 *       on entry:
 *           - entering Confirmed: record confirmed at as now
 *   }
 * 
 * Generated Java:
 *   public enum OrderStatus { PENDING, CONFIRMED, COMPLETED; ... }
 *   
 *   public void transitionTo(OrderStatus newStatus) {
 *       validateTransition(this.status, newStatus);
 *       executeOnExit(this.status);
 *       this.status = newStatus;
 *       executeOnEntry(newStatus);
 *   }
 * </pre>
 */
public class StateMachineTranslator {
    
    private final TypeMapper typeMapper;
    private final ExpressionTranslator expressionTranslator;
    private final EnumTranslator enumTranslator;
    private final String basePackage;
    
    public StateMachineTranslator(TypeMapper typeMapper, String basePackage) {
        this.typeMapper = typeMapper;
        this.expressionTranslator = new ExpressionTranslator(typeMapper);
        this.enumTranslator = new EnumTranslator(typeMapper, basePackage);
        this.basePackage = basePackage;
    }
    
    // =================================================================================
    // MAIN TRANSLATION
    // =================================================================================
    
    /**
     * Translate a state machine to multiple artifacts:
     * 1. State enum
     * 2. State machine helper class (optional, for complex machines)
     */
    public List<CodeArtifact> translateStateMachine(StateMachineDecl stateMachine, String entityName) {
        List<CodeArtifact> artifacts = new ArrayList<>();
        
        // Generate state enum
        CodeArtifact stateEnum = enumTranslator.translateStateMachineToEnum(stateMachine, entityName);
        artifacts.add(stateEnum);
        
        // Generate state machine behavior methods (to be added to entity)
        // These are returned as a separate artifact with method definitions
        if (hasComplexBehavior(stateMachine)) {
            CodeArtifact helperClass = generateStateMachineHelper(stateMachine, entityName, stateEnum.typeName());
            artifacts.add(helperClass);
        }
        
        return artifacts;
    }
    
    /**
     * Generate methods to be added to the entity class for state machine behavior.
     * Returns a list of MethodSpecs that should be added to the entity.
     */
    public List<MethodSpec> generateStateMachineMethods(StateMachineDecl stateMachine, String enumTypeName) {
        List<MethodSpec> methods = new ArrayList<>();
        
        // Generate transitionTo method
        methods.add(generateTransitionMethod(stateMachine, enumTypeName));
        
        // Generate canTransitionTo method
        methods.add(generateCanTransitionMethod(stateMachine, enumTypeName));
        
        // Generate validateTransition method (private)
        methods.add(generateValidateTransitionMethod(stateMachine, enumTypeName));
        
        // Generate on-entry handler
        if (!stateMachine.onEntryRules().isEmpty()) {
            methods.add(generateOnEntryMethod(stateMachine, enumTypeName));
        }
        
        // Generate on-exit handler
        if (!stateMachine.onExitRules().isEmpty()) {
            methods.add(generateOnExitMethod(stateMachine, enumTypeName));
        }
        
        return methods;
    }
    
    // =================================================================================
    // TRANSITION METHOD
    // =================================================================================
    
    private MethodSpec generateTransitionMethod(StateMachineDecl stateMachine, String enumTypeName) {
        ClassName stateType = ClassName.bestGuess(enumTypeName);
        String fieldName = stateMachine.forField() != null ? stateMachine.forField() : "status";
        
        MethodSpec.Builder method = MethodSpec.methodBuilder("transitionTo")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(stateType, "newState")
                .addJavadoc("Transition to a new state.\n")
                .addJavadoc("@param newState The target state\n")
                .addJavadoc("@throws IllegalStateException if transition is not allowed\n");
        
        // Validate transition
        method.addStatement("validateTransition(this.$L, newState)", fieldName);
        
        // Execute on-exit hooks
        if (!stateMachine.onExitRules().isEmpty()) {
            method.addStatement("executeOnExit(this.$L)", fieldName);
        }
        
        // Perform transition
        method.addStatement("this.$L = newState", fieldName);
        
        // Execute on-entry hooks
        if (!stateMachine.onEntryRules().isEmpty()) {
            method.addStatement("executeOnEntry(newState)");
        }
        
        return method.build();
    }
    
    // =================================================================================
    // CAN TRANSITION CHECK
    // =================================================================================
    
    private MethodSpec generateCanTransitionMethod(StateMachineDecl stateMachine, String enumTypeName) {
        ClassName stateType = ClassName.bestGuess(enumTypeName);
        String fieldName = stateMachine.forField() != null ? stateMachine.forField() : "status";
        
        MethodSpec.Builder method = MethodSpec.methodBuilder("canTransitionTo")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(stateType, "targetState")
                .returns(TypeName.BOOLEAN)
                .addJavadoc("Check if transition to target state is allowed from current state.\n")
                .addJavadoc("@param targetState The target state to check\n")
                .addJavadoc("@return true if transition is allowed\n");
        
        // Check guards
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("$T currentState = this.$L;\n", stateType, fieldName);
        
        // Check transition rules
        if (!stateMachine.transitions().isEmpty()) {
            code.beginControlFlow("return switch (currentState)");
            
            // Group transitions by source state
            for (StateDecl state : stateMachine.states()) {
                String constantName = toEnumConstant(state.name());
                List<TransitionRule> fromThisState = stateMachine.transitions().stream()
                        .filter(t -> t.matchesSource(state.name()))
                        .toList();
                
                if (!fromThisState.isEmpty()) {
                    code.add("case $L -> ", constantName);
                    generateTransitionCheck(code, fromThisState, stateType);
                }
            }
            
            code.addStatement("default -> false");
            code.endControlFlow();
        } else {
            code.addStatement("return true"); // Allow all transitions if none specified
        }
        
        method.addCode(code.build());
        return method.build();
    }
    
    private void generateTransitionCheck(CodeBlock.Builder code, List<TransitionRule> transitions, ClassName stateType) {
        if (transitions.size() == 1) {
            TransitionRule t = transitions.get(0);
            if (t.condition().type() == TransitionRule.TransitionCondition.ConditionType.ALWAYS) {
                code.add("targetState == $T.$L;\n", stateType, toEnumConstant(t.targetState()));
            } else if (t.condition().type() == TransitionRule.TransitionCondition.ConditionType.NEVER) {
                code.add("false;\n");
            } else {
                code.add("targetState == $T.$L;\n", stateType, toEnumConstant(t.targetState()));
            }
        } else {
            // Multiple possible transitions
            code.add("$T.asList(", java.util.Arrays.class);
            for (int i = 0; i < transitions.size(); i++) {
                if (i > 0) code.add(", ");
                code.add("$T.$L", stateType, toEnumConstant(transitions.get(i).targetState()));
            }
            code.add(").contains(targetState);\n");
        }
    }
    
    // =================================================================================
    // VALIDATE TRANSITION (WITH GUARDS)
    // =================================================================================
    
    private MethodSpec generateValidateTransitionMethod(StateMachineDecl stateMachine, String enumTypeName) {
        ClassName stateType = ClassName.bestGuess(enumTypeName);
        
        MethodSpec.Builder method = MethodSpec.methodBuilder("validateTransition")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(stateType, "fromState")
                .addParameter(stateType, "toState")
                .addJavadoc("Validate that a transition is allowed.\n")
                .addJavadoc("@throws IllegalStateException if transition is not allowed\n");
        
        // Check from final state
        method.beginControlFlow("if (fromState.isFinal())")
                .addStatement("throw new $T($S + fromState)", 
                        IllegalStateException.class, "Cannot transition from final state: ")
                .endControlFlow();
        
        // Apply guards
        for (GuardRule guard : stateMachine.guards()) {
            method.addCode(generateGuardCheck(guard, stateType));
        }
        
        // Check if transition is defined
        method.beginControlFlow("if (!canTransitionTo(toState))")
                .addStatement("throw new $T($S + fromState + $S + toState)",
                        IllegalStateException.class, "Invalid transition from ", " to ")
                .endControlFlow();
        
        return method.build();
    }
    
    private CodeBlock generateGuardCheck(GuardRule guard, ClassName stateType) {
        CodeBlock.Builder code = CodeBlock.builder();
        
        String fromConstant = toEnumConstant(guard.fromState());
        String toConstant = toEnumConstant(guard.toState());
        
        if (guard.type() == GuardRule.GuardType.CANNOT) {
            code.beginControlFlow("if (fromState == $T.$L && toState == $T.$L)",
                    stateType, fromConstant, stateType, toConstant);
            
            // Add condition check if present
            if (guard.condition() != null) {
                CodeBlock conditionCode = expressionTranslator.translateCondition(guard.condition());
                code.beginControlFlow("if ($L)", conditionCode);
            }
            
            code.addStatement("throw new $T($S)", 
                    IllegalStateException.class, 
                    "Cannot transition from " + guard.fromState() + " to " + guard.toState());
            
            if (guard.condition() != null) {
                code.endControlFlow();
            }
            code.endControlFlow();
        } else { // MUST
            code.beginControlFlow("if (fromState == $T.$L && toState != $T.$L)",
                    stateType, fromConstant, stateType, toConstant);
            
            if (guard.condition() != null) {
                CodeBlock conditionCode = expressionTranslator.translateCondition(guard.condition());
                code.beginControlFlow("if ($L)", conditionCode);
                code.addStatement("throw new $T($S)",
                        IllegalStateException.class,
                        "Must transition from " + guard.fromState() + " to " + guard.toState());
                code.endControlFlow();
            }
            code.endControlFlow();
        }
        
        return code.build();
    }
    
    // =================================================================================
    // ON-ENTRY / ON-EXIT HOOKS
    // =================================================================================
    
    private MethodSpec generateOnEntryMethod(StateMachineDecl stateMachine, String enumTypeName) {
        ClassName stateType = ClassName.bestGuess(enumTypeName);
        
        MethodSpec.Builder method = MethodSpec.methodBuilder("executeOnEntry")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(stateType, "state")
                .addJavadoc("Execute on-entry actions for a state.\n");
        
        CodeBlock.Builder switchCode = CodeBlock.builder();
        switchCode.beginControlFlow("switch (state)");
        
        for (OnEntryRule onEntry : stateMachine.onEntryRules()) {
            String constantName = toEnumConstant(onEntry.stateName());
            switchCode.add("case $L:\n", constantName);
            switchCode.indent();
            
            // Generate statements
            for (Stmt stmt : onEntry.statements()) {
                switchCode.addStatement("// TODO: $L", stmt.getClass().getSimpleName());
            }
            switchCode.addStatement("break");
            switchCode.unindent();
        }
        
        switchCode.add("default:\n");
        switchCode.indent();
        switchCode.addStatement("// No action");
        switchCode.addStatement("break");
        switchCode.unindent();
        
        switchCode.endControlFlow();
        
        method.addCode(switchCode.build());
        
        return method.build();
    }
    
    private MethodSpec generateOnExitMethod(StateMachineDecl stateMachine, String enumTypeName) {
        ClassName stateType = ClassName.bestGuess(enumTypeName);
        
        MethodSpec.Builder method = MethodSpec.methodBuilder("executeOnExit")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(stateType, "state")
                .addJavadoc("Execute on-exit actions for a state.\n");
        
        CodeBlock.Builder switchCode = CodeBlock.builder();
        switchCode.beginControlFlow("switch (state)");
        
        for (OnExitRule onExit : stateMachine.onExitRules()) {
            String constantName = toEnumConstant(onExit.stateName());
            switchCode.add("case $L:\n", constantName);
            switchCode.indent();
            
            // Generate statements
            for (Stmt stmt : onExit.statements()) {
                switchCode.addStatement("// TODO: $L", stmt.getClass().getSimpleName());
            }
            switchCode.addStatement("break");
            switchCode.unindent();
        }
        
        switchCode.add("default:\n");
        switchCode.indent();
        switchCode.addStatement("// No action");
        switchCode.addStatement("break");
        switchCode.unindent();
        
        switchCode.endControlFlow();
        
        method.addCode(switchCode.build());
        
        return method.build();
    }
    
    // =================================================================================
    // HELPER CLASS GENERATION
    // =================================================================================
    
    private boolean hasComplexBehavior(StateMachineDecl stateMachine) {
        return !stateMachine.guards().isEmpty() 
                || !stateMachine.onEntryRules().isEmpty()
                || !stateMachine.onExitRules().isEmpty()
                || stateMachine.transitions().size() > 5;
    }
    
    private CodeArtifact generateStateMachineHelper(StateMachineDecl stateMachine, 
                                                     String entityName, String enumTypeName) {
        String className = entityName + "StateMachine";
        String packageName = basePackage + ".model";
        ClassName stateType = ClassName.bestGuess(enumTypeName);
        ClassName entityType = ClassName.get(packageName, entityName);
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("State machine helper for $L.\n", entityName)
                .addJavadoc("Provides transition validation and lifecycle hooks.\n");
        
        // Add entity reference
        classBuilder.addField(FieldSpec.builder(entityType, "entity", Modifier.PRIVATE, Modifier.FINAL)
                .build());
        
        // Add constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(entityType, "entity")
                .addStatement("this.entity = entity")
                .build());
        
        // Add state machine methods
        List<MethodSpec> methods = generateStateMachineMethods(stateMachine, enumTypeName);
        for (MethodSpec method : methods) {
            classBuilder.addMethod(method);
        }
        
        TypeSpec classSpec = classBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, classSpec)
                .indent("    ")
                .build();
        
        return new CodeArtifact(
                className,
                packageName,
                javaFile.toString(),
                CodeArtifact.ArtifactType.DOMAIN_SERVICE
        );
    }
    
    // =================================================================================
    // UTILITIES
    // =================================================================================
    
    private String toEnumConstant(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toUpperCase(c));
        }
        return result.toString()
                .replace(' ', '_')
                .replace('-', '_')
                .replaceAll("_+", "_");
    }
}
