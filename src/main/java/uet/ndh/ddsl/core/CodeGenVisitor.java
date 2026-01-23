package uet.ndh.ddsl.core;

import uet.ndh.ddsl.core.model.*;
import uet.ndh.ddsl.core.application.applicationservice.ApplicationService;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.entity.Entity;
import uet.ndh.ddsl.core.model.factory.Factory;
import uet.ndh.ddsl.core.model.repository.RepositoryInterface;
import uet.ndh.ddsl.core.model.specification.Specification;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;

/**
 * Visitor interface for code generation from AST nodes.
 * Implements the Visitor pattern to traverse and generate code from the AST.
 */
public interface CodeGenVisitor {
    void visitDomainModel(DomainModel model);
    void visitBoundedContext(BoundedContext context);
    void visitAggregate(Aggregate aggregate);
    void visitEntity(Entity entity);
    void visitValueObject(ValueObject valueObject);
    void visitDomainService(DomainService service);
    void visitDomainEvent(DomainEvent event);
    void visitRepositoryInterface(RepositoryInterface repository);
    void visitFactory(Factory factory);
    void visitSpecification(Specification specification);
    void visitApplicationService(ApplicationService service);
    void visitMethod(Method method);
    void visitField(Field field);
}
