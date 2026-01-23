package uet.ndh.ddsl.validator.rules.valueobject;

import lombok.Data;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;
import uet.ndh.ddsl.validator.rules.DDDValidationError;
import uet.ndh.ddsl.validator.rules.DDDValidationRule;
import uet.ndh.ddsl.validator.rules.DDDValidationSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 2.1: Value Object Immutability
 * Validates that Value Objects are completely immutable.
 */
@Data
public class ValueObjectImmutableRule implements DDDValidationRule {

    private static final String RULE_CODE = "VALUE_OBJECT_IMMUTABLE";
    private static final String RULE_NAME = "Value Object Immutable";
    private static final String DESCRIPTION = "Value Object must be completely immutable: all fields final, no setters, class should be final";

    @Override
    public List<DDDValidationError> validate(DomainModel model) {
        List<DDDValidationError> errors = new ArrayList<>();

        model.getBoundedContexts().forEach(context -> {
            context.getValueObjects().forEach(vo ->
                validateValueObjectImmutability(vo, errors));
        });

        return errors;
    }

    private void validateValueObjectImmutability(ValueObject vo, List<DDDValidationError> errors) {
        // Check all fields are final
        for (Field field : vo.getFields()) {
            if (!field.isFinal()) {
                errors.add(new DDDValidationError(
                    RULE_CODE,
                    RULE_NAME,
                    DESCRIPTION,
                    String.format("Value Object '%s' has mutable field '%s' - all fields must be final",
                        vo.getName(), field.getName()),
                    vo.getLocation(),
                    DDDValidationSeverity.ERROR
                ));
            }
        }

        // Check for setter methods
        for (Method method : vo.getMethods()) {
            if (isSetterMethod(method)) {
                errors.add(new DDDValidationError(
                    RULE_CODE,
                    RULE_NAME,
                    DESCRIPTION,
                    String.format("Value Object '%s' has setter method '%s' - Value Objects must be immutable",
                        vo.getName(), method.getName()),
                    vo.getLocation(),
                    DDDValidationSeverity.ERROR
                ));
            }
        }

        // Suggest making the class final (warning level)
        errors.add(new DDDValidationError(
            RULE_CODE,
            RULE_NAME,
            DESCRIPTION,
            String.format("Value Object '%s' should be declared as final class to prevent inheritance",
                vo.getName()),
            vo.getLocation(),
            DDDValidationSeverity.WARNING
        ));

        // Check for side effects in methods (simplified check)
        validateMethodsSideEffectFree(vo, errors);
    }

    private boolean isSetterMethod(Method method) {
        return method.getName() != null &&
               method.getName().startsWith("set") &&
               method.getParameters().size() == 1 &&
               (method.getReturnType() == null || "void".equals(method.getReturnType().getSimpleName()));
    }

    private void validateMethodsSideEffectFree(ValueObject vo, List<DDDValidationError> errors) {
        for (Method method : vo.getMethods()) {
            // Skip constructors and standard methods
            if (method.getName() == null ||
                method.getName().equals("equals") ||
                method.getName().equals("hashCode") ||
                method.getName().equals("toString") ||
                method.getName().startsWith("get") ||
                method.getName().startsWith("is")) {
                continue;
            }

            // If method returns void, it might have side effects
            if (method.getReturnType() == null || "void".equals(method.getReturnType().getSimpleName())) {
                errors.add(new DDDValidationError(
                    RULE_CODE,
                    RULE_NAME,
                    DESCRIPTION,
                    String.format("Value Object '%s' method '%s' returns void - might have side effects. Value Object methods should be pure functions",
                        vo.getName(), method.getName()),
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
