package uet.ndh.ddsl.validator.rules.valueobject;

import lombok.Data;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 2.3: Value Object Has No Identity
 * Validates that Value Objects do not have identity/ID fields.
 */
@Data
public class ValueObjectNoIdentityRule implements DDDValidationRule {

    private static final String RULE_CODE = "VALUE_OBJECT_NO_IDENTITY";
    private static final String RULE_NAME = "Value Object Has No Identity";
    private static final String DESCRIPTION = "Value Objects must not have identity/ID fields";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getValueObjects().forEach(vo ->
                validateNoIdentityField(vo, errors));
        });

        return errors;
    }

    private void validateNoIdentityField(ValueObject vo, List<DDDValidationError> errors) {
        for (Field field : vo.getFields()) {
            String fieldName = field.getName().toLowerCase();

            // Check for common identity field patterns
            if (fieldName.equals("id") ||
                fieldName.endsWith("id") ||
                fieldName.equals("identifier") ||
                fieldName.endsWith("identifier")) {

                errors.add(new DDDValidationError(
                    RULE_CODE,
                    RULE_NAME,
                    DESCRIPTION,
                    String.format("Value Object '%s' should not have identity field '%s' - Value Objects are identified by their values, not IDs",
                        vo.getName(), field.getName()),
                    vo.getLocation(),
                    DDDValidationSeverity.ERROR
                ));
            }

            // Check for UUID types which are commonly used for IDs
            if (field.getType() != null &&
                ("UUID".equals(field.getType().getSimpleName()) ||
                 "java.util.UUID".equals(field.getType().getFullyQualifiedName()))) {

                errors.add(new DDDValidationError(
                    RULE_CODE,
                    RULE_NAME,
                    DESCRIPTION,
                    String.format("Value Object '%s' has UUID field '%s' - UUIDs are typically used for identity, which Value Objects should not have",
                        vo.getName(), field.getName()),
                    vo.getLocation(),
                    DDDValidationSeverity.WARNING
                ));
            }
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
