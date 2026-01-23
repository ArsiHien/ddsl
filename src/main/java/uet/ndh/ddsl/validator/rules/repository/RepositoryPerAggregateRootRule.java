package uet.ndh.ddsl.validator.rules.repository;

import lombok.Data;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.repository.RepositoryInterface;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rule 5.1: Repository Per Aggregate Root Only
 * Validates that repositories exist only for aggregate root entities.
 */
@Data
public class RepositoryPerAggregateRootRule implements DDDValidationRule {

    private static final String RULE_CODE = "REPOSITORY_PER_AGGREGATE_ROOT";
    private static final String RULE_NAME = "Repository Per Aggregate Root Only";
    private static final String DESCRIPTION = "Repositories must only exist for aggregate root entities, not for internal entities or value objects";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        // Collect all aggregate root types and internal entity types
        Set<String> aggregateRootTypes = new HashSet<>();
        Set<String> internalEntityTypes = new HashSet<>();
        Set<String> valueObjectTypes = new HashSet<>();

        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate -> {
                aggregateRootTypes.add(aggregate.getRoot().getName());
                aggregate.getEntities().forEach(entity ->
                    internalEntityTypes.add(entity.getName()));
            });

            context.getValueObjects().forEach(vo ->
                valueObjectTypes.add(vo.getName()));
        });

        // Validate repositories
        model.getBoundedContexts().forEach(context -> {
            context.getRepositories().forEach(repo ->
                validateRepository(repo, aggregateRootTypes, internalEntityTypes, valueObjectTypes, errors));
        });

        return errors;
    }

    private void validateRepository(RepositoryInterface repository, Set<String> aggregateRootTypes,
            Set<String> internalEntityTypes, Set<String> valueObjectTypes, List<DDDValidationError> errors) {

        String aggregateType = repository.getAggregateType() != null ?
            repository.getAggregateType().getSimpleName() : null;

        if (aggregateType == null) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Repository '%s' does not specify an aggregate type",
                    repository.getName()),
                repository.getLocation(),
                DDDValidationSeverity.ERROR
            ));
            return;
        }

        // Check if repository is for internal entity (should not exist)
        if (internalEntityTypes.contains(aggregateType)) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Repository '%s' exists for internal entity '%s' - repositories should only exist for aggregate roots",
                    repository.getName(), aggregateType),
                repository.getLocation(),
                DDDValidationSeverity.ERROR
            ));
        }

        // Check if repository is for value object (should not exist)
        if (valueObjectTypes.contains(aggregateType)) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Repository '%s' exists for value object '%s' - repositories should not exist for value objects",
                    repository.getName(), aggregateType),
                repository.getLocation(),
                DDDValidationSeverity.ERROR
            ));
        }

        // Check if repository is for aggregate root (should exist)
        if (!aggregateRootTypes.contains(aggregateType) &&
            !internalEntityTypes.contains(aggregateType) &&
            !valueObjectTypes.contains(aggregateType)) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Repository '%s' references unknown entity type '%s'",
                    repository.getName(), aggregateType),
                repository.getLocation(),
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
