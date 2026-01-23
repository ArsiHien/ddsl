package uet.ndh.ddsl.core;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Base class for all Abstract Syntax Tree nodes in the DDD DSL.
 * Provides common functionality for source location tracking and validation.
 */
@Getter
public abstract class ASTNode {
    protected final SourceLocation location;
    @Setter
    protected String documentation;

    protected ASTNode(SourceLocation location) {
        this.location = location;
    }

    protected ASTNode(SourceLocation location, String documentation) {
        this.location = location;
        this.documentation = documentation;
    }

    /**
     * Accept a visitor for code generation.
     * @param visitor The code generation visitor
     */
    public abstract void accept(CodeGenVisitor visitor);

    /**
     * Validate this AST node and return any validation errors.
     * @return List of validation errors, empty if valid
     */
    public abstract List<ValidationError> validate();
}
