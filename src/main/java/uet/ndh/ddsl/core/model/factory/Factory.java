package uet.ndh.ddsl.core.model.factory;

import lombok.Getter;
import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.building.Parameter;
import uet.ndh.ddsl.core.building.CodeBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Factory in Domain-Driven Design.
 * Factories encapsulate complex object creation logic.
 */
public class Factory extends ASTNode {
    @Getter
    private final String name;
    @Getter
    private final JavaType createdType;
    private final List<FactoryMethod> methods;

    public Factory(SourceLocation location, String name, JavaType createdType) {
        super(location);
        this.name = name;
        this.createdType = createdType;
        this.methods = new ArrayList<>();
    }

    public List<FactoryMethod> getMethods() {
        return new ArrayList<>(methods);
    }

    public void addMethod(FactoryMethod method) {
        methods.add(method);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitFactory(this);
    }

    /**
     * Create a deep copy of this factory for normalization.
     */
    public Factory copy() {
        Factory copy = new Factory(this.location, this.name, this.createdType);

        // Copy all methods
        for (FactoryMethod method : this.methods) {
            copy.addMethod(method.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Factory name cannot be empty", getLocation()));
        }

        if (createdType == null) {
            errors.add(new ValidationError("Factory created type cannot be null", getLocation()));
        }

        if (methods.isEmpty()) {
            errors.add(new ValidationError("Factory must have at least one method", getLocation()));
        }

        // Validate all methods
        for (FactoryMethod method : methods) {
            errors.addAll(method.validate(getLocation()));
        }

        return errors;
    }
}
