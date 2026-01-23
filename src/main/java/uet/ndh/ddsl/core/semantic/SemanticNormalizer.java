package uet.ndh.ddsl.core.semantic;

import lombok.Data;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.model.BoundedContext;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.entity.IdentityField;
import uet.ndh.ddsl.core.model.entity.IdentityType;
import uet.ndh.ddsl.core.model.entity.IdGenerationStrategy;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.building.Visibility;
import uet.ndh.ddsl.core.type.PrimitiveType;
import uet.ndh.ddsl.core.type.PrimitiveKind;

import java.util.*;
import java.util.stream.Stream;

/**
 * Semantic model normalizer implementing desugaring transformation.
 *
 * Transforms high-level DSL constructs into normalized semantic model:
 * - Expands implicit declarations
 * - Infers missing attributes
 * - Applies default values
 * - Performs semantic enrichment
 *
 * Academic reference: Tree rewriting systems and term rewriting
 * Implementation pattern: Multi-pass AST transformation
 */
@Data
public class SemanticNormalizer {

    private final Map<String, NormalizationRule> rules;
    private final List<String> appliedTransformations;

    public SemanticNormalizer() {
        this.rules = new LinkedHashMap<>();
        this.appliedTransformations = new ArrayList<>();
        initializeNormalizationRules();
    }

    /**
     * Main normalization entry point
     * Algorithm: Fixed-point computation until no more transformations apply
     */
    public NormalizedDomainModel normalize(DomainModel model) {
        System.out.println("🔄 Starting semantic normalization (desugaring)...");

        // Create mutable copy for transformation
        DomainModel normalized = model.copy();

        boolean changed = true;
        int iterations = 0;
        final int MAX_ITERATIONS = 10; // Prevent infinite loops

        // Fixed-point iteration
        while (changed && iterations < MAX_ITERATIONS) {
            changed = false;
            iterations++;

            System.out.printf("   Pass %d: Applying normalization rules...\n", iterations);

            // Apply all normalization rules
            for (NormalizationRule rule : rules.values()) {
                if (rule.isApplicable(normalized)) {
                    System.out.printf("     ✓ Applying rule: %s\n", rule.getName());
                    normalized = rule.apply(normalized);
                    appliedTransformations.add(rule.getName());
                    changed = true;
                }
            }
        }

        System.out.printf("✅ Normalization completed in %d iterations\n", iterations);
        System.out.printf("📝 Applied transformations: %s\n", String.join(", ", appliedTransformations));

        return new NormalizedDomainModel(normalized, appliedTransformations);
    }

    /**
     * Initialize standard normalization rules
     * Order matters - dependencies between rules
     */
    private void initializeNormalizationRules() {
        // Rule 1: Infer missing identity fields for entities
        rules.put("INFER_ENTITY_IDENTITY", new InferEntityIdentityRule());

        // Rule 2: Add standard methods to value objects
        rules.put("EXPAND_VALUE_OBJECT_METHODS", new ExpandValueObjectMethodsRule());

        // Rule 3: Infer aggregate boundaries
        rules.put("INFER_AGGREGATE_BOUNDARIES", new InferAggregateBoundariesRule());

        // Rule 4: Add standard fields to domain events
        rules.put("EXPAND_DOMAIN_EVENTS", new ExpandDomainEventsRule());

        // Rule 5: Infer repository interfaces
        rules.put("INFER_REPOSITORIES", new InferRepositoriesRule());
    }

    /**
     * Rule: Infer missing identity fields for entities
     * Transformation: Entity without id → Entity with {EntityName}Id
     */
    private static class InferEntityIdentityRule implements NormalizationRule {

        @Override
        public String getName() {
            return "Infer Entity Identity Fields";
        }

        @Override
        public boolean isApplicable(DomainModel model) {
            return model.getBoundedContexts().stream()
                    .flatMap(ctx -> ctx.getAggregates().stream())
                    .flatMap(agg -> Stream.concat(Stream.of(agg.getRoot()), agg.getEntities().stream()))
                    .anyMatch(entity -> entity.getIdentityField() == null);
        }

