package uet.ndh.ddsl.codegen.poet;

import lombok.extern.slf4j.Slf4j;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.model.specification.SpecificationDecl;
import uet.ndh.ddsl.ast.model.statemachine.StateMachineDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.scaffold.ScaffoldGenerator;
import uet.ndh.ddsl.codegen.poet.translator.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordination module for JavaPoet-based code generation.
 * 
 * This is the main entry point for generating domain model code from DDSL AST.
 * It orchestrates the various translators and produces a complete set of CodeArtifacts.
 * 
 * The module handles:
 * - Aggregate translation (root entities, nested entities, value objects, events)
 * - Standalone entity translation
 * - Domain service translation
 * - Repository interface generation
 * 
 * Generated classes reference the scaffold-generated base interfaces:
 * - Aggregates implement AggregateRoot<ID>
 * - Entities implement Entity<ID>
 * - Value objects implement ValueObject
 * - Events implement DomainEvent
 * 
 * Usage:
 * <pre>
 * PoetModule poet = new PoetModule("com.example.domain");
 * List<CodeArtifact> artifacts = poet.generateFromModel(domainModel);
 * </pre>
 */
@Slf4j
public class PoetModule {
    
    private final TypeMapper typeMapper;
    private final AggregateTranslator aggregateTranslator;
    private final EntityTranslator entityTranslator;
    private final ServiceTranslator serviceTranslator;
    private final SpecificationTranslator specificationTranslator;
    private final EnumTranslator enumTranslator;
    private final StateMachineTranslator stateMachineTranslator;
    private final ScaffoldGenerator scaffoldGenerator;
    
    public PoetModule(String basePackage) {
        this.typeMapper = new TypeMapper(basePackage);
        String normalizedBasePackage = this.typeMapper.getBasePackage();
        this.aggregateTranslator = new AggregateTranslator(typeMapper);
        this.entityTranslator = new EntityTranslator(typeMapper);
        this.serviceTranslator = new ServiceTranslator(typeMapper);
        this.specificationTranslator = new SpecificationTranslator(typeMapper);
        this.enumTranslator = new EnumTranslator(typeMapper, normalizedBasePackage);
        this.stateMachineTranslator = new StateMachineTranslator(typeMapper, normalizedBasePackage);
        this.scaffoldGenerator = new ScaffoldGenerator();
    }
    
    /**
     * Generate all code artifacts from a complete domain model.
     * 
     * @param model The parsed DDSL domain model
     * @return List of all generated code artifacts
     */
    public List<CodeArtifact> generateFromModel(DomainModel model) {
        List<CodeArtifact> artifacts = new ArrayList<>();

        // Generate shared scaffolding (AggregateRoot, Entity, ValueObject, DomainEvent, etc.)
        artifacts.addAll(scaffoldGenerator.generateBaseScaffolding(typeMapper.getBasePackage()));
        
        // Register all domain types for proper import resolution
        registerDomainTypes(model);
        
        // Process bounded contexts
        for (var boundedContext : model.boundedContexts()) {
            // Generate aggregates directly in bounded context
            for (AggregateDecl aggregate : boundedContext.aggregates()) {
                artifacts.addAll(generateAggregate(aggregate));
            }
            
            // Generate standalone value objects
            for (ValueObjectDecl valueObject : boundedContext.valueObjects()) {
                artifacts.add(generateValueObject(valueObject));
            }
            
            // Generate domain events
            for (DomainEventDecl event : boundedContext.domainEvents()) {
                artifacts.add(generateDomainEvent(event));
            }
            
            // Generate domain services
            for (DomainServiceDecl service : boundedContext.domainServices()) {
                artifacts.add(generateDomainService(service));
            }
            
            // Generate repository interfaces
            for (RepositoryDecl repository : boundedContext.repositories()) {
                artifacts.add(generateRepository(repository));
            }
            
            // Generate specifications
            for (SpecificationDecl specification : boundedContext.specifications()) {
                artifacts.add(generateSpecification(specification));
            }
        }
        
        log.info("Generated {} artifacts from domain model", artifacts.size());
        return artifacts;
    }
    
    /**
     * Generate all artifacts for an aggregate.
     */
    public List<CodeArtifact> generateAggregate(AggregateDecl aggregate) {
        log.debug("Generating aggregate: {}", aggregate.name());
        return aggregateTranslator.translate(aggregate);
    }
    
    /**
     * Generate a single entity.
     */
    public CodeArtifact generateEntity(EntityDecl entity) {
        log.debug("Generating entity: {}", entity.name());
        return entityTranslator.translate(entity);
    }
    
    /**
     * Generate a value object.
     */
    public CodeArtifact generateValueObject(ValueObjectDecl valueObject) {
        log.debug("Generating value object: {}", valueObject.name());
        return entityTranslator.translateValueObject(valueObject);
    }
    
