package uet.ndh.ddsl.validator.rules.entity;

import lombok.Data;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 1.2: Entity Equality by Identity
 * Validates that Entity equals() and hashCode() methods only use identity field.
 */
@Data
public class EntityEqualsByIdentityRule implements DDDValidationRule {

    private static final String RULE_CODE = "ENTITY_EQUALS_BY_IDENTITY";
    private static final String RULE_NAME = "Entity Equality by Identity";
    private static final String DESCRIPTION = "Entity equals() and hashCode() methods must only compare/use identity field, not other attributes";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate -> {
                // Check aggregate root
                validateEntityEquality(aggregate.getRoot(), errors);

                // Check internal entities
                aggregate.getEntities().forEach(entity ->
                    validateEntityEquality(entity, errors));
            });
        });

        return errors;
    }

    private void validateEntityEquality(Entity entity, List<DDDValidationError> errors) {
        // Check if entity has custom equals method
        boolean hasCustomEquals = entity.getMethods().stream()
            .anyMatch(method -> "equals".equals(method.getName()));

        // Check if entity has custom hashCode method
        boolean hasCustomHashCode = entity.getMethods().stream()
            .anyMatch(method -> "hashCode".equals(method.getName()));

        if (hasCustomEquals || hasCustomHashCode) {
            // If they have custom implementations, we should validate they only use identity
            validateEqualsMethod(entity, errors);
            validateHashCodeMethod(entity, errors);
        } else {
            // Warn that they should implement equals/hashCode based on identity
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Entity '%s' should implement equals() and hashCode() methods based on identity field",
                    entity.getName()),
                entity.getLocation(),
                DDDValidationSeverity.WARNING
            ));
        }
    }

    private void validateEqualsMethod(Entity entity, List<DDDValidationError> errors) {
        entity.getMethods().stream()
            .filter(method -> "equals".equals(method.getName()))
            .forEach(method -> {
                // This is a simplified check - in a real implementation,
                // you'd parse the method body to ensure only identity is compared
                String methodBody = method.getBody() != null ? method.getBody().toString() : "";

                // Check if method body contains references to non-identity fields
                entity.getFields().stream()
                    .filter(field -> !field.getName().equals(entity.getIdentityField().name()))
                    .forEach(field -> {
                        if (methodBody.contains(field.getName())) {
                            errors.add(new DDDValidationError(
                                RULE_CODE,
                                RULE_NAME,
                                DESCRIPTION,
                                String.format("Entity '%s' equals() method compares non-identity field '%s'",
                                    entity.getName(), field.getName()),
                                entity.getLocation(),
                                DDDValidationSeverity.ERROR
                            ));
                        }
                    });
            });
    }

    private void validateHashCodeMethod(Entity entity, List<DDDValidationError> errors) {
        entity.getMethods().stream()
            .filter(method -> "hashCode".equals(method.getName()))
            .forEach(method -> {
                // Similar validation for hashCode - should only use identity
                String methodBody = method.getBody() != null ? method.getBody().toString() : "";

                entity.getFields().stream()
                    .filter(field -> !field.getName().equals(entity.getIdentityField().name()))
                    .forEach(field -> {
                        if (methodBody.contains(field.getName())) {
                            errors.add(new DDDValidationError(
                                RULE_CODE,
                                RULE_NAME,
                                DESCRIPTION,
                                String.format("Entity '%s' hashCode() method uses non-identity field '%s'",
                                    entity.getName(), field.getName()),
                                entity.getLocation(),
                                DDDValidationSeverity.ERROR
                            ));
                        }
                    });
            });
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