        @Override
        public DomainModel apply(DomainModel model) {
            for (BoundedContext context : model.getBoundedContexts()) {
                for (Aggregate aggregate : context.getAggregates()) {
                    // Process aggregate root
                    if (aggregate.getRoot().getIdentityField() == null) {
                        inferIdentityField(aggregate.getRoot());
                    }

                    // Process internal entities
                    for (Entity entity : aggregate.getEntities()) {
                        if (entity.getIdentityField() == null) {
                            inferIdentityField(entity);
                        }
                    }
                }
            }
            return model;
        }

        private void inferIdentityField(Entity entity) {
            // If entity already has an identity field, no need to infer
            if (entity.getIdentityField() != null) {
                return;
            }

            // Create a new identity field
            IdentityField identityField = new IdentityField(
                "id",
                IdentityType.UUID,
                IdGenerationStrategy.CLIENT_GENERATED
            );

            // Note: Since Entity is immutable, we would need to create a new Entity
            // For now, we'll just note that this should be handled in the normalization process
            // This is a placeholder for when proper entity reconstruction is implemented
            System.out.println("Would infer identity field for entity: " + entity.getName());
        }
    }

    /**
     * Rule: Expand value object methods
     * Transformation: ValueObject → ValueObject + equals/hashCode/toString
     */
    private static class ExpandValueObjectMethodsRule implements NormalizationRule {

        @Override
        public String getName() {
            return "Expand Value Object Standard Methods";
        }

        @Override
        public boolean isApplicable(DomainModel model) {
            return model.getBoundedContexts().stream()
                    .flatMap(ctx -> ctx.getValueObjects().stream())
                    .anyMatch(vo -> vo.getMethods().stream()
                            .noneMatch(method -> "equals".equals(method.getName()) ||
                                               "hashCode".equals(method.getName()) ||
                                               "toString".equals(method.getName())));
        }

        @Override
        public DomainModel apply(DomainModel model) {
            for (BoundedContext context : model.getBoundedContexts()) {
                for (ValueObject valueObject : context.getValueObjects()) {
                    addStandardMethods(valueObject);
                }
            }
            return model;
        }

        private void addStandardMethods(ValueObject valueObject) {
            List<Method> methods = valueObject.getMethods();

            // Add equals method if missing
            if (methods.stream().noneMatch(m -> "equals".equals(m.getName()))) {
                Method equalsMethod = Method.builder()
                        .name("equals")
                        .returnType(new PrimitiveType(PrimitiveKind.BOOLEAN))
                        .visibility(Visibility.PUBLIC)
                        .build();
                methods.add(equalsMethod);
            }

            // Add hashCode method if missing
            if (methods.stream().noneMatch(m -> "hashCode".equals(m.getName()))) {
                Method hashCodeMethod = Method.builder()
                        .name("hashCode")
                        .returnType(new PrimitiveType(PrimitiveKind.INT))
                        .visibility(Visibility.PUBLIC)
                        .build();
                methods.add(hashCodeMethod);
            }

            // Add toString method if missing
            if (methods.stream().noneMatch(m -> "toString".equals(m.getName()))) {
                Method toStringMethod = Method.builder()
                        .name("toString")
                        .returnType(new PrimitiveType(PrimitiveKind.STRING))
                        .visibility(Visibility.PUBLIC)
                        .build();
                methods.add(toStringMethod);
            }
        }
    }

    /**
     * Rule: Infer aggregate boundaries
     * Transformation: Isolated entities → Group into aggregates based on relationships
     */
    private static class InferAggregateBoundariesRule implements NormalizationRule {

        @Override
        public String getName() {
            return "Infer Aggregate Boundaries";
        }

        @Override
        public boolean isApplicable(DomainModel model) {
            // Check if there are entities that might need to be grouped
            return model.getBoundedContexts().stream()
                    .anyMatch(ctx -> ctx.getAggregates().size() < getOptimalAggregateCount(ctx));
        }

