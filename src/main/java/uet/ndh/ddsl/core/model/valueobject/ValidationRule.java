package uet.ndh.ddsl.core.model.valueobject;

import uet.ndh.ddsl.core.building.expression.Expression;

/**
 * Individual validation rule.
 */
public record ValidationRule(Expression condition, String errorMessage) {

    /**
     * Create a copy of this validation rule.
     */
    public ValidationRule copy() {
        return new ValidationRule(this.condition.copy(), this.errorMessage);
    }
}
