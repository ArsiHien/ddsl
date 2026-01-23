package uet.ndh.ddsl.core.model;

import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Domain Service in Domain-Driven Design.
 * Domain services contain business logic that doesn't naturally fit within entities or value objects.
 */
public class DomainService extends ASTNode {
    private final String name;
    private final boolean isInterface;
    private final List<Method> methods;

    public DomainService(SourceLocation location, String name, boolean isInterface) {
        super(location);
        this.name = name;
        this.isInterface = isInterface;
        this.methods = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public List<Method> getMethods() {
        return new ArrayList<>(methods);
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitDomainService(this);
    }

    /**
     * Create a deep copy of this domain service for normalization.
     */
    public DomainService copy() {
        DomainService copy = new DomainService(this.location, this.name, this.isInterface);

        // Copy all methods
        for (Method method : this.methods) {
            copy.addMethod(method.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Domain service name cannot be empty", getLocation()));
        }

        if (methods.isEmpty()) {
            errors.add(new ValidationError("Domain service must have at least one method", getLocation()));
        }

        // Validate all methods
        for (Method method : methods) {
            errors.addAll(method.validate(getLocation()));
        }

        return errors;
    }
}
