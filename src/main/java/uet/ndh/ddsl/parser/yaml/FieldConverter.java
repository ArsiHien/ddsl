package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Visibility;
import uet.ndh.ddsl.core.building.Constraint;
import uet.ndh.ddsl.core.building.ConstraintType;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML field data to Field AST.
 */
public class FieldConverter extends BaseYamlConverter {

    private final JavaTypeConverter typeConverter;

    public FieldConverter(JavaTypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    public Field convert(Map<String, Object> data, SourceLocation location) throws ParseException {
        String name = getRequiredString(data, "name", location);
        String typeString = getRequiredString(data, "type", location);
        JavaType type = typeConverter.convertType(typeString, location);

        Visibility visibility = parseVisibility(getOptionalString(data, "visibility", "PRIVATE"));
        boolean isFinal = getOptionalBoolean(data, "final", true);
        boolean isNullable = getOptionalBoolean(data, "nullable", false);
        String defaultValue = getOptionalString(data, "default", null);

        Field field = new Field(name, type, visibility, isFinal, isNullable, defaultValue);

        // Add description as comment (we could store this in Field if needed)
        String description = getOptionalString(data, "description", null);

        // Convert constraints
        List<Object> constraintsData = getOptionalList(data, "constraints");
        for (Object constraintData : constraintsData) {
            if (constraintData instanceof Map) {
                Constraint constraint = convertConstraint((Map<String, Object>) constraintData, location);
                field.addConstraint(constraint);
            }
        }

        return field;
    }

    private Visibility parseVisibility(String visibilityString) {
        return switch (visibilityString.toUpperCase()) {
            case "PUBLIC" -> Visibility.PUBLIC;
            case "PROTECTED" -> Visibility.PROTECTED;
            case "PACKAGE_PRIVATE" -> Visibility.PACKAGE_PRIVATE;
            default -> Visibility.PRIVATE;
        };
    }

    private Constraint convertConstraint(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String typeString = getRequiredString(data, "type", location);
        ConstraintType type = parseConstraintType(typeString);
        String value = getOptionalString(data, "value", "");
        String message = getOptionalString(data, "message", "Validation failed");

        return new Constraint(type, value, message);
    }

    private ConstraintType parseConstraintType(String typeString) {
        return switch (typeString.toUpperCase()) {
            case "NOT_NULL" -> ConstraintType.NOT_NULL;
            case "NOT_EMPTY" -> ConstraintType.NOT_EMPTY;
            case "MIN" -> ConstraintType.MIN;
            case "MAX" -> ConstraintType.MAX;
            case "SIZE" -> ConstraintType.SIZE;
            case "PATTERN" -> ConstraintType.PATTERN;
            case "POSITIVE" -> ConstraintType.POSITIVE;
            case "NEGATIVE" -> ConstraintType.NEGATIVE;
            case "EMAIL" -> ConstraintType.PATTERN; // We'll use PATTERN for email
            default -> ConstraintType.CUSTOM;
        };
    }
}
