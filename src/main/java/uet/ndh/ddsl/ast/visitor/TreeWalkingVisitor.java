package uet.ndh.ddsl.ast.visitor;

// Application layer
import uet.ndh.ddsl.ast.application.ApplicationServiceDecl;
import uet.ndh.ddsl.ast.application.UseCaseDecl;

// Behavior layer
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;

// Expressions
import uet.ndh.ddsl.ast.expr.*;

// Members
import uet.ndh.ddsl.ast.member.*;

// Model declarations
import uet.ndh.ddsl.ast.model.BoundedContextDecl;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.ast.model.ModuleDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.factory.FactoryDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.model.specification.SpecificationDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;

// Statements
import uet.ndh.ddsl.ast.stmt.*;

/**
 * A visitor that traverses the entire AST tree, visiting all children.
 * Useful as a base class for visitors that need to walk the whole tree.
 * 
 * This is similar to Crafting Interpreters' approach where you
 * override only the nodes you care about.
 */
public abstract class TreeWalkingVisitor<R> extends BaseAstVisitor<R> {
    
    /**
     * Combine results from child visits.
     * Override to aggregate results differently.
     */
    protected R aggregateResult(R current, R next) {
        return next != null ? next : current;
    }
    
    // ===== Model Declarations =====
    
    @Override
    public R visitDomainModel(DomainModel model) {
        R result = defaultResult();
        for (var context : model.boundedContexts()) {
            result = aggregateResult(result, context.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitBoundedContext(BoundedContextDecl decl) {
        R result = defaultResult();
        for (var module : decl.modules()) {
            result = aggregateResult(result, module.accept(this));
        }
        for (var aggregate : decl.aggregates()) {
            result = aggregateResult(result, aggregate.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitModule(ModuleDecl decl) {
        R result = defaultResult();
        for (var context : decl.boundedContexts()) {
            result = aggregateResult(result, context.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitAggregate(AggregateDecl decl) {
        R result = defaultResult();
        if (decl.root() != null) {
            result = aggregateResult(result, decl.root().accept(this));
        }
        for (var entity : decl.entities()) {
            result = aggregateResult(result, entity.accept(this));
        }
        for (var valueObject : decl.valueObjects()) {
            result = aggregateResult(result, valueObject.accept(this));
        }
        for (var behavior : decl.behaviors()) {
            result = aggregateResult(result, behavior.accept(this));
        }
        for (var invariant : decl.invariants()) {
            result = aggregateResult(result, invariant.accept(this));
        }
        for (var factory : decl.factories()) {
            result = aggregateResult(result, factory.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitEntity(EntityDecl decl) {
        R result = defaultResult();
        for (var field : decl.fields()) {
            result = aggregateResult(result, field.accept(this));
        }
        for (var behavior : decl.behaviors()) {
            result = aggregateResult(result, behavior.accept(this));
        }
        for (var invariant : decl.invariants()) {
            result = aggregateResult(result, invariant.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitValueObject(ValueObjectDecl decl) {
        R result = defaultResult();
        for (var field : decl.fields()) {
            result = aggregateResult(result, field.accept(this));
        }
        for (var invariant : decl.invariants()) {
            result = aggregateResult(result, invariant.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitDomainService(DomainServiceDecl decl) {
        R result = defaultResult();
        for (var method : decl.methods()) {
            result = aggregateResult(result, method.accept(this));
        }
        for (var dependency : decl.dependencies()) {
            result = aggregateResult(result, dependency.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitDomainEvent(DomainEventDecl decl) {
        R result = defaultResult();
        for (var field : decl.fields()) {
            result = aggregateResult(result, field.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitRepository(RepositoryDecl decl) {
        R result = defaultResult();
        // Repository methods are visited separately if needed
        return result;
    }
    
    @Override
    public R visitFactory(FactoryDecl decl) {
        R result = defaultResult();
        // Factory methods are visited separately if needed
        return result;
    }
    
    @Override
    public R visitSpecification(SpecificationDecl decl) {
        R result = defaultResult();
        for (var param : decl.parameters()) {
            result = aggregateResult(result, param.accept(this));
        }
        return result;
    }
    
    // ===== Application Layer =====
    
    @Override
    public R visitApplicationService(ApplicationServiceDecl decl) {
        R result = defaultResult();
        for (var useCase : decl.useCases()) {
            result = aggregateResult(result, useCase.accept(this));
        }
        for (var dependency : decl.dependencies()) {
            result = aggregateResult(result, dependency.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitUseCase(UseCaseDecl decl) {
        R result = defaultResult();
        for (var input : decl.inputs()) {
            result = aggregateResult(result, input.accept(this));
        }
        if (decl.body() != null) {
            result = aggregateResult(result, decl.body().accept(this));
        }
        for (var behavior : decl.behaviors()) {
            result = aggregateResult(result, behavior.accept(this));
        }
        return result;
    }
    
    // ===== Members =====
    
    @Override
    public R visitMethod(MethodDecl decl) {
        R result = defaultResult();
        for (var param : decl.parameters()) {
            result = aggregateResult(result, param.accept(this));
        }
        if (decl.body() != null) {
            result = aggregateResult(result, decl.body().accept(this));
        }
        return result;
    }
    
    @Override
    public R visitConstructor(ConstructorDecl decl) {
        R result = defaultResult();
        for (var param : decl.parameters()) {
            result = aggregateResult(result, param.accept(this));
        }
        if (decl.body() != null) {
            result = aggregateResult(result, decl.body().accept(this));
        }
        return result;
    }
    
    // ===== Behaviors =====
    
    @Override
    public R visitBehavior(BehaviorDecl decl) {
        R result = defaultResult();
        for (var param : decl.parameters()) {
            result = aggregateResult(result, param.accept(this));
        }
        // Clauses don't implement AstNode, so we don't visit them here
        // Subclasses can override for clause-specific traversal
        return result;
    }
    
    // ===== Statements =====
    
    @Override
    public R visitBlockStmt(BlockStmt stmt) {
        R result = defaultResult();
        for (var s : stmt.statements()) {
            result = aggregateResult(result, s.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitExpressionStmt(ExpressionStmt stmt) {
        return stmt.expression().accept(this);
    }
    
    @Override
    public R visitIfStmt(IfStmt stmt) {
        R result = stmt.condition().accept(this);
        result = aggregateResult(result, stmt.thenBranch().accept(this));
        if (stmt.hasElseBranch()) {
            result = aggregateResult(result, stmt.elseBranch().accept(this));
        }
        return result;
    }
    
    @Override
    public R visitForEachStmt(ForEachStmt stmt) {
        R result = stmt.collection().accept(this);
        result = aggregateResult(result, stmt.body().accept(this));
        return result;
    }
    
    @Override
    public R visitReturnStmt(ReturnStmt stmt) {
        if (stmt.hasValue()) {
            return stmt.value().accept(this);
        }
        return defaultResult();
    }
    
    @Override
    public R visitAssignmentStmt(AssignmentStmt stmt) {
        R result = stmt.target().accept(this);
        result = aggregateResult(result, stmt.value().accept(this));
        return result;
    }
    
    @Override
    public R visitVariableDeclarationStmt(VariableDeclarationStmt stmt) {
        if (stmt.initializer() != null) {
            return stmt.initializer().accept(this);
        }
        return defaultResult();
    }
    
    // ===== Expressions =====
    
    @Override
    public R visitBinaryExpr(BinaryExpr expr) {
        R result = expr.left().accept(this);
        result = aggregateResult(result, expr.right().accept(this));
        return result;
    }
    
    @Override
    public R visitUnaryExpr(UnaryExpr expr) {
        return expr.operand().accept(this);
    }
    
    @Override
    public R visitMethodCallExpr(MethodCallExpr expr) {
        R result = defaultResult();
        if (expr.hasReceiver()) {
            result = expr.receiver().accept(this);
        }
        for (var arg : expr.arguments()) {
            result = aggregateResult(result, arg.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitFieldAccessExpr(FieldAccessExpr expr) {
        return expr.object().accept(this);
    }
    
    @Override
    public R visitNewInstanceExpr(NewInstanceExpr expr) {
        R result = defaultResult();
        for (var arg : expr.arguments()) {
            result = aggregateResult(result, arg.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitListExpr(ListExpr expr) {
        R result = defaultResult();
        for (var elem : expr.elements()) {
            result = aggregateResult(result, elem.accept(this));
        }
        return result;
    }
    
    @Override
    public R visitMapExpr(MapExpr expr) {
        R result = defaultResult();
        for (var entry : expr.entries().entrySet()) {
            result = aggregateResult(result, entry.getKey().accept(this));
            result = aggregateResult(result, entry.getValue().accept(this));
        }
        return result;
    }
    
    @Override
    public R visitTernaryExpr(TernaryExpr expr) {
        R result = expr.condition().accept(this);
        result = aggregateResult(result, expr.thenExpr().accept(this));
        result = aggregateResult(result, expr.elseExpr().accept(this));
        return result;
    }
    
    @Override
    public R visitTypeRef(uet.ndh.ddsl.ast.common.TypeRef typeRef) {
        return defaultResult();
    }
}
