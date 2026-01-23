package uet.ndh.ddsl.core.model;

import lombok.Getter;
import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.building.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Domain Event in Domain-Driven Design.
 * Domain events represent something significant that happened in the domain.
 */
public class DomainEvent extends ASTNode {
    @Getter
    private final String name;
    @Getter
    private final Field aggregateId;
    @Getter
    private final Field occurredOn;
    private final List<Field> fields;

    public DomainEvent(SourceLocation location, String name, Field aggregateId, Field occurredOn) {
        super(location);
        this.name = name;
        this.aggregateId = aggregateId;
        this.occurredOn = occurredOn;
        this.fields = new ArrayList<>();
    }

    public List<Field> getFields() {
        return new ArrayList<>(fields);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitDomainEvent(this);
    }

    /**
     * Create a deep copy of this domain event for normalization.
     */
    public DomainEvent copy() {
        DomainEvent copy = new DomainEvent(this.location, this.name,
                this.aggregateId.copy(), this.occurredOn.copy());

        // Copy all fields
        for (Field field : this.fields) {
            copy.addField(field.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Domain event name cannot be empty", getLocation()));
        }

        if (aggregateId == null) {
            errors.add(new ValidationError("Domain event must have an aggregate ID field", getLocation()));
        } else {
            errors.addAll(aggregateId.validate(getLocation()));
        }

        if (occurredOn == null) {
            errors.add(new ValidationError("Domain event must have an occurred on field", getLocation()));
        } else {
            errors.addAll(occurredOn.validate(getLocation()));
        }

        // Validate all fields
        for (Field field : fields) {
            errors.addAll(field.validate(getLocation()));
        }

        return errors;
    }
}
