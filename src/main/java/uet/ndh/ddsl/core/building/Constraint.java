package uet.ndh.ddsl.core.building;

/**
 * Represents a validation constraint on a field.
 */
public class Constraint {
    private final ConstraintType type;
    private final String value;
    private final String message;

    public Constraint(ConstraintType type, String value, String message) {
        this.type = type;
        this.value = value;
        this.message = message;
    }

    public ConstraintType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Create a copy of this constraint.
     */
    public Constraint copy() {
        return new Constraint(this.type, this.value, this.message);
    }

    /**
     * Generate validation code for this constraint.
     * @return Java code string for validation
     */
    public String generateValidationCode() {
        switch (type) {
            case NOT_NULL:
                return "if (value == null) throw new IllegalArgumentException(\"" + message + "\");";
            case NOT_EMPTY:
                return "if (value == null || value.isEmpty()) throw new IllegalArgumentException(\"" + message + "\");";
            case MIN:
                return "if (value < " + value + ") throw new IllegalArgumentException(\"" + message + "\");";
            case MAX:
                return "if (value > " + value + ") throw new IllegalArgumentException(\"" + message + "\");";
            default:
                return "// Custom constraint: " + type;
        }
    }
}
