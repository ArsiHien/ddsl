package uet.ndh.ddsl.validator.rules.aggregate;

import lombok.Data;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rule 3.3: No Direct References Between Aggregates
 * Validates that aggregates only reference other aggregates by ID.
 */
@Data
public class AggregateReferenceByIdOnlyRule implements DDDValidationRule {

    private static final String RULE_CODE = "AGGREGATE_REFERENCE_BY_ID_ONLY";
    private static final String RULE_NAME = "Aggregate Reference By ID Only";
    private static final String DESCRIPTION = "Aggregates must only reference other aggregates by their ID types, not by direct object reference";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        // First, collect all aggregate entity types
        Set<String> aggregateEntityTypes = collectAggregateEntityTypes(model);

        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate ->
                validateAggregateReferences(aggregate, aggregateEntityTypes, errors));
        });

        return errors;
    }

    private Set<String> collectAggregateEntityTypes(DomainModel model) {
        Set<String> entityTypes = new HashSet<>();

        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate -> {
                // Add root entity type
                entityTypes.add(aggregate.getRoot().getName());

                // Add internal entity types
                aggregate.getEntities().forEach(entity ->
                    entityTypes.add(entity.getName()));
            });
        });

        return entityTypes;
    }

    private void validateAggregateReferences(Aggregate aggregate, Set<String> allEntityTypes, List<DDDValidationError> errors) {
        // Get all entity types within this aggregate
        Set<String> internalEntityTypes = new HashSet<>();
        internalEntityTypes.add(aggregate.getRoot().getName());
        aggregate.getEntities().forEach(entity -> internalEntityTypes.add(entity.getName()));

        // Check root entity
        validateEntityReferences(aggregate.getRoot(), allEntityTypes, internalEntityTypes, aggregate, errors);

        // Check internal entities
        aggregate.getEntities().forEach(entity ->
            validateEntityReferences(entity, allEntityTypes, internalEntityTypes, aggregate, errors));
    }

    private void validateEntityReferences(Entity entity, Set<String> allEntityTypes,
            Set<String> internalEntityTypes, Aggregate aggregate, List<DDDValidationError> errors) {

        for (Field field : entity.getFields()) {
            JavaType fieldType = field.getType();
            if (fieldType == null) continue;

            String typeName = fieldType.getSimpleName();

            // Check if this field references another entity type
            if (allEntityTypes.contains(typeName)) {
                // If it's not an internal entity, it should be an ID reference
                if (!internalEntityTypes.contains(typeName)) {
                    // This is a reference to an external aggregate entity
                    if (!isIdType(typeName)) {
                        errors.add(new DDDValidationError(
                            RULE_CODE,
                            RULE_NAME,
                            DESCRIPTION,
                            String.format("Entity '%s' in aggregate '%s' has direct reference to external aggregate entity '%s' via field '%s' - should reference by ID type (e.g., %sId)",
                                entity.getName(), aggregate.getName(), typeName, field.getName(), typeName),
                            entity.getLocation(),
                            DDDValidationSeverity.ERROR
                        ));
                    }
                }
            }

            // Check collection types for entity references
            if (fieldType.getSimpleName().startsWith("List<") ||
                fieldType.getSimpleName().startsWith("Set<") ||
                fieldType.getSimpleName().startsWith("Collection<")) {

                validateCollectionElementType(field, allEntityTypes, internalEntityTypes,
                    entity, aggregate, errors);
            }
        }
    }

    private void validateCollectionElementType(Field field, Set<String> allEntityTypes,
            Set<String> internalEntityTypes, Entity entity, Aggregate aggregate, List<DDDValidationError> errors) {

        String fieldTypeName = field.getType().getSimpleName();

        // Extract element type from collection (simplified parsing)
        if (fieldTypeName.contains("<") && fieldTypeName.contains(">")) {
            int startIndex = fieldTypeName.indexOf('<') + 1;
            int endIndex = fieldTypeName.lastIndexOf('>');
            String elementType = fieldTypeName.substring(startIndex, endIndex);

            if (allEntityTypes.contains(elementType) && !internalEntityTypes.contains(elementType)) {
                if (!isIdType(elementType)) {
                    errors.add(new DDDValidationError(
                        RULE_CODE,
                        RULE_NAME,
                        DESCRIPTION,
                        String.format("Entity '%s' in aggregate '%s' has collection field '%s' with direct references to external aggregate entity '%s' - should reference by ID type (e.g., %sId)",
                            entity.getName(), aggregate.getName(), field.getName(), elementType, elementType),
                        entity.getLocation(),
                        DDDValidationSeverity.ERROR
                    ));
                }
            }
        }
    }

    private boolean isIdType(String typeName) {
        return typeName.endsWith("Id") ||
               typeName.endsWith("ID") ||
               typeName.equals("UUID") ||
               typeName.equals("Long") ||
               typeName.equals("String") ||
               typeName.equals("Integer");
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
