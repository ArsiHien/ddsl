package uet.ndh.ddsl.core.application.usecase;

import uet.ndh.ddsl.core.building.CodeBlock;

/**
 * Base class for all use case steps.
 */
public abstract class UseCaseStep {
    private final int order;

    public UseCaseStep(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    /**
     * Generate code for this step.
     * @return Code block representing this step
     */
    public abstract CodeBlock generateCode();

    /**
     * Create a copy of this use case step.
     * @return A copy of this step
     */
    public abstract UseCaseStep copy();
}
