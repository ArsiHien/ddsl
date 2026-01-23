package uet.ndh.ddsl.core.model.aggregate;

import lombok.Getter;
import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.factory.Factory;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Aggregate in Domain-Driven Design.
 * An aggregate is a cluster of domain objects treated as a single unit.
 */
public class Aggregate extends ASTNode {
    @Getter
    private final String name;
    @Getter
    private final Entity root;
    private final List<Entity> entities;
    private final List<ValueObject> valueObjects;
    private final List<Invariant> invariants;
    private final List<Factory> factories;

    public Aggregate(SourceLocation location, String name, Entity root) {
        super(location);
        this.name = name;
        this.root = root;
        this.entities = new ArrayList<>();
        this.valueObjects = new ArrayList<>();
        this.invariants = new ArrayList<>();
        this.factories = new ArrayList<>();
    }

    public List<Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    public List<ValueObject> getValueObjects() {
        return new ArrayList<>(valueObjects);
    }

    public List<Invariant> getInvariants() {
        return new ArrayList<>(invariants);
    }

    public List<Factory> getFactories() {
        return new ArrayList<>(factories);
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void addValueObject(ValueObject valueObject) {
        valueObjects.add(valueObject);
    }

    public void addInvariant(Invariant invariant) {
        invariants.add(invariant);
    }

    public void addFactory(Factory factory) {
        factories.add(factory);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitAggregate(this);
    }

    /**
     * Create a deep copy of this aggregate for normalization.
     */
    public Aggregate copy() {
        Aggregate copy = new Aggregate(this.location, this.name, this.root.copy());

        // Copy all entities
        for (Entity entity : this.entities) {
            copy.addEntity(entity.copy());
        }

        // Copy all value objects
        for (ValueObject valueObject : this.valueObjects) {
            copy.addValueObject(valueObject.copy());
        }

        // Copy all invariants
        for (Invariant invariant : this.invariants) {
            copy.addInvariant(invariant.copy());
        }

        // Copy all factories
        for (Factory factory : this.factories) {
            copy.addFactory(factory.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Aggregate name cannot be empty", getLocation()));
        }

        if (root == null) {
            errors.add(new ValidationError("Aggregate must have a root entity", getLocation()));
        } else {
            if (!root.isAggregateRoot()) {
                errors.add(new ValidationError("Aggregate root entity must be marked as aggregate root", getLocation()));
            }
            errors.addAll(root.validate());
        }

        // Validate all contained entities
        for (Entity entity : entities) {
            if (entity.isAggregateRoot()) {
                errors.add(new ValidationError("Non-root entities in aggregate cannot be aggregate roots", getLocation()));
            }
            errors.addAll(entity.validate());
        }

        // Validate value objects
        for (ValueObject vo : valueObjects) {
            errors.addAll(vo.validate());
        }

        // Validate invariants
        for (Invariant invariant : invariants) {
            errors.addAll(invariant.validate(getLocation()));
        }

        return errors;
    }
}
