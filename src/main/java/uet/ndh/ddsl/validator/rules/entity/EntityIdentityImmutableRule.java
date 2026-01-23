package uet.ndh.ddsl.validator.rules.entity;

import lombok.Data;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 1.3: Identity Immutability
 * Validates that Entity identity field is immutable after creation.
 */
@Data
public class EntityIdentityImmutableRule implements DDDValidationRule {

    private static final String RULE_CODE = "ENTITY_IDENTITY_IMMUTABLE";
    private static final String RULE_NAME = "Entity Identity Immutable";
    private static final String DESCRIPTION = "Entity identity field must be immutable (final) and have no setter method";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate -> {
                // Check aggregate root
                validateIdentityImmutability(aggregate.getRoot(), errors);

                // Check internal entities
                aggregate.getEntities().forEach(entity ->
                    validateIdentityImmutability(entity, errors));
            });
        });

        return errors;
    }

    private void validateIdentityImmutability(Entity entity, List<DDDValidationError> errors) {
        if (entity.getIdentityField() == null) {
            return; // This will be caught by EntityMustHaveIdentityRule
        }

        String identityFieldName = entity.getIdentityField().name();

        // Check if there's a setter method for the identity field
        boolean hasIdentitySetter = entity.getMethods().stream()
            .anyMatch(method -> isSetterForField(method, identityFieldName));

        if (hasIdentitySetter) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Entity '%s' has setter for identity field '%s' - identity must be immutable",
                    entity.getName(), identityFieldName),
                entity.getLocation(),
                DDDValidationSeverity.ERROR
            ));
        }

        // Check if identity field is final (this would be in Field object)
        entity.getFields().stream()
            .filter(field -> field.getName().equals(identityFieldName))
            .forEach(field -> {
                if (!field.isFinal()) {
                    errors.add(new DDDValidationError(
                        RULE_CODE,
                        RULE_NAME,
                        DESCRIPTION,
                        String.format("Entity '%s' identity field '%s' must be final",
                            entity.getName(), identityFieldName),
                        entity.getLocation(),
                        DDDValidationSeverity.ERROR
                    ));
                }
            });
    }

    private boolean isSetterForField(Method method, String fieldName) {
        if (method.getName() == null) return false;

        String expectedSetterName = "set" + capitalize(fieldName);
        return expectedSetterName.equals(method.getName()) &&
               method.getParameters().size() == 1;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
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
