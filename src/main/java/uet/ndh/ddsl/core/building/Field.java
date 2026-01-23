package uet.ndh.ddsl.core.building;

import lombok.Getter;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a field in a domain object.
 */
public class Field {
    private final SourceLocation location;
    @Getter
    private final String name;
    @Getter
    private final JavaType type;
    @Getter
    private final Visibility visibility;
    @Getter
    private final boolean isFinal;
    @Getter
    private final boolean isNullable;
    private final String defaultValue;
    private final List<Constraint> constraints;

    public Field(SourceLocation location, String name, JavaType type, Visibility visibility,
                 boolean isFinal, boolean isNullable, String defaultValue) {
        this.location = location;
        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.isFinal = isFinal;
        this.isNullable = isNullable;
        this.defaultValue = defaultValue;
        this.constraints = new ArrayList<>();
    }

    public Field(String name, JavaType type, Visibility visibility,
                 boolean isFinal, boolean isNullable, String defaultValue) {
        this(null, name, type, visibility, isFinal, isNullable, defaultValue);
    }

    public Field(String name, JavaType type) {
        this(null, name, type, Visibility.PRIVATE, true, false, null);
    }

    public SourceLocation getLocation() {
        return location;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<Constraint> getConstraints() {
        return new ArrayList<>(constraints);
    }

    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    /**
     * Create a deep copy of this field for normalization.
     */
    public Field copy() {
        Field copy = new Field(this.location, this.name, this.type, this.visibility,
                              this.isFinal, this.isNullable, this.defaultValue);

        // Copy all constraints
        for (Constraint constraint : this.constraints) {
            copy.addConstraint(constraint.copy());
        }

        return copy;
    }

    /**
     * Generate field declaration code.
     * @return Java field declaration
     */
    public String generateFieldDeclaration() {
        StringBuilder sb = new StringBuilder();

        // Visibility
        switch (visibility) {
            case PRIVATE: sb.append("private "); break;
            case PROTECTED: sb.append("protected "); break;
            case PUBLIC: sb.append("public "); break;
            case PACKAGE_PRIVATE: break; // No modifier
        }

        // Final modifier
        if (isFinal) {
            sb.append("final ");
        }

        // Type and name
        sb.append(type.getSimpleName()).append(" ").append(name);

        // Default value
        if (defaultValue != null) {
            sb.append(" = ").append(defaultValue);
        }

        sb.append(";");
        return sb.toString();
    }

    /**
     * Generate getter method code.
     * @return Java getter method
     */
    public String generateGetter() {
        String getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        if (type.getSimpleName().equals("boolean")) {
            getterName = "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        return String.format("public %s %s() {\n    return %s;\n}",
                type.getSimpleName(), getterName, name);
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Field name cannot be empty", location));
        }

        if (type == null) {
            errors.add(new ValidationError("Field type cannot be null", location));
        }

        return errors;
    }
}
