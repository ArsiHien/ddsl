package uet.ndh.ddsl.ast.model;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.enumeration.EnumDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.factory.FactoryDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.model.specification.SpecificationDecl;
import uet.ndh.ddsl.ast.model.statemachine.StateMachineDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.ast.application.ApplicationServiceDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a Bounded Context - a linguistic and conceptual boundary
 * for a set of domain models.
 * 
 * Pure data record - no logic except accept().
 */
public record BoundedContextDecl(
    SourceSpan span,
    String name,
    List<ModuleDecl> modules,
    List<AggregateDecl> aggregates,
    List<EnumDecl> enums,
    List<ValueObjectDecl> valueObjects,
    List<DomainServiceDecl> domainServices,
    List<DomainEventDecl> domainEvents,
    List<RepositoryDecl> repositories,
    List<FactoryDecl> factories,
    List<StateMachineDecl> stateMachines,
    List<SpecificationDecl> specifications,
    List<ApplicationServiceDecl> applicationServices,
    String documentation
) implements AstNode {
    
    public BoundedContextDecl {
        modules = modules != null ? List.copyOf(modules) : List.of();
        aggregates = aggregates != null ? List.copyOf(aggregates) : List.of();
        enums = enums != null ? List.copyOf(enums) : List.of();
        valueObjects = valueObjects != null ? List.copyOf(valueObjects) : List.of();
        domainServices = domainServices != null ? List.copyOf(domainServices) : List.of();
        domainEvents = domainEvents != null ? List.copyOf(domainEvents) : List.of();
        repositories = repositories != null ? List.copyOf(repositories) : List.of();
        factories = factories != null ? List.copyOf(factories) : List.of();
        stateMachines = stateMachines != null ? List.copyOf(stateMachines) : List.of();
        specifications = specifications != null ? List.copyOf(specifications) : List.of();
        applicationServices = applicationServices != null ? List.copyOf(applicationServices) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitBoundedContext(this);
    }
}