    /**
     * Generate a domain event.
     */
    public CodeArtifact generateDomainEvent(DomainEventDecl event) {
        log.debug("Generating domain event: {}", event.name());
        return entityTranslator.translateDomainEvent(event);
    }
    
    /**
     * Generate a domain service.
     */
    public CodeArtifact generateDomainService(DomainServiceDecl service) {
        log.debug("Generating domain service: {}", service.name());
        serviceTranslator.clearTranslationErrors();
        CodeArtifact artifact = serviceTranslator.translate(service);
        
        // Log any translation errors (unresolved types, etc.)
        for (String error : serviceTranslator.getTranslationErrors()) {
            log.error("Code generation error: {}", error);
        }
        
        return artifact;
    }
    
    /**
     * Generate a repository interface.
     */
    public CodeArtifact generateRepository(RepositoryDecl repository) {
        log.debug("Generating repository: {}", repository.name());
        return serviceTranslator.translateRepository(repository);
    }
    
    /**
     * Generate a specification class (DDD Specification pattern).
     */
    public CodeArtifact generateSpecification(SpecificationDecl specification) {
        log.debug("Generating specification: {}", specification.name());
        return specificationTranslator.translate(specification);
    }
    
    /**
     * Generate state machine artifacts (state enum and helper class).
     * 
     * @param stateMachine The state machine declaration
     * @param entityName The owning entity name
     * @return List of generated artifacts (enum, optional helper)
     */
    public List<CodeArtifact> generateStateMachine(StateMachineDecl stateMachine, String entityName) {
        log.debug("Generating state machine for: {}", entityName);
        return stateMachineTranslator.translateStateMachine(stateMachine, entityName);
    }
    
    /**
     * Generate a state enum from a state machine.
     * 
     * @param stateMachine The state machine declaration
     * @param entityName The owning entity name
     * @return The generated enum artifact
     */
    public CodeArtifact generateStateEnum(StateMachineDecl stateMachine, String entityName) {
        log.debug("Generating state enum for: {}", entityName);
        return enumTranslator.translateStateMachineToEnum(stateMachine, entityName);
    }
    
    /**
     * Generate an enum from a list of allowed values.
     * 
     * @param enumName Name of the enum
     * @param values List of allowed values
     * @param documentation Optional documentation
     * @return The generated enum artifact
     */
    public CodeArtifact generateEnum(String enumName, List<String> values, String documentation) {
        log.debug("Generating enum: {}", enumName);
        return enumTranslator.translateValuesToEnum(enumName, values, documentation);
    }
    
    /**
     * Get the state machine translator for advanced usage.
     */
    public StateMachineTranslator getStateMachineTranslator() {
        return stateMachineTranslator;
    }
    
    /**
     * Get the enum translator for advanced usage.
     */
    public EnumTranslator getEnumTranslator() {
        return enumTranslator;
    }
    
    /**
     * Register all domain types for import resolution.
     */
    private void registerDomainTypes(DomainModel model) {
        for (var boundedContext : model.boundedContexts()) {
            String contextPackage = typeMapper.getBasePackage();
            String standaloneModelPackage = typeMapper.packageForStandaloneModel();
            String standaloneEventPackage = typeMapper.packageForStandaloneEvents();
            String specificationPackage = typeMapper.packageForSpecifications();
            
            // Register aggregate types
            for (AggregateDecl aggregate : boundedContext.aggregates()) {
                String aggPackage = contextPackage + "." + aggregate.name().toLowerCase();
                
                if (aggregate.root() != null) {
                    typeMapper.registerDomainType(aggregate.root().name(), aggPackage);
                }
                
                for (EntityDecl entity : aggregate.entities()) {
                    typeMapper.registerDomainType(entity.name(), aggPackage);
                }
                
                for (ValueObjectDecl vo : aggregate.valueObjects()) {
                    typeMapper.registerDomainType(vo.name(), aggPackage);
                }
                
                for (DomainEventDecl event : aggregate.events()) {
                    typeMapper.registerDomainType(event.name(), aggPackage);
                }
            }
            
            // Register standalone types
            for (ValueObjectDecl vo : boundedContext.valueObjects()) {
                typeMapper.registerDomainType(vo.name(), standaloneModelPackage);
            }
            
            for (DomainEventDecl event : boundedContext.domainEvents()) {
                typeMapper.registerDomainType(event.name(), standaloneEventPackage);
            }
            
            // Register specification types
            for (SpecificationDecl spec : boundedContext.specifications()) {
                typeMapper.registerDomainType(spec.name(), specificationPackage);
            }
        }
    }
    
    /**
     * Get the type mapper for external use.
     */
    public TypeMapper getTypeMapper() {
        return typeMapper;
    }
}
