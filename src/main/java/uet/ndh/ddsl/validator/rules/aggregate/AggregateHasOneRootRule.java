package uet.ndh.ddsl.validator.rules.aggregate;

import lombok.Data;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 3.1: Aggregate Has One Root
 * Validates that each aggregate has exactly one aggregate root.
 */
@Data
public class AggregateHasOneRootRule implements DDDValidationRule {

    private static final String RULE_CODE = "AGGREGATE_ONE_ROOT";
    private static final String RULE_NAME = "Aggregate Has One Root";
    private static final String DESCRIPTION = "Each aggregate must have exactly one aggregate root entity";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate ->
                validateAggregateHasOneRoot(aggregate, errors));
        });

        return errors;
    }

    private void validateAggregateHasOneRoot(Aggregate aggregate, List<DDDValidationError> errors) {
        Entity root = aggregate.getRoot();

        // Check if aggregate has a root
        if (root == null) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Aggregate '%s' has no aggregate root - each aggregate must have exactly one root entity",
                    aggregate.getName()),
                aggregate.getLocation(),
                DDDValidationSeverity.ERROR
            ));
            return;
        }

        // Check if the root is marked as aggregate root
        if (!root.isAggregateRoot()) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Aggregate '%s' root entity '%s' is not marked as aggregate root",
                    aggregate.getName(), root.getName()),
                aggregate.getLocation(),
                DDDValidationSeverity.ERROR
            ));
        }

        // Check that internal entities are not marked as aggregate roots
        long additionalRoots = aggregate.getEntities().stream()
            .mapToLong(entity -> entity.isAggregateRoot() ? 1 : 0)
            .sum();

        if (additionalRoots > 0) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                DESCRIPTION,
                String.format("Aggregate '%s' has multiple entities marked as aggregate root - only the root entity should be marked as aggregate root",
                    aggregate.getName()),
                aggregate.getLocation(),
                DDDValidationSeverity.ERROR
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
