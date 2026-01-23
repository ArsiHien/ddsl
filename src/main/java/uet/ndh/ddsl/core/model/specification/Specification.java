package uet.ndh.ddsl.core.model.specification;

import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.building.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Specification in Domain-Driven Design.
 * Specifications encapsulate business rules as composable objects.
 */
public class Specification extends ASTNode {
    private final String name;
    private final JavaType targetType;
    private final Field criteriaField;

    public Specification(SourceLocation location, String name, JavaType targetType, Field criteriaField) {
        super(location);
        this.name = name;
        this.targetType = targetType;
        this.criteriaField = criteriaField;
    }

    public String getName() {
        return name;
    }

    public JavaType getTargetType() {
        return targetType;
    }

    public Field getCriteriaField() {
        return criteriaField;
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitSpecification(this);
    }

    /**
     * Create a deep copy of this specification for normalization.
     */
    public Specification copy() {
        Field copiedCriteriaField = (criteriaField != null) ? criteriaField.copy() : null;
        Specification copy = new Specification(this.location, this.name, this.targetType, copiedCriteriaField);
        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Specification name cannot be empty", getLocation()));
        }

        if (targetType == null) {
            errors.add(new ValidationError("Specification target type cannot be null", getLocation()));
        }

        if (criteriaField != null) {
            errors.addAll(criteriaField.validate(getLocation()));
        }

        return errors;
    }
}
