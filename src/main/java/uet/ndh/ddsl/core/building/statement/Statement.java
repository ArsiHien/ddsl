package uet.ndh.ddsl.core.building.statement;

/**
 * Base class for all statements in business logic.
 */
public abstract class Statement {

    /**
     * Generate Java code for this statement.
     */
    public abstract String generateCode();

    /**
     * Create a copy of this statement.
     */
    public abstract Statement copy();
}
