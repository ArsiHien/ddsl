package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.model.DomainService;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML domain service data to DomainService AST.
 */
public class DomainServiceConverter extends BaseYamlConverter {

    private final MethodConverter methodConverter;

    public DomainServiceConverter() {
        this.methodConverter = new MethodConverter(new JavaTypeConverter());
    }

    public DomainService convert(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String description = getOptionalString(data, "description", null);
        boolean isInterface = getOptionalBoolean(data, "interface", false);

        DomainService service = new DomainService(location, name, isInterface);
        if (description != null) {
            service.setDocumentation(description);
        }

        // Convert methods
        List<Object> methodsData = getOptionalList(data, "methods");
        for (Object methodData : methodsData) {
            if (methodData instanceof Map) {
                Method method = methodConverter.convert((Map<String, Object>) methodData, location);
                service.addMethod(method);
            }
        }

        return service;
    }
}
