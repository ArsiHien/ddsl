package uet.ndh.ddsl.core.model;

import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.codegen.CodeArtifacts;

import java.util.ArrayList;
import java.util.List;

/**
 * Root node representing the entire domain model.
 * Contains multiple bounded contexts and generates complete code artifacts.
 */
public class DomainModel extends ASTNode {
    private final String modelName;
    private final String basePackage;
    private final List<BoundedContext> boundedContexts;

    public DomainModel(SourceLocation location, String modelName, String basePackage) {
        super(location);
        this.modelName = modelName;
        this.basePackage = basePackage;
        this.boundedContexts = new ArrayList<>();
    }

    public String getModelName() {
        return modelName;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public List<BoundedContext> getBoundedContexts() {
        return new ArrayList<>(boundedContexts);
    }

    public void addBoundedContext(BoundedContext context) {
        boundedContexts.add(context);
    }

    /**
     * Generate code artifacts for the entire domain model.
     * @return Complete code artifacts ready for file system generation
     */
    public CodeArtifacts generateCode() {
        // Implementation will delegate to visitor pattern
        throw new UnsupportedOperationException("To be implemented with visitor pattern");
    }

    /**
     * Create a deep copy of this domain model for normalization.
     * Required for semantic normalization phase.
     */
    public DomainModel copy() {
        DomainModel copy = new DomainModel(this.location, this.modelName, this.basePackage);

        // Copy all bounded contexts
        for (BoundedContext context : this.boundedContexts) {
            copy.addBoundedContext(context.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitDomainModel(this);
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (modelName == null || modelName.trim().isEmpty()) {
            errors.add(new ValidationError("Model name cannot be empty", getLocation()));
        }

        if (basePackage == null || basePackage.trim().isEmpty()) {
            errors.add(new ValidationError("Base package cannot be empty", getLocation()));
        }

        if (boundedContexts.isEmpty()) {
            errors.add(new ValidationError("Domain model must contain at least one bounded context", getLocation()));
        }

        // Validate all bounded contexts
        for (BoundedContext context : boundedContexts) {
            errors.addAll(context.validate());
        }

        return errors;
    }
}
