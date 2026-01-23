package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;
import uet.ndh.ddsl.core.model.aggregate.Invariant;
import uet.ndh.ddsl.core.building.expression.LiteralExpression;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML aggregate data to Aggregate AST.
 */
public class AggregateConverter extends BaseYamlConverter {

    private final EntityConverter entityConverter;
    private final ValueObjectConverter valueObjectConverter;

    public AggregateConverter() {
        this.entityConverter = new EntityConverter();
        this.valueObjectConverter = new ValueObjectConverter();
    }

    public Aggregate convert(Map<String, Object> data, SourceLocation location) throws ParseException {
        String name = getRequiredString(data, "name", location);
        String description = getOptionalString(data, "description", null);

        // Convert root entity - the root section contains the entity data directly
        Map<String, Object> rootData = getRequiredMap(data, "root", location);

        // The entity name is specified in the "entity" field within root
        String entityName = getRequiredString(rootData, "entity", location);

        // Create a new map with the entity name and the root data (excluding the "entity" field)
        Map<String, Object> entityData = new java.util.HashMap<>(rootData);
        entityData.put("name", entityName);
        entityData.remove("entity"); // Remove the entity field as it's not part of entity structure

        Entity root = entityConverter.convert(entityData, true, location);

        Aggregate aggregate = new Aggregate(location, name, root);
        if (description != null) {
            aggregate.setDocumentation(description);
        }

        // Convert internal entities
        List<Object> entitiesData = getOptionalList(data, "entities");
        for (Object entityDataObj : entitiesData) {
            if (entityDataObj instanceof Map) {
                Entity entity = entityConverter.convert((Map<String, Object>) entityDataObj, false, location);
                aggregate.addEntity(entity);
            }
        }

        // Convert value objects
        List<Object> valueObjectsData = getOptionalList(data, "valueObjects");
        for (Object voData : valueObjectsData) {
            if (voData instanceof Map) {
                ValueObject vo = valueObjectConverter.convert((Map<String, Object>) voData, location);
                aggregate.addValueObject(vo);
            }
        }

        // Convert invariants
        List<Object> invariantsData = getOptionalList(data, "invariants");
        for (Object invData : invariantsData) {
            if (invData instanceof Map) {
                Invariant invariant = convertInvariant((Map<String, Object>) invData, location);
                aggregate.addInvariant(invariant);
            }
        }

        return aggregate;
    }

    private Invariant convertInvariant(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String description = getOptionalString(data, "description", "");
        String conditionCode = getRequiredString(data, "condition", location);
        String errorMessage = getRequiredString(data, "errorMessage", location);

        // For now, create a simple literal expression with the condition code
        // In a full implementation, we'd parse this into proper expression AST
        LiteralExpression condition = new LiteralExpression(conditionCode, null);

        return new Invariant(name, description, condition, errorMessage);
    }
}
