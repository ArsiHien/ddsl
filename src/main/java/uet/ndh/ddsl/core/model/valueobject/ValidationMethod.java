package uet.ndh.ddsl.core.model.valueobject;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List; /**
 * Validation method for value objects.
 */
public class ValidationMethod {
    @Getter
    private final String name;
    private final List<ValidationRule> rules;

    public ValidationMethod(String name) {
        this.name = name;
        this.rules = new ArrayList<>();
    }

    public List<ValidationRule> getRules() {
        return new ArrayList<>(rules);
    }

    public void addRule(ValidationRule rule) {
        rules.add(rule);
    }

    /**
     * Generate validation code for this method.
     * @return Java validation method code
     */
    public String generateValidationCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("private void ").append(name).append("() {\n");

        for (ValidationRule rule : rules) {
            sb.append("    if (!(").append(rule.condition().generateCode()).append(")) {\n");
            sb.append("        throw new IllegalArgumentException(\"").append(rule.errorMessage()).append("\");\n");
            sb.append("    }\n");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Create a copy of this validation method.
     */
    public ValidationMethod copy() {
        ValidationMethod copy = new ValidationMethod(this.name);
        for (ValidationRule rule : this.rules) {
            copy.addRule(rule.copy());
        }
        return copy;
    }
}
