package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.parser.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class for YAML converters with common utility methods.
 */
public abstract class BaseYamlConverter {

    protected String getRequiredString(Map<String, Object> data, String key, SourceLocation location)
            throws ParseException {
        Object value = data.get(key);
        if (value == null) {
            // Add debug information about what keys are available
            String availableKeys = data.keySet().toString();
            throw new ParseException("Required field '" + key + "' is missing. Available keys: " + availableKeys, location);
        }
        if (!(value instanceof String)) {
            throw new ParseException("Field '" + key + "' must be a string", location);
        }
        return (String) value;
    }

    protected String getOptionalString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    protected Boolean getOptionalBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getRequiredMap(Map<String, Object> data, String key,
            SourceLocation location) throws ParseException {
        Object value = data.get(key);
        if (value == null) {
            throw new ParseException("Required field '" + key + "' is missing", location);
        }
        if (!(value instanceof Map)) {
            throw new ParseException("Field '" + key + "' must be an object", location);
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getOptionalMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected List<Object> getRequiredList(Map<String, Object> data, String key,
            SourceLocation location) throws ParseException {
        Object value = data.get(key);
        if (value == null) {
            throw new ParseException("Required field '" + key + "' is missing", location);
        }
        if (!(value instanceof List)) {
            throw new ParseException("Field '" + key + "' must be a list", location);
        }
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    protected List<Object> getOptionalList(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return new ArrayList<>();
    }
}
