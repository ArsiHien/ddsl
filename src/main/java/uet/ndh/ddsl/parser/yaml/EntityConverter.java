package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.entity.IdentityField;
import uet.ndh.ddsl.core.model.entity.IdentityType;
import uet.ndh.ddsl.core.model.entity.IdGenerationStrategy;
import uet.ndh.ddsl.core.model.entity.EventReference;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML entity data to Entity AST.
 */
public class EntityConverter extends BaseYamlConverter {

    private final FieldConverter fieldConverter;
    private final MethodConverter methodConverter;

    public EntityConverter() {
        JavaTypeConverter typeConverter = new JavaTypeConverter();
        this.fieldConverter = new FieldConverter(typeConverter);
        this.methodConverter = new MethodConverter(typeConverter);
    }

    public Entity convert(Map<String, Object> data, boolean isAggregateRoot,
            SourceLocation location) throws ParseException {

        String name = getRequiredString(data, "name", location);
        String description = getOptionalString(data, "description", null);

        // Convert identity field
        Map<String, Object> identityData = getRequiredMap(data, "identity", location);
        IdentityField identityField = convertIdentityField(identityData, location);

        Entity entity = new Entity(location, name, isAggregateRoot, identityField);
        if (description != null) {
            entity.setDocumentation(description);
        }

        // Convert fields
        List<Object> fieldsData = getOptionalList(data, "fields");
        for (Object fieldData : fieldsData) {
            if (fieldData instanceof Map) {
                Field field = fieldConverter.convert((Map<String, Object>) fieldData, location);
                entity.addField(field);
            }
        }

        // Convert methods
        List<Object> methodsData = getOptionalList(data, "methods");
        for (Object methodData : methodsData) {
            if (methodData instanceof Map) {
                Method method = methodConverter.convert((Map<String, Object>) methodData, location);
                entity.addMethod(method);
            }
        }

        // Convert invariants (if in root data)
        List<Object> invariantsData = getOptionalList(data, "invariants");
        for (Object invData : invariantsData) {
            // Invariants will be handled at aggregate level
        }

        return entity;
    }

    private IdentityField convertIdentityField(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String typeString = getRequiredString(data, "type", location);
        String generationString = getOptionalString(data, "generation", "CLIENT_GENERATED");

        IdentityType type = parseIdentityType(typeString);
        IdGenerationStrategy generation = parseGenerationStrategy(generationString);

        return new IdentityField(name, type, generation);
    }

    private IdentityType parseIdentityType(String typeString) {
        return switch (typeString.toUpperCase()) {
            case "UUID" -> IdentityType.UUID;
            case "LONG" -> IdentityType.LONG;
            case "STRING" -> IdentityType.STRING;
            case "COMPOSITE" -> IdentityType.COMPOSITE;
            default -> IdentityType.UUID;
        };
    }

    private IdGenerationStrategy parseGenerationStrategy(String strategyString) {
        return switch (strategyString.toUpperCase()) {
            case "CLIENT_GENERATED" -> IdGenerationStrategy.CLIENT_GENERATED;
            case "SEQUENCE" -> IdGenerationStrategy.SEQUENCE;
            case "AUTO" -> IdGenerationStrategy.AUTO;
            default -> IdGenerationStrategy.CLIENT_GENERATED;
        };
    }
}
