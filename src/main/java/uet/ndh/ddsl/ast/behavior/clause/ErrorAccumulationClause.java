package uet.ndh.ddsl.ast.behavior.clause;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;

import java.util.List;

/**
 * Represents an error accumulation block for collecting validation errors.
 * 
 * Syntax:
 * <pre>
 *     collect all errors:
 *         - condition, otherwise "error message"
 *         - condition, warning "warning message"
 *     fail if any errors
 *     
 *     collect up to N errors:
 *         - validations
 *     return errors if any
 *     
 *     collect errors by group:
 *         groupName errors:
 *             - validations
 *     fail if any errors
 * </pre>
 */
public record ErrorAccumulationClause(
    SourceSpan span,
    AccumulationType type,
    int maxErrors,                          // For COLLECT_UP_TO
    List<ValidationRule> validationRules,   // For COLLECT_ALL, COLLECT_UP_TO
    List<GroupedValidation> groupedValidations,  // For COLLECT_BY_GROUP
    ReturnErrorsType returnType
) implements Clause {
    
    public ErrorAccumulationClause {
        validationRules = validationRules != null ? List.copyOf(validationRules) : List.of();
        groupedValidations = groupedValidations != null ? List.copyOf(groupedValidations) : List.of();
    }
    
    @Override
    public ClauseType type() {
        return ClauseType.REQUIRE;  // Acts as an enhanced require clause
    }
    
    /**
     * Factory for "collect all errors".
     */
    public static ErrorAccumulationClause collectAll(SourceSpan span, 
                                                       List<ValidationRule> rules,
                                                       ReturnErrorsType returnType) {
        return new ErrorAccumulationClause(span, AccumulationType.COLLECT_ALL, 
            Integer.MAX_VALUE, rules, List.of(), returnType);
    }
    
    /**
     * Factory for "collect up to N errors".
     */
    public static ErrorAccumulationClause collectUpTo(SourceSpan span, int maxErrors,
                                                        List<ValidationRule> rules,
                                                        ReturnErrorsType returnType) {
        return new ErrorAccumulationClause(span, AccumulationType.COLLECT_UP_TO, 
            maxErrors, rules, List.of(), returnType);
    }
    
    /**
     * Factory for "collect errors by group".
     */
    public static ErrorAccumulationClause collectByGroup(SourceSpan span,
                                                           List<GroupedValidation> groups,
                                                           ReturnErrorsType returnType) {
        return new ErrorAccumulationClause(span, AccumulationType.COLLECT_BY_GROUP,
            Integer.MAX_VALUE, List.of(), groups, returnType);
    }
    
    /**
     * Type of error accumulation.
     */
    public enum AccumulationType {
        COLLECT_ALL,        // collect all errors
        COLLECT_UP_TO,      // collect up to N errors
        COLLECT_BY_GROUP    // collect errors by group
    }
    
    /**
     * How to handle collected errors.
     */
    public enum ReturnErrorsType {
        RETURN_ALL_ERRORS,      // return all errors
        RETURN_ERRORS_IF_ANY,   // return errors if any
        FAIL_IF_ANY_ERRORS,     // fail if any errors
        FAIL_IF_CRITICAL        // fail if critical errors
    }
    
    /**
     * A single validation rule with optional error/warning message.
     */
    public record ValidationRule(
        SourceSpan span,
        NaturalLanguageCondition condition,
        String message,
        MessageType messageType
    ) {
        
        /**
         * Factory for error validation.
         */
        public static ValidationRule error(SourceSpan span, NaturalLanguageCondition condition, 
                                            String message) {
            return new ValidationRule(span, condition, message, MessageType.ERROR);
        }
        
        /**
         * Factory for warning validation.
         */
        public static ValidationRule warning(SourceSpan span, NaturalLanguageCondition condition,
                                              String message) {
            return new ValidationRule(span, condition, message, MessageType.WARNING);
        }
        
        /**
         * Factory for "otherwise" error.
         */
        public static ValidationRule otherwise(SourceSpan span, NaturalLanguageCondition condition,
                                                String message) {
            return new ValidationRule(span, condition, message, MessageType.OTHERWISE);
        }
        
        /**
         * Type of validation message.
         */
        public enum MessageType {
            ERROR,      // error "message"
            WARNING,    // warning "message"
            OTHERWISE   // otherwise "message"
        }
    }
    
    /**
     * A group of validations with a group name.
     */
    public record GroupedValidation(
        SourceSpan span,
        String groupName,
        List<ValidationRule> validations
    ) {
        public GroupedValidation {
            validations = validations != null ? List.copyOf(validations) : List.of();
        }
    }
}
