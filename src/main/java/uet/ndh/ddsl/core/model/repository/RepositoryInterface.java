package uet.ndh.ddsl.core.model.repository;

import lombok.Getter;
import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.building.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Repository Interface in Domain-Driven Design.
 * Repository interfaces define the contract for persisting and retrieving aggregates.
 */
public class RepositoryInterface extends ASTNode {
    @Getter
    private final String name;
    @Getter
    private final JavaType aggregateType;
    private final JavaType idType;
    private final List<RepositoryMethod> methods;

    public RepositoryInterface(SourceLocation location, String name, JavaType aggregateType, JavaType idType) {
        super(location);
        this.name = name;
        this.aggregateType = aggregateType;
        this.idType = idType;
        this.methods = new ArrayList<>();
    }

    public JavaType getIdType() {
        return idType;
    }

    public List<RepositoryMethod> getMethods() {
        return new ArrayList<>(methods);
    }

    public void addMethod(RepositoryMethod method) {
        methods.add(method);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitRepositoryInterface(this);
    }

    /**
     * Create a deep copy of this repository interface for normalization.
     */
    public RepositoryInterface copy() {
        RepositoryInterface copy = new RepositoryInterface(this.location, this.name,
                this.aggregateType, this.idType);

        // Copy all methods
        for (RepositoryMethod method : this.methods) {
            copy.addMethod(method.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Repository interface name cannot be empty", getLocation()));
        }

        if (aggregateType == null) {
            errors.add(new ValidationError("Repository aggregate type cannot be null", getLocation()));
        }

        if (idType == null) {
            errors.add(new ValidationError("Repository ID type cannot be null", getLocation()));
        }

        if (methods.isEmpty()) {
            errors.add(new ValidationError("Repository interface must have at least one method", getLocation()));
        }

        // Validate all methods
        for (RepositoryMethod method : methods) {
            errors.addAll(method.validate(getLocation()));
        }

        return errors;
    }
}
