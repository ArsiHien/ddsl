package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.model.repository.RepositoryInterface;
import uet.ndh.ddsl.core.model.repository.RepositoryMethod;
import uet.ndh.ddsl.core.model.repository.RepositoryMethodType;
import uet.ndh.ddsl.core.building.Parameter;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML repository data to RepositoryInterface AST.
 */
public class RepositoryConverter extends BaseYamlConverter {

    private final JavaTypeConverter typeConverter;

    public RepositoryConverter() {
        this.typeConverter = new JavaTypeConverter();
    }

    public RepositoryInterface convert(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String aggregateTypeString = getRequiredString(data, "aggregate", location);
        String idTypeString = getRequiredString(data, "idType", location);

        JavaType aggregateType = typeConverter.convertType(aggregateTypeString, location);
        JavaType idType = typeConverter.convertType(idTypeString, location);

        RepositoryInterface repository = new RepositoryInterface(location, name, aggregateType, idType);

        // Convert methods
        List<Object> methodsData = getOptionalList(data, "methods");
        for (Object methodData : methodsData) {
            if (methodData instanceof Map) {
                RepositoryMethod method = convertRepositoryMethod(
                    (Map<String, Object>) methodData, location);
                repository.addMethod(method);
            }
        }

        return repository;
    }

    private RepositoryMethod convertRepositoryMethod(Map<String, Object> data,
            SourceLocation location) throws ParseException {
        String name = getRequiredString(data, "name", location);
        String returnTypeString = getRequiredString(data, "returnType", location);
        JavaType returnType = typeConverter.convertType(returnTypeString, location);

        RepositoryMethodType methodType = parseRepositoryMethodType(name);

        RepositoryMethod method = new RepositoryMethod(name, returnType, methodType);

        // Convert parameters
        List<Object> parametersData = getOptionalList(data, "parameters");
        for (Object paramData : parametersData) {
            if (paramData instanceof Map) {
                Parameter parameter = convertParameter((Map<String, Object>) paramData, location);
                method.addParameter(parameter);
            }
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

    private RepositoryMethodType parseRepositoryMethodType(String methodName) {
        if (methodName.startsWith("findById")) {
            return RepositoryMethodType.FIND_BY_ID;
        } else if (methodName.equals("save")) {
            return RepositoryMethodType.SAVE;
        } else if (methodName.equals("delete")) {
            return RepositoryMethodType.DELETE;
        } else if (methodName.equals("findAll")) {
            return RepositoryMethodType.FIND_ALL;
        } else if (methodName.startsWith("find")) {
            return RepositoryMethodType.FIND_BY_CRITERIA;
        } else if (methodName.startsWith("exists")) {
            return RepositoryMethodType.EXISTS;
        } else if (methodName.startsWith("count")) {
            return RepositoryMethodType.COUNT;
        } else {
            return RepositoryMethodType.FIND_BY_CRITERIA;
        }
    }
}
