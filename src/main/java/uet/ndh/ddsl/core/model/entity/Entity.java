package uet.ndh.ddsl.core.model.entity;

import lombok.Getter;
import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Entity in Domain-Driven Design.
 * Entities have identity and lifecycle.
 */
public class Entity extends ASTNode {
    @Getter
    private final String name;
    @Getter
    private final boolean isAggregateRoot;
    @Getter
    private final IdentityField identityField;
    private final List<Field> fields;
    private final List<Method> methods;
    private final List<EventReference> domainEvents;

    public Entity(SourceLocation location, String name, boolean isAggregateRoot, IdentityField identityField) {
        super(location);
        this.name = name;
        this.isAggregateRoot = isAggregateRoot;
        this.identityField = identityField;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
    }

    public List<Field> getFields() {
        return new ArrayList<>(fields);
    }

    public List<Method> getMethods() {
        return new ArrayList<>(methods);
    }

    public List<EventReference> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public void addDomainEvent(EventReference eventRef) {
        domainEvents.add(eventRef);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitEntity(this);
    }

    /**
     * Create a deep copy of this entity for normalization.
     */
    public Entity copy() {
        Entity copy = new Entity(this.location, this.name, this.isAggregateRoot,
                this.identityField.copy());

        // Copy all fields
        for (Field field : this.fields) {
            copy.addField(field.copy());
        }

        // Copy all methods
        for (Method method : this.methods) {
            copy.addMethod(method.copy());
        }

        // Copy all domain events
        for (EventReference eventRef : this.domainEvents) {
            copy.addDomainEvent(eventRef.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Entity name cannot be empty", getLocation()));
        }

        if (identityField == null) {
            errors.add(new ValidationError("Entity must have an identity field", getLocation()));
        } else {
            errors.addAll(identityField.validate(getLocation()));
        }

        // Validate all fields
        for (Field field : fields) {
            errors.addAll(field.validate(getLocation()));
        }

        // Validate all methods
        for (Method method : methods) {
            errors.addAll(method.validate(getLocation()));
        }

        return errors;
    }
}
