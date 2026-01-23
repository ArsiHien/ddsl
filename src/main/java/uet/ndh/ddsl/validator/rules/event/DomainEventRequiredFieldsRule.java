package uet.ndh.ddsl.validator.rules.event;

import lombok.Data;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.DomainEvent;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 4.3 & 4.4: Domain Event Has Timestamp and Aggregate ID
 * Validates that Domain Events contain timestamp and aggregate ID fields.
 */
@Data
public class DomainEventRequiredFieldsRule implements DDDValidationRule {

    private static final String RULE_CODE = "DOMAIN_EVENT_REQUIRED_FIELDS";
    private static final String RULE_NAME = "Domain Event Required Fields";
    private static final String DESCRIPTION = "Domain Events must have timestamp (occurredOn) and aggregate ID fields";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getDomainEvents().forEach(event ->
                validateRequiredFields(event, errors));
        });

        return errors;
    }

    private void validateRequiredFields(DomainEvent event, List<DDDValidationError> errors) {
        boolean hasTimestamp = false;
        boolean hasAggregateId = false;

        for (Field field : event.getFields()) {
            String fieldName = field.getName().toLowerCase();
            String fieldType = field.getType() != null ? field.getType().getSimpleName() : "";

            // Check for timestamp field
            if (fieldName.equals("occurredon") ||
                fieldName.equals("timestamp") ||
                fieldName.equals("eventtime") ||
                fieldType.equals("Instant") ||
                fieldType.equals("LocalDateTime")) {
                hasTimestamp = true;
            }

            // Check for aggregate ID field
            if (fieldName.endsWith("id") ||
                fieldName.endsWith("identifier") ||
                fieldType.equals("UUID")) {
                hasAggregateId = true;
            }
        }

        if (!hasTimestamp) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Domain Event '%s' missing timestamp field - should have 'occurredOn' field of type Instant",
                    event.getName()),
                event.getLocation(),
                DDDValidationSeverity.WARNING
            ));
        }

        if (!hasAggregateId) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Domain Event '%s' missing aggregate ID field - should reference the aggregate that raised the event",
                    event.getName()),
                event.getLocation(),
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
