package uet.ndh.ddsl.ast.application;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Represents a step within a use case.
 * Pure data record.
 */
public record UseCaseStepDecl(
    SourceSpan span,
    int order,
    String description,
    String action,
    StepType type
) {
    
    /**
     * Type of use case step.
     */
    public enum StepType {
        VALIDATE,
        LOAD,
        EXECUTE,
        SAVE,
        PUBLISH,
        RETURN
    }
}
