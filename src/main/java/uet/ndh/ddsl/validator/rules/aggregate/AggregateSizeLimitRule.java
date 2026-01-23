package uet.ndh.ddsl.validator.rules.aggregate;

import lombok.Data;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 3.6: Keep Aggregates Small
 * Validates that aggregates don't become too large and complex.
 */
@Data
public class AggregateSizeLimitRule implements DDDValidationRule {

    private static final String RULE_CODE = "AGGREGATE_SIZE_LIMIT";
    private static final String RULE_NAME = "Aggregate Size Limit";
    private static final String DESCRIPTION = "Aggregates should be kept small to maintain manageable complexity";

    // Configurable thresholds
    private static final int MAX_ENTITIES_PER_AGGREGATE = 5;
    private static final int MAX_FIELDS_PER_ROOT = 15;
    private static final int MAX_INVARIANTS_PER_AGGREGATE = 8;

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate ->
                validateAggregateSize(aggregate, errors));
        });

        return errors;
    }

    private void validateAggregateSize(Aggregate aggregate, List<DDDValidationError> errors) {
        // Check number of entities
        int entityCount = 1 + aggregate.getEntities().size(); // +1 for root
        if (entityCount > MAX_ENTITIES_PER_AGGREGATE) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Aggregate '%s' has %d entities (max recommended: %d) - consider splitting into smaller aggregates",
                    aggregate.getName(), entityCount, MAX_ENTITIES_PER_AGGREGATE),
                aggregate.getLocation(),
                DDDValidationSeverity.WARNING
            ));
        }

        // Check number of fields in root entity
        if (aggregate.getRoot() != null) {
            int fieldCount = aggregate.getRoot().getFields().size();
            if (fieldCount > MAX_FIELDS_PER_ROOT) {
                errors.add(new DDDValidationError(
                    RULE_CODE,
                    RULE_NAME,
                    DESCRIPTION,
                    String.format("Aggregate root '%s' has %d fields (max recommended: %d) - too complex, consider extracting value objects",
                        aggregate.getRoot().getName(), fieldCount, MAX_FIELDS_PER_ROOT),
                    aggregate.getLocation(),
                    DDDValidationSeverity.WARNING
                ));
            }
        }

        // Check number of invariants
        int invariantCount = aggregate.getInvariants().size();
        if (invariantCount > MAX_INVARIANTS_PER_AGGREGATE) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Aggregate '%s' has %d invariants (max recommended: %d) - too many rules to maintain",
                    aggregate.getName(), invariantCount, MAX_INVARIANTS_PER_AGGREGATE),
                aggregate.getLocation(),
                DDDValidationSeverity.WARNING
            ));
        }
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
