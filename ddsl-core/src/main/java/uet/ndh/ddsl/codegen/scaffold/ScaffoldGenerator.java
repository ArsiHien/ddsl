package uet.ndh.ddsl.codegen.scaffold;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.codegen.CodeArtifact;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generator for static DDD building block scaffolding using FreeMarker templates.
 * 
 * This generator produces the "frame" of the application:
 * - Base interfaces (AggregateRoot, Entity, ValueObject, DomainEvent)
 * - Abstract base classes
 * - Common utilities and markers
 * 
 * These are semi-static components that rarely change based on DDSL input.
 * JavaPoet-generated classes then implement/extend these base types.
 * 
 * Usage:
 * <pre>
 * ScaffoldGenerator generator = new ScaffoldGenerator();
 * List<CodeArtifact> artifacts = generator.generateBaseScaffolding("com.example.domain");
 * </pre>
 */
@Component
@Slf4j
public class ScaffoldGenerator {
    
    private final Configuration freemarkerConfig;
    
    public ScaffoldGenerator() {
        this.freemarkerConfig = createFreemarkerConfiguration();
    }
    
    /**
     * Generate all base scaffolding artifacts for a domain project.
     * 
     * @param basePackage The base package for generated code (e.g., "com.example.domain")
     * @return List of code artifacts for base interfaces and classes
     */
    public List<CodeArtifact> generateBaseScaffolding(String basePackage) {
        List<CodeArtifact> artifacts = new ArrayList<>();
        String sharedPackage = basePackage + ".shared";
        
        // Create template context
        TemplateContext context = new TemplateContext(basePackage, sharedPackage);
        
        // Generate base interfaces
        artifacts.add(generateAggregateRootInterface(context));
        artifacts.add(generateEntityInterface(context));
        artifacts.add(generateValueObjectInterface(context));
        artifacts.add(generateDomainEventInterface(context));
        
        // Generate repository interface
        artifacts.add(generateRepositoryInterface(context));
        
        // Generate base exception
        artifacts.add(generateDomainException(context));
        
        // Generate specification pattern base
        artifacts.add(generateSpecificationInterface(context));
        
        log.info("Generated {} scaffold artifacts for package {}", artifacts.size(), basePackage);
        return artifacts;
    }
    
    /**
     * Generate AggregateRoot interface.
     */
    private CodeArtifact generateAggregateRootInterface(TemplateContext context) {
        Map<String, Object> model = Map.of(
            "packageName", context.sharedPackage(),
            "interfaceName", "AggregateRoot",
            "description", "Marker interface for DDD Aggregate Roots. " +
                          "Aggregate roots are the entry points to aggregates and ensure consistency boundaries."
        );
        
        String content = processTemplate("aggregate-root.ftl", model);
        return new CodeArtifact(
            "AggregateRoot",
            context.sharedPackage(),
            content,
            CodeArtifact.ArtifactType.INTERFACE
        );
    }
    
    /**
     * Generate Entity interface.
     */
    private CodeArtifact generateEntityInterface(TemplateContext context) {
        Map<String, Object> model = Map.of(
            "packageName", context.sharedPackage(),
            "interfaceName", "Entity",
            "description", "Marker interface for DDD Entities. " +
                          "Entities have identity that persists through time and state changes."
        );
        
        String content = processTemplate("entity.ftl", model);
        return new CodeArtifact(
            "Entity",
            context.sharedPackage(),
            content,
            CodeArtifact.ArtifactType.INTERFACE
        );
    }
    
    /**
     * Generate ValueObject interface.
     */
    private CodeArtifact generateValueObjectInterface(TemplateContext context) {
        Map<String, Object> model = Map.of(
            "packageName", context.sharedPackage(),
            "interfaceName", "ValueObject",
            "description", "Marker interface for DDD Value Objects. " +
                          "Value objects are immutable and compared by their attributes, not identity."
        );
        
        String content = processTemplate("value-object.ftl", model);
        return new CodeArtifact(
            "ValueObject",
            context.sharedPackage(),
            content,
            CodeArtifact.ArtifactType.INTERFACE
        );
    }
    
    /**
     * Generate DomainEvent interface.
     */
    private CodeArtifact generateDomainEventInterface(TemplateContext context) {
        Map<String, Object> model = Map.of(
            "packageName", context.sharedPackage(),
            "interfaceName", "DomainEvent",
            "description", "Marker interface for Domain Events. " +
                          "Domain events capture something that happened in the domain that domain experts care about."
        );
        
        String content = processTemplate("domain-event.ftl", model);
        return new CodeArtifact(
            "DomainEvent",
            context.sharedPackage(),
            content,
            CodeArtifact.ArtifactType.INTERFACE
        );
    }
    
    /**
     * Generate Repository interface.
     */
    private CodeArtifact generateRepositoryInterface(TemplateContext context) {
        Map<String, Object> model = Map.of(
            "packageName", context.sharedPackage(),
            "interfaceName", "Repository",
            "description", "Base interface for DDD Repositories. " +
                          "Repositories provide collection-like access to aggregates."
        );
        
        String content = processTemplate("repository.ftl", model);
        return new CodeArtifact(
            "Repository",
            context.sharedPackage(),
            content,
            CodeArtifact.ArtifactType.INTERFACE
        );
    }
    
    /**
     * Generate DomainException class.
     */
    private CodeArtifact generateDomainException(TemplateContext context) {
        Map<String, Object> model = Map.of(
            "packageName", context.sharedPackage(),
            "className", "DomainException",
            "description", "Base exception for domain layer errors."
        );
        
        String content = processTemplate("domain-exception.ftl", model);
        return new CodeArtifact(
            "DomainException",
            context.sharedPackage(),
            content,
            CodeArtifact.ArtifactType.CLASS
        );
    }
    
