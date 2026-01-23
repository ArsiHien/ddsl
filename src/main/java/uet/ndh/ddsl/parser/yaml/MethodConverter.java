package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.building.Parameter;
import uet.ndh.ddsl.core.building.Visibility;
import uet.ndh.ddsl.core.building.CodeBlock;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML method data to Method AST.
 */
public class MethodConverter extends BaseYamlConverter {

    private final JavaTypeConverter typeConverter;

    public MethodConverter(JavaTypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    public Method convert(Map<String, Object> data, SourceLocation location) throws ParseException {
        String name = getRequiredString(data, "name", location);
        String returnTypeString = getOptionalString(data, "returnType", "void");
        JavaType returnType = typeConverter.convertType(returnTypeString, location);

        Visibility visibility = parseVisibility(getOptionalString(data, "visibility", "PUBLIC"));
        boolean isStatic = getOptionalBoolean(data, "static", false);
        boolean isFinal = getOptionalBoolean(data, "final", false);

        Method method = new Method(name, returnType, visibility, isStatic, isFinal);

        // Add description as documentation
        String description = getOptionalString(data, "description", null);

        // Convert parameters
        List<Object> parametersData = getOptionalList(data, "parameters");
        for (Object paramData : parametersData) {
            if (paramData instanceof Map) {
                Parameter parameter = convertParameter((Map<String, Object>) paramData, location);
                method.addParameter(parameter);
            }
        }

        // Convert method body
        String bodyCode = getOptionalString(data, "body", null);
        if (bodyCode != null) {
            CodeBlock body = new CodeBlock();
            // For now, we'll store the body as raw code
            // In a full implementation, we'd parse this into Statement objects
            body.addRawCode(bodyCode);
            method.setBody(body);
        }

        return method;
    }

    private Parameter convertParameter(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String typeString = getRequiredString(data, "type", location);
        JavaType type = typeConverter.convertType(typeString, location);
        boolean isFinal = getOptionalBoolean(data, "final", false);

        return new Parameter(name, type, isFinal);
    }

    private Visibility parseVisibility(String visibilityString) {
        return switch (visibilityString.toUpperCase()) {
            case "PUBLIC" -> Visibility.PUBLIC;
            case "PROTECTED" -> Visibility.PROTECTED;
            case "PACKAGE_PRIVATE" -> Visibility.PACKAGE_PRIVATE;
            default -> Visibility.PRIVATE;
        };
    }
}
