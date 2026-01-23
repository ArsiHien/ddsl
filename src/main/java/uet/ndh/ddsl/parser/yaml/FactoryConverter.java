package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.model.factory.Factory;
import uet.ndh.ddsl.core.model.factory.FactoryMethod;
import uet.ndh.ddsl.core.building.Parameter;
import uet.ndh.ddsl.core.building.CodeBlock;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML factory data to Factory AST.
 */
public class FactoryConverter extends BaseYamlConverter {

    private final JavaTypeConverter typeConverter;

    public FactoryConverter() {
        this.typeConverter = new JavaTypeConverter();
    }

    public Factory convert(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String createsTypeString = getRequiredString(data, "creates", location);
        JavaType createdType = typeConverter.convertType(createsTypeString, location);

        Factory factory = new Factory(location, name, createdType);

        // Convert factory methods
        List<Object> methodsData = getOptionalList(data, "methods");
        for (Object methodData : methodsData) {
            if (methodData instanceof Map) {
                FactoryMethod factoryMethod = convertFactoryMethod(
                    (Map<String, Object>) methodData, location);
                factory.addMethod(factoryMethod);
            }
        }

        return factory;
    }

    private FactoryMethod convertFactoryMethod(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String returnTypeString = getRequiredString(data, "returnType", location);
        JavaType returnType = typeConverter.convertType(returnTypeString, location);

        FactoryMethod method = new FactoryMethod(name, returnType);

        // Convert parameters
        List<Object> parametersData = getOptionalList(data, "parameters");
        for (Object paramData : parametersData) {
            if (paramData instanceof Map) {
                Parameter parameter = convertParameter((Map<String, Object>) paramData, location);
                method.addParameter(parameter);
            }
        }

        // Convert creation logic
        String bodyCode = getOptionalString(data, "body", null);
        if (bodyCode != null) {
            CodeBlock creationLogic = new CodeBlock();
            creationLogic.addRawCode(bodyCode);
            method.setCreationLogic(creationLogic);
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
}