    /**
     * Generate Specification interface.
     */
    private CodeArtifact generateSpecificationInterface(TemplateContext context) {
        Map<String, Object> model = Map.of(
            "packageName", context.sharedPackage(),
            "interfaceName", "Specification",
            "description", "Specification pattern interface for business rule encapsulation."
        );
        
        String content = processTemplate("specification.ftl", model);
        return new CodeArtifact(
            "Specification",
            context.sharedPackage(),
            content,
            CodeArtifact.ArtifactType.INTERFACE
        );
    }
    
    /**
     * Process a FreeMarker template with the given model.
     */
    private String processTemplate(String templateName, Map<String, Object> model) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            log.error("Failed to process template: {}", templateName, e);
            // Return fallback inline generated code
            return generateFallbackCode(templateName, model);
        }
    }
    
    /**
     * Generate fallback code when template is not available.
     */
    private String generateFallbackCode(String templateName, Map<String, Object> model) {
        String packageName = (String) model.get("packageName");
        String typeName = model.containsKey("interfaceName") 
            ? (String) model.get("interfaceName") 
            : (String) model.get("className");
        String description = (String) model.get("description");
        
        // Generate inline code based on template type
        return switch (templateName) {
            case "aggregate-root.ftl" -> generateAggregateRootCode(packageName, description);
            case "entity.ftl" -> generateEntityCode(packageName, description);
            case "value-object.ftl" -> generateValueObjectCode(packageName, description);
            case "domain-event.ftl" -> generateDomainEventCode(packageName, description);
            case "repository.ftl" -> generateRepositoryCode(packageName, description);
            case "domain-exception.ftl" -> generateDomainExceptionCode(packageName, description);
            case "specification.ftl" -> generateSpecificationCode(packageName, description);
            default -> "// Template not found: " + templateName;
        };
    }
    
    private String generateAggregateRootCode(String packageName, String description) {
        return """
            package %s;
            
            import java.util.List;
            
            /**
             * %s
             *
             * @param <ID> The type of the aggregate's identity
             */
            public interface AggregateRoot<ID> extends Entity<ID> {
                
                /**
                 * Get all domain events that have been registered by this aggregate.
                 */
                List<DomainEvent> getDomainEvents();
                
                /**
                 * Clear all registered domain events.
                 * Typically called after events have been published.
                 */
                void clearDomainEvents();
            }
            """.formatted(packageName, description);
    }
    
    private String generateEntityCode(String packageName, String description) {
        return """
            package %s;
            
            /**
             * %s
             *
             * @param <ID> The type of the entity's identity
             */
            public interface Entity<ID> {
                
                /**
                 * Get the unique identifier of this entity.
                 */
                ID getId();
            }
            """.formatted(packageName, description);
    }
    
    private String generateValueObjectCode(String packageName, String description) {
        return """
            package %s;
            
            /**
             * %s
             *
             * Value objects should:
             * - Be immutable
             * - Have structural equality (equals based on all attributes)
             * - Have no identity
             */
            public interface ValueObject {
                // Marker interface
            }
            """.formatted(packageName, description);
    }
    
    private String generateDomainEventCode(String packageName, String description) {
        return """
            package %s;
            
            import java.time.Instant;
            
            /**
             * %s
             */
            public interface DomainEvent {
                
                /**
                 * When this event occurred.
                 */
                Instant occurredAt();
            }
            """.formatted(packageName, description);
    }
    
    private String generateRepositoryCode(String packageName, String description) {
        return """
            package %s;
            
            import java.util.Optional;
            
            /**
             * %s
             *
             * @param <T> The aggregate root type
             * @param <ID> The aggregate's identity type
             */
            public interface Repository<T extends AggregateRoot<ID>, ID> {
                
                /**
                 * Find an aggregate by its identity.
                 */
                Optional<T> findById(ID id);
                
                /**
                 * Check if an aggregate exists with the given identity.
                 */
                boolean existsById(ID id);
                
                /**
                 * Save an aggregate (insert or update).
                 */
                T save(T aggregate);
                
                /**
                 * Delete an aggregate by its identity.
                 */
                void deleteById(ID id);
            }
            """.formatted(packageName, description);
    }
    
    private String generateDomainExceptionCode(String packageName, String description) {
        return """
            package %s;
            
            /**
             * %s
             */
            public class DomainException extends RuntimeException {
                
                public DomainException(String message) {
                    super(message);
                }
                
                public DomainException(String message, Throwable cause) {
                    super(message, cause);
                }
            }
            """.formatted(packageName, description);
    }
    
    private String generateSpecificationCode(String packageName, String description) {
        return """
            package %s;
            
            import java.util.function.Predicate;
            
            /**
             * %s
             *
             * @param <T> The type of object this specification applies to
             */
            @FunctionalInterface
            public interface Specification<T> extends Predicate<T> {
                
                /**
                 * Check if the candidate satisfies this specification.
                 */
                boolean isSatisfiedBy(T candidate);
                
                @Override
                default boolean test(T candidate) {
                    return isSatisfiedBy(candidate);
                }
                
                /**
                 * Combine this specification with another using AND logic.
                 */
                default Specification<T> and(Specification<T> other) {
                    return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
                }
                
                /**
                 * Combine this specification with another using OR logic.
                 */
                default Specification<T> or(Specification<T> other) {
                    return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
                }
                
                /**
                 * Negate this specification.
                 */
                default Specification<T> not() {
                    return candidate -> !this.isSatisfiedBy(candidate);
                }
            }
            """.formatted(packageName, description);
    }
    
    /**
     * Create FreeMarker configuration.
     */
    private Configuration createFreemarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates/scaffold");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        return cfg;
    }
}
