package uet.ndh.ddsl.validator.rules.entity;

import lombok.Data;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.entity.IdentityField;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 1.1: Entity Must Have Identity
 * Validates that every Entity has exactly one identity field.
 */
@Data
public class EntityMustHaveIdentityRule implements DDDValidationRule {

    private static final String RULE_CODE = "ENTITY_MUST_HAVE_IDENTITY";
    private static final String RULE_NAME = "Entity Must Have Identity";
    private static final String DESCRIPTION = "Every Entity must have exactly one identity field that is non-null and immutable";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate -> {
                // Check aggregate root
                validateEntityIdentity(aggregate.getRoot(), errors);

                // Check internal entities
                aggregate.getEntities().forEach(entity ->
                    validateEntityIdentity(entity, errors));
            });
        });

        return errors;
    }

    private void validateEntityIdentity(Entity entity, List<DDDValidationError> errors) {
        IdentityField identityField = entity.getIdentityField();

        if (identityField == null) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Entity '%s' missing identity field", entity.getName()),
                entity.getLocation(),
                DDDValidationSeverity.ERROR
            ));
            return;
        }

        // Validate identity field is not nullable
        if (identityField.name() == null || identityField.name().trim().isEmpty()) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Entity '%s' has invalid identity field name", entity.getName()),
                entity.getLocation(),
                DDDValidationSeverity.ERROR
            ));
        }

        // Identity should be immutable (this would be enforced in code generation)
        // We can check if there are any setters for the identity field
    }

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}

