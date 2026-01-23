package uet.ndh.ddsl.validator.rules;

import uet.ndh.ddsl.core.model.DomainModel;

import java.util.List;

/**
 * Interface for DDD validation rules.
 */
public interface DDDValidationRule {

    /**
     * Validate the domain model against this rule.
     * @param model The domain model to validate
     * @return List of validation errors found
     */
    List<DDDValidationError> validate(DomainModel model);

    /**
     * Get the rule code (e.g., "ENTITY_MUST_HAVE_IDENTITY").
     * @return The unique rule identifier
     */
    String getRuleCode();

    /**
     * Get the rule name/title.
     * @return Human-readable rule name
     */
    String getRuleName();

    /**
     * Get the rule description.
     * @return Detailed description of what this rule checks
     */
    String getDescription();
}
