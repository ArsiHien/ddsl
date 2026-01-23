package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.BoundedContext;
import uet.ndh.ddsl.parser.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts YAML data structure to DomainModel AST.
 */
public class YamlToDomainModelConverter {

    private final BoundedContextConverter boundedContextConverter;
    private LocationTracker locationTracker;

    public YamlToDomainModelConverter() {
        this.boundedContextConverter = new BoundedContextConverter();
    }

    @SuppressWarnings("unchecked")
    public DomainModel convert(Map<String, Object> yamlData, LocationTracker locationTracker) throws ParseException {
        this.locationTracker = locationTracker;
        SourceLocation rootLocation = locationTracker.getRootLocation();

        // Debug: Print tracked key locations
        System.out.println("=== YAML Data Structure ===");
        System.out.println(yamlData);
        System.out.println("=== Tracked Key Locations ===");
        locationTracker.printKeyLocations();

        // Parse model section
        Map<String, Object> modelData = getRequiredMap(yamlData, "model", "model");
        String modelName = getRequiredString(modelData, "name", "model.name");
        String basePackage = getRequiredString(modelData, "basePackage", "model.basePackage");

        // Parse bounded contexts
        List<Object> boundedContextsData = getRequiredList(yamlData, "boundedContexts", "boundedContexts");
        DomainModel domainModel = new DomainModel(rootLocation, modelName, basePackage);

        for (int i = 0; i < boundedContextsData.size(); i++) {
            Object contextData = boundedContextsData.get(i);
            if (!(contextData instanceof Map)) {
                SourceLocation contextLocation = locationTracker.getLocationForKey("boundedContexts[" + i + "]");
                throw new ParseException("Bounded context must be an object", contextLocation);
            }

            String contextPath = "boundedContexts[" + i + "]";
            SourceLocation contextLocation = locationTracker.getLocationForKey(contextPath);
            BoundedContext context = boundedContextConverter.convert(
                (Map<String, Object>) contextData, basePackage, contextLocation, locationTracker, contextPath);
            domainModel.addBoundedContext(context);
        }

        return domainModel;
    }

    private String getRequiredString(Map<String, Object> data, String key, String keyPath)
            throws ParseException {
        Object value = data.get(key);
        if (value == null) {
            String parentPath = keyPath.contains(".") ? keyPath.substring(0, keyPath.lastIndexOf('.')) : "";
            SourceLocation location = locationTracker.getLocationForMissingKey(parentPath, key);
            throw new ParseException("Required field '" + key + "' is missing", location);
        }
        if (!(value instanceof String)) {
            SourceLocation location = locationTracker.getLocationForKey(keyPath);
            throw new ParseException("Field '" + key + "' must be a string", location);
        }
        return (String) value;
    }

    private Map<String, Object> getRequiredMap(Map<String, Object> data, String key, String keyPath)
            throws ParseException {
        Object value = data.get(key);
        if (value == null) {
            String parentPath = keyPath.contains(".") ? keyPath.substring(0, keyPath.lastIndexOf('.')) : "";
            SourceLocation location = locationTracker.getLocationForMissingKey(parentPath, key);
            throw new ParseException("Required field '" + key + "' is missing. Available keys: " + data.keySet(), location);
        }
        if (!(value instanceof Map)) {
            SourceLocation location = locationTracker.getLocationForKey(keyPath);
            throw new ParseException("Field '" + key + "' must be an object", location);
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> getRequiredList(Map<String, Object> data, String key, String keyPath)
            throws ParseException {
        Object value = data.get(key);
        if (value == null) {
            String parentPath = keyPath.contains(".") ? keyPath.substring(0, keyPath.lastIndexOf('.')) : "";
            SourceLocation location = locationTracker.getLocationForMissingKey(parentPath, key);
            throw new ParseException("Required field '" + key + "' is missing", location);
        }
        if (!(value instanceof List)) {
            SourceLocation location = locationTracker.getLocationForKey(keyPath);
            throw new ParseException("Field '" + key + "' must be a list", location);
        }
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOptionalMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> getOptionalList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return new ArrayList<>();
    }

    private String getOptionalString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
}
