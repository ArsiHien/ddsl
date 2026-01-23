package uet.ndh.ddsl.core.application.applicationservice;

import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;

import java.util.ArrayList;
import java.util.List; /**
 * Represents a dependency in an application service.
 */
public class Dependency {
    private final String name;
    private final JavaType type;
    private final boolean isRequired;

    public Dependency(String name, JavaType type, boolean isRequired) {
        this.name = name;
        this.type = type;
        this.isRequired = isRequired;
    }

    public String getName() {
        return name;
    }

    public JavaType getType() {
        return type;
    }

    public boolean isRequired() {
        return isRequired;
    }

    /**
     * Create a deep copy of this dependency for normalization.
     */
    public Dependency copy() {
        return new Dependency(this.name, this.type, this.isRequired);
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Dependency name cannot be empty", location));
        }

        if (type == null) {
            errors.add(new ValidationError("Dependency type cannot be null", location));
        }

        return errors;
    }
}
