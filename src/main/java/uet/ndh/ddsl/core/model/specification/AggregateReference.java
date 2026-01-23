package uet.ndh.ddsl.core.model.specification;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.building.Field; /**
 * Represents an aggregate reference constraint.
 * Ensures aggregates only reference other aggregates by ID.
 */
public class AggregateReference {
    private final String sourceAggregate;
    private final String targetAggregate;
    private final Field referenceField;
    private final boolean byIdOnly;

    public AggregateReference(String sourceAggregate, String targetAggregate,
                             Field referenceField, boolean byIdOnly) {
        this.sourceAggregate = sourceAggregate;
        this.targetAggregate = targetAggregate;
        this.referenceField = referenceField;
        this.byIdOnly = byIdOnly;
    }

    public String getSourceAggregate() {
        return sourceAggregate;
    }

    public String getTargetAggregate() {
        return targetAggregate;
    }

    public Field getReferenceField() {
        return referenceField;
    }

    public boolean isByIdOnly() {
        return byIdOnly;
    }

    /**
     * Validate this aggregate reference.
     * @return Validation error if invalid, null if valid
     */
    public ValidationError validate(SourceLocation location) {
        if (!byIdOnly) {
            return new ValidationError(
                "Aggregate references must be by ID only: " + sourceAggregate + " -> " + targetAggregate,
                location,
                ValidationError.Severity.ERROR
            );
        }

        if (referenceField == null) {
            return new ValidationError(
                "Aggregate reference must have a reference field",
                location
            );
        }

        return null;
    }
}
