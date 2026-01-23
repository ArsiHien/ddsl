package uet.ndh.ddsl.validator.rules.application;

import lombok.Data;
import uet.ndh.ddsl.core.application.applicationservice.ApplicationService;
import uet.ndh.ddsl.core.application.applicationservice.UseCase;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 8.1: Application Service Has No Business Logic
 * Validates that Application Services only orchestrate and don't contain business logic.
 */
@Data
public class ApplicationServiceNoBusinessLogicRule implements DDDValidationRule {

    private static final String RULE_CODE = "APPLICATION_SERVICE_NO_BUSINESS_LOGIC";
    private static final String RULE_NAME = "Application Service Has No Business Logic";
    private static final String DESCRIPTION = "Application Services must only orchestrate operations (load, call domain, save) and not contain business logic";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getApplicationServices().forEach(service ->
                validateApplicationService(service, errors));
        });

        return errors;
    }

    private void validateApplicationService(ApplicationService service, List<DDDValidationError> errors) {
        // Check use cases for business logic patterns
        for (UseCase useCase : service.getUseCases()) {
            validateUseCase(service, useCase, errors);
        }

        // Check methods for business logic patterns (if any custom methods exist)
        // This would be expanded based on how methods are defined in ApplicationService
    }

    private void validateUseCase(ApplicationService service, UseCase useCase, List<DDDValidationError> errors) {
        // Note: In a real implementation, you'd parse the use case steps or method bodies
        // to look for business logic patterns. Here we provide a framework for such checks.

        // Check for suspicious patterns that might indicate business logic
        String useCaseName = useCase.getName();

        // Simple heuristic checks (these would be more sophisticated in practice)
        if (useCaseName.contains("calculate") || useCaseName.contains("compute")) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Application Service '%s' use case '%s' might contain business logic - calculations should be in domain entities/services",
                    service.getName(), useCaseName),
                service.getLocation(),
                DDDValidationSeverity.WARNING
            ));
        }

        // Check for business validation patterns
        if (useCaseName.contains("validate") && !useCaseName.contains("input")) {
            errors.add(new DDDValidationError(
                RULE_CODE,
                RULE_NAME,
                    DESCRIPTION,
                String.format("Application Service '%s' use case '%s' might contain business validation - domain validation should be in entities/domain services",
                    service.getName(), useCaseName),
                service.getLocation(),
                DDDValidationSeverity.WARNING
            ));
        }

        // In a full implementation, you would:
        // 1. Parse use case steps
        // 2. Look for if/else statements with business rules
        // 3. Check for mathematical operations
        // 4. Identify domain calculations
        // 5. Find business validation logic
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