        @Override
        public DomainModel apply(DomainModel model) {
            // This is a placeholder for more sophisticated aggregate boundary inference
            // In practice, this would analyze entity relationships and coupling
            return model;
        }

        private int getOptimalAggregateCount(BoundedContext context) {
            // Simple heuristic: entities should be grouped in small aggregates
            int totalEntities = context.getAggregates().stream()
                    .mapToInt(agg -> 1 + agg.getEntities().size())
                    .sum();
            return Math.max(1, totalEntities / 3); // Target ~3 entities per aggregate
        }
    }

    /**
     * Rule: Expand domain events with standard fields
     */
    private static class ExpandDomainEventsRule implements NormalizationRule {

        @Override
        public String getName() {
            return "Expand Domain Events with Standard Fields";
        }

        @Override
        public boolean isApplicable(DomainModel model) {
            return model.getBoundedContexts().stream()
                    .flatMap(ctx -> ctx.getDomainEvents().stream())
                    .anyMatch(event -> event.getFields().stream()
                            .noneMatch(field -> "occurredOn".equals(field.getName()) ||
                                               "aggregateId".equals(field.getName())));
        }

        @Override
        public DomainModel apply(DomainModel model) {
            for (BoundedContext context : model.getBoundedContexts()) {
                context.getDomainEvents().forEach(this::addStandardEventFields);
            }
            return model;
        }

        private void addStandardEventFields(uet.ndh.ddsl.core.model.DomainEvent event) {
            List<Field> fields = event.getFields();

            // Add occurredOn field if missing
            if (fields.stream().noneMatch(f -> "occurredOn".equals(f.getName()))) {
                Field occurredOnField = new Field(
                        event.getLocation(),
                        "occurredOn",
                        new PrimitiveType(PrimitiveKind.INSTANT),
                        Visibility.PRIVATE,
                        true,
                        false,
                        null
                );
                fields.add(occurredOnField);
            }

            // Add aggregateId field if missing
            if (fields.stream().noneMatch(f -> "aggregateId".equals(f.getName()))) {
                Field aggregateIdField = new Field(
                        event.getLocation(),
                        "aggregateId",
                        new PrimitiveType(PrimitiveKind.STRING),
                        Visibility.PRIVATE,
                        true,
                        false,
                        null
                );
                fields.add(aggregateIdField);
            }
        }
    }

    /**
     * Rule: Infer repository interfaces for aggregate roots
     */
    private static class InferRepositoriesRule implements NormalizationRule {

        @Override
        public String getName() {
            return "Infer Repository Interfaces";
        }

        @Override
        public boolean isApplicable(DomainModel model) {
            return model.getBoundedContexts().stream()
                    .anyMatch(ctx -> ctx.getAggregates().size() > ctx.getRepositories().size());
        }

        @Override
        public DomainModel apply(DomainModel model) {
            for (BoundedContext context : model.getBoundedContexts()) {
                for (Aggregate aggregate : context.getAggregates()) {
                    String repositoryName = aggregate.getName() + "Repository";

                    // Check if repository already exists
                    boolean repositoryExists = context.getRepositories().stream()
                            .anyMatch(repo -> repositoryName.equals(repo.getName()));

                    if (!repositoryExists) {
                        // Create repository interface would go here
                        // This is a simplified placeholder
                        System.out.printf("     → Would create repository: %s\n", repositoryName);
                    }
                }
            }
            return model;
        }
    }

    /**
     * Interface for normalization rules
     */
    private interface NormalizationRule {
        String getName();
        boolean isApplicable(DomainModel model);
        DomainModel apply(DomainModel model);
    }

    /**
     * Result of normalization process
     */
    @Data
    public static class NormalizedDomainModel {
        private final DomainModel model;
        private final List<String> appliedTransformations;

        public boolean wasTransformed() {
            return !appliedTransformations.isEmpty();
        }

        public String getTransformationSummary() {
            return String.format("Applied %d transformations: %s",
                               appliedTransformations.size(),
                               String.join(", ", appliedTransformations));
        }
    }
}
