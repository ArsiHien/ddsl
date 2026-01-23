package uet.ndh.ddsl.validator.rules.event;

import lombok.Data;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.DomainEvent;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Rule 4.2: Domain Event Past Tense Naming
 * Validates that Domain Event names are in past tense.
 */
@Data
public class DomainEventPastTenseNamingRule implements DDDValidationRule {

    private static final String RULE_CODE = "DOMAIN_EVENT_PAST_TENSE";
    private static final String RULE_NAME = "Domain Event Past Tense Naming";
    private static final String DESCRIPTION = "Domain Event names must be in past tense to indicate something that has already happened";

    // Common past tense patterns
    private static final Pattern PAST_TENSE_PATTERN = Pattern.compile(
        ".*(?:ed|Created|Updated|Deleted|Added|Removed|Sent|Received|Processed|Completed|Started|Finished|Cancelled|Approved|Rejected)Event$",
        Pattern.CASE_INSENSITIVE
    );

    // Words that should be avoided (present tense or imperative)
    private static final Pattern PRESENT_TENSE_PATTERN = Pattern.compile(
        ".*(?:Create|Update|Delete|Add|Remove|Send|Receive|Process|Complete|Start|Finish|Cancel|Approve|Reject).*Event$",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getDomainEvents().forEach(event ->
                validateEventNaming(event, errors));
        });

        return errors;
    }

    private void validateEventNaming(DomainEvent event, List<DDDValidationError> errors) {
        String eventName = event.getName();

        // Check if it ends with "Event"
        if (!eventName.endsWith("Event")) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Domain Event '%s' should end with 'Event' suffix", eventName),
                event.getLocation(),
                DDDValidationSeverity.WARNING
            ));
        }

        // Check for present tense patterns that should be avoided
        if (PRESENT_TENSE_PATTERN.matcher(eventName).matches()) {
            String suggestion = suggestPastTenseAlternative(eventName);
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Domain Event '%s' uses present tense - should use past tense like '%s'",
                    eventName, suggestion),
                event.getLocation(),
                DDDValidationSeverity.ERROR
            ));
        }

        // Check if it follows past tense pattern
        else if (!PAST_TENSE_PATTERN.matcher(eventName).matches()) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Domain Event '%s' should use past tense naming (e.g., OrderPlacedEvent, PaymentProcessedEvent)",
                    eventName),
                event.getLocation(),
                DDDValidationSeverity.WARNING
            ));
        }
    }

    private String suggestPastTenseAlternative(String presentTenseName) {
        // Simple transformation suggestions
        return presentTenseName
            .replace("CreateEvent", "CreatedEvent")
            .replace("UpdateEvent", "UpdatedEvent")
            .replace("DeleteEvent", "DeletedEvent")
            .replace("AddEvent", "AddedEvent")
            .replace("RemoveEvent", "RemovedEvent")
            .replace("SendEvent", "SentEvent")
            .replace("ReceiveEvent", "ReceivedEvent")
            .replace("ProcessEvent", "ProcessedEvent")
            .replace("CompleteEvent", "CompletedEvent")
            .replace("StartEvent", "StartedEvent")
            .replace("FinishEvent", "FinishedEvent")
            .replace("CancelEvent", "CancelledEvent")
            .replace("ApproveEvent", "ApprovedEvent")
            .replace("RejectEvent", "RejectedEvent");
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
