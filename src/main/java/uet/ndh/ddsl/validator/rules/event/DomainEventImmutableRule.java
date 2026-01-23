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
 * Rule 4.1: Domain Event Immutability
 * Validates that Domain Events are immutable.
 */
@Data
public class DomainEventImmutableRule implements DDDValidationRule {

    private static final String RULE_CODE = "DOMAIN_EVENT_IMMUTABLE";
    private static final String RULE_NAME = "Domain Event Immutable";
    private static final String DESCRIPTION = "Domain Events must be immutable - all fields must be final and no setters";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getDomainEvents().forEach(event ->
                validateDomainEventImmutability(event, errors));
        });

        return errors;
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

    private void validateDomainEventImmutability(DomainEvent event, List<DDDValidationError> errors) {
        // Check all fields are final
        for (Field field : event.getFields()) {
            if (!field.isFinal()) {
                errors.add(new DDDValidationError(
                    RULE_CODE,
                    RULE_NAME,
                    DESCRIPTION,
                    String.format("Domain Event '%s' has mutable field '%s' - all event fields must be final",
                        event.getName(), field.getName()),
                    event.getLocation(),
                    DDDValidationSeverity.ERROR
                ));
            }
        }

        // Note: In a full implementation, you would also check for setter methods
        // by examining the event's methods if they exist in the model
    }

}
