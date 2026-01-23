package uet.ndh.ddsl.core.building.expression;

/**
 * Base class for all expressions in business logic.
 */
public abstract class Expression {

    /**
     * Generate Java code for this expression.
     */
    public abstract String generateCode();

    /**
     * Get the expected Java type of this expression.
     */
    public abstract String getType();

    /**
     * Create a copy of this expression.
     */
    public abstract Expression copy();
}
