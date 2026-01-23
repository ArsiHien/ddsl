package uet.ndh.ddsl.core.application.applicationservice;

import lombok.Getter;
import uet.ndh.ddsl.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Application Service in Domain-Driven Design.
 * Application services orchestrate use cases and coordinate domain objects.
 */
public class ApplicationService extends ASTNode {
    @Getter
    private final String name;
    private final List<UseCase> useCases;
    private final List<Dependency> dependencies;

    public ApplicationService(SourceLocation location, String name) {
        super(location);
        this.name = name;
        this.useCases = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public List<UseCase> getUseCases() {
        return new ArrayList<>(useCases);
    }

    public List<Dependency> getDependencies() {
        return new ArrayList<>(dependencies);
    }

    public void addUseCase(UseCase useCase) {
        useCases.add(useCase);
    }

    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitApplicationService(this);
    }

    /**
     * Create a deep copy of this application service for normalization.
     */
    public ApplicationService copy() {
        ApplicationService copy = new ApplicationService(this.location, this.name);

        // Copy all use cases
        for (UseCase useCase : this.useCases) {
            copy.addUseCase(useCase.copy());
        }

        // Copy all dependencies
        for (Dependency dependency : this.dependencies) {
            copy.addDependency(dependency.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Application service name cannot be empty", getLocation()));
        }

        if (useCases.isEmpty()) {
            errors.add(new ValidationError("Application service must have at least one use case", getLocation()));
        }

        // Validate use cases
        for (UseCase useCase : useCases) {
            errors.addAll(useCase.validate(getLocation()));
        }

        // Validate dependencies
        for (Dependency dependency : dependencies) {
            errors.addAll(dependency.validate(getLocation()));
        }

        return errors;
    }
}
