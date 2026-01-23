package uet.ndh.ddsl.validator;

import lombok.Getter;
import org.springframework.stereotype.Service;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.validator.rules.DDDValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring-managed validator for domain model correctness and DDD best practices.
 * Updated to include comprehensive DDD tactical design validation.
 */
@Service
public class DomainModelValidator {

    @Getter
    private final DDDTacticalValidator dddValidator;

    public DomainModelValidator() {
        this.dddValidator = new DDDTacticalValidator();
    }

    public List<ValidationError> validate(DomainModel model) {
        List<ValidationError> errors = new ArrayList<>();

        // Delegate to model's basic validation
        errors.addAll(model.validate());

        // Add traditional validation rules
        validateNamingConventions(model, errors);
        validateAggregateReferences(model, errors);
        validatePackageStructure(model, errors);

        // Run comprehensive DDD tactical design validation
        List<DDDValidationError> dddErrors = dddValidator.validate(model);

        // Convert DDD validation errors to standard validation errors
        for (DDDValidationError dddError : dddErrors) {
            ValidationError.Severity severity = convertSeverity(dddError);
            errors.add(new ValidationError(
                String.format("[%s] %s", dddError.getRuleCode(), dddError.getMessage()),
                dddError.getLocation(),
                severity
            ));
        }

        // Print DDD validation summary
        DDDValidationSummary summary = dddValidator.getSummary(dddErrors);
        if (!dddErrors.isEmpty()) {
            System.out.println("\n" + "=".repeat(50));
            summary.printSummary();
            System.out.println("=".repeat(50));
        }

        return errors;
    }

    private ValidationError.Severity convertSeverity(DDDValidationError dddError) {
        return switch (dddError.getSeverity()) {
            case ERROR -> ValidationError.Severity.ERROR;
            case WARNING -> ValidationError.Severity.WARNING;
            default -> ValidationError.Severity.INFO;
        };
    }

    private void validateNamingConventions(DomainModel model, List<ValidationError> errors) {
        // Check that class names follow Java conventions
        model.getBoundedContexts().forEach(context -> {
            context.getAggregates().forEach(aggregate -> {
                if (!isValidJavaClassName(aggregate.getName())) {
                    errors.add(new ValidationError(
                        "Invalid aggregate name: " + aggregate.getName() +
                        ". Must follow Java class naming conventions.",
                        aggregate.getLocation()));
                }
            });

            context.getValueObjects().forEach(vo -> {
                if (!isValidJavaClassName(vo.getName())) {
                    errors.add(new ValidationError(
                        "Invalid value object name: " + vo.getName() +
                        ". Must follow Java class naming conventions.",
                        vo.getLocation()));
                }
            });
        });
    }

    private void validateAggregateReferences(DomainModel model, List<ValidationError> errors) {
        // Check that aggregates only reference other aggregates by ID
        // This is now handled by AggregateReferenceByIdOnlyRule in DDDTacticalValidator
    }

    private void validatePackageStructure(DomainModel model, List<ValidationError> errors) {
        // Validate that package names are valid Java package names
        if (!isValidJavaPackageName(model.getBasePackage())) {
            errors.add(new ValidationError(
                "Invalid base package: " + model.getBasePackage() +
                ". Must be a valid Java package name.",
                model.getLocation()));
        }
    }

    private boolean isValidJavaClassName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Must start with uppercase letter
        if (!Character.isUpperCase(name.charAt(0))) {
            return false;
        }

        // Must contain only valid Java identifier characters
        for (char c : name.toCharArray()) {
            if (!Character.isJavaIdentifierPart(c)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidJavaPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }

        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (part.isEmpty() || !Character.isJavaIdentifierStart(part.charAt(0))) {
                return false;
            }
            for (char c : part.toCharArray()) {
                if (!Character.isJavaIdentifierPart(c)) {
                    return false;
                }
            }
        }

        return true;
    }
}
