package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.building.Visibility;
import uet.ndh.ddsl.core.building.Constraint;
import uet.ndh.ddsl.core.building.ConstraintType;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;
import uet.ndh.ddsl.core.model.valueobject.ValidationMethod;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML value object data to ValueObject AST.
 */
public class ValueObjectConverter extends BaseYamlConverter {

    private final JavaTypeConverter typeConverter;
    private final FieldConverter fieldConverter;
    private final MethodConverter methodConverter;

    public ValueObjectConverter() {
        this.typeConverter = new JavaTypeConverter();
        this.fieldConverter = new FieldConverter(typeConverter);
        this.methodConverter = new MethodConverter(typeConverter);
    }

    public ValueObject convert(Map<String, Object> data, SourceLocation location) throws ParseException {
        String name = getRequiredString(data, "name", location);

        ValueObject valueObject = new ValueObject(location, name);

        // Add description as documentation
        String description = getOptionalString(data, "description", null);
        if (description != null) {
            valueObject.setDocumentation(description);
        }

        // Convert fields
        List<Object> fieldsData = getOptionalList(data, "fields");
        for (Object fieldData : fieldsData) {
            if (fieldData instanceof Map) {
                Field field = fieldConverter.convert((Map<String, Object>) fieldData, location);
                valueObject.addField(field);
            }
        }

        // Convert methods
        List<Object> methodsData = getOptionalList(data, "methods");
        for (Object methodData : methodsData) {
            if (methodData instanceof Map) {
                Method method = methodConverter.convert((Map<String, Object>) methodData, location);
                valueObject.addMethod(method);
            }
        }

        // For now, we'll create a default validation method if there are field constraints
        if (!fieldsData.isEmpty()) {
            ValidationMethod validation = new ValidationMethod("validate");
            valueObject.addValidation(validation);
        }

        return valueObject;
    }
}
