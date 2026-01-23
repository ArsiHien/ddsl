package uet.ndh.ddsl.core.model.valueobject;

import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Value Object in Domain-Driven Design.
 * Value objects are immutable and defined by their attributes.
 */
public class ValueObject extends ASTNode {
    private final String name;
    private final List<Field> fields;
    private final List<Method> methods;
    private final List<ValidationMethod> validations;

    public ValueObject(SourceLocation location, String name) {
        super(location);
        this.name = name;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.validations = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Field> getFields() {
        return new ArrayList<>(fields);
    }

    public List<Method> getMethods() {
        return new ArrayList<>(methods);
    }

    public List<ValidationMethod> getValidations() {
        return new ArrayList<>(validations);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public void addValidation(ValidationMethod validation) {
        validations.add(validation);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitValueObject(this);
    }

    /**
     * Create a deep copy of this value object for normalization.
     */
    public ValueObject copy() {
        ValueObject copy = new ValueObject(this.location, this.name);

        // Copy all fields
        for (Field field : this.fields) {
            copy.addField(field.copy());
        }

        // Copy all methods
        for (Method method : this.methods) {
            copy.addMethod(method.copy());
        }

        // Copy all validations
        for (ValidationMethod validation : this.validations) {
            copy.addValidation(validation.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Value object name cannot be empty", getLocation()));
        }

        if (fields.isEmpty()) {
            errors.add(new ValidationError("Value object must have at least one field", getLocation()));
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
