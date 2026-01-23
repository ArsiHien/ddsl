package uet.ndh.ddsl.core.model;

import uet.ndh.ddsl.core.*;
import uet.ndh.ddsl.core.application.applicationservice.ApplicationService;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.factory.Factory;
import uet.ndh.ddsl.core.model.repository.RepositoryInterface;
import uet.ndh.ddsl.core.model.specification.Specification;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bounded context within a domain model.
 * Contains all domain objects and services within a specific business boundary.
 */
public class BoundedContext extends ASTNode {
    private final String name;
    private final String packageName;
    private final List<Aggregate> aggregates;
    private final List<ValueObject> valueObjects;
    private final List<DomainService> domainServices;
    private final List<DomainEvent> domainEvents;
    private final List<RepositoryInterface> repositories;
    private final List<Factory> factories;
    private final List<Specification> specifications;
    private final List<ApplicationService> applicationServices;

    public BoundedContext(SourceLocation location, String name, String packageName) {
        super(location);
        this.name = name;
        this.packageName = packageName;
        this.aggregates = new ArrayList<>();
        this.valueObjects = new ArrayList<>();
        this.domainServices = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.repositories = new ArrayList<>();
        this.factories = new ArrayList<>();
        this.specifications = new ArrayList<>();
        this.applicationServices = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<Aggregate> getAggregates() {
        return new ArrayList<>(aggregates);
    }

    public List<ValueObject> getValueObjects() {
        return new ArrayList<>(valueObjects);
    }

    public List<DomainService> getDomainServices() {
        return new ArrayList<>(domainServices);
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public List<RepositoryInterface> getRepositories() {
        return new ArrayList<>(repositories);
    }

    public List<Factory> getFactories() {
        return new ArrayList<>(factories);
    }

    public List<Specification> getSpecifications() {
        return new ArrayList<>(specifications);
    }

    public List<ApplicationService> getApplicationServices() {
        return new ArrayList<>(applicationServices);
    }

    // Add methods
    public void addAggregate(Aggregate aggregate) {
        aggregates.add(aggregate);
    }

    public void addValueObject(ValueObject valueObject) {
        valueObjects.add(valueObject);
    }

    public void addDomainService(DomainService service) {
        domainServices.add(service);
    }

    public void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public void addRepository(RepositoryInterface repository) {
        repositories.add(repository);
    }

    public void addFactory(Factory factory) {
        factories.add(factory);
    }

    public void addSpecification(Specification specification) {
        specifications.add(specification);
    }

    public void addApplicationService(ApplicationService service) {
        applicationServices.add(service);
    }

    @Override
    public void accept(CodeGenVisitor visitor) {
        visitor.visitBoundedContext(this);
    }

    /**
     * Create a deep copy of this bounded context for normalization.
     */
    public BoundedContext copy() {
        BoundedContext copy = new BoundedContext(this.location, this.name, this.packageName);

        // Copy all aggregates
        for (Aggregate aggregate : this.aggregates) {
            copy.addAggregate(aggregate.copy());
        }

        // Copy all value objects
        for (ValueObject valueObject : this.valueObjects) {
            copy.addValueObject(valueObject.copy());
        }

        // Copy domain services
        for (DomainService service : this.domainServices) {
            copy.addDomainService(service.copy());
        }

        // Copy domain events
        for (DomainEvent event : this.domainEvents) {
            copy.addDomainEvent(event.copy());
        }

        // Copy repositories
        for (RepositoryInterface repo : this.repositories) {
            copy.addRepository(repo.copy());
        }

        // Copy factories
        for (Factory factory : this.factories) {
            copy.addFactory(factory.copy());
        }

        // Copy specifications
        for (Specification spec : this.specifications) {
            copy.addSpecification(spec.copy());
        }

        // Copy application services
        for (ApplicationService service : this.applicationServices) {
            copy.addApplicationService(service.copy());
        }

        copy.documentation = this.documentation;
        return copy;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Bounded context name cannot be empty", getLocation()));
        }

        if (packageName == null || packageName.trim().isEmpty()) {
            errors.add(new ValidationError("Package name cannot be empty", getLocation()));
        }

        // Validate all contained elements
        aggregates.forEach(aggregate -> errors.addAll(aggregate.validate()));
        valueObjects.forEach(vo -> errors.addAll(vo.validate()));
        domainServices.forEach(service -> errors.addAll(service.validate()));
        domainEvents.forEach(event -> errors.addAll(event.validate()));
        repositories.forEach(repo -> errors.addAll(repo.validate()));
        factories.forEach(factory -> errors.addAll(factory.validate()));
        specifications.forEach(spec -> errors.addAll(spec.validate()));
        applicationServices.forEach(service -> errors.addAll(service.validate()));

        return errors;
    }
}
