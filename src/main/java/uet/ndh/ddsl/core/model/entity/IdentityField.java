package uet.ndh.ddsl.core.model.entity;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Identity field for entities.
 */
public record IdentityField(String name, IdentityType type, IdGenerationStrategy generationStrategy) {

    /**
     * Create a copy of this identity field.
     */
    public IdentityField copy() {
        return new IdentityField(this.name, this.type, this.generationStrategy);
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Identity field name cannot be empty", location));
        }

        if (type == null) {
            errors.add(new ValidationError("Identity field type cannot be null", location));
        }

        return errors;
    }
}
