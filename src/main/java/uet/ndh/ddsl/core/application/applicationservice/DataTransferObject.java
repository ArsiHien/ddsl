package uet.ndh.ddsl.core.application.applicationservice;

import lombok.Getter;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.building.Field;

import java.util.ArrayList;
import java.util.List; /**
 * Represents a Data Transfer Object (DTO).
 * DTOs are used to transfer data between application boundaries.
 */
public class DataTransferObject {
    @Getter
    private final SourceLocation location;
    @Getter
    private final String name;
    private final List<Field> fields;

    public DataTransferObject(String name) {
        this(null, name);
    }

    public DataTransferObject(SourceLocation location, String name) {
        this.location = location;
        this.name = name;
        this.fields = new ArrayList<>();
    }

    public List<Field> getFields() {
        return new ArrayList<>(fields);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    /**
     * Create a deep copy of this DTO for normalization.
     */
    public DataTransferObject copy() {
        DataTransferObject copy = new DataTransferObject(this.location, this.name);

        // Copy all fields
        for (Field field : this.fields) {
            copy.addField(field.copy());
        }

        return copy;
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("DTO name cannot be empty", location));
        }

        // Validate all fields
        for (Field field : fields) {
            errors.addAll(field.validate(location));
        }

        return errors;
    }
}
