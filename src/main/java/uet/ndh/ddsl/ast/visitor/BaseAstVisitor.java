package uet.ndh.ddsl.ast.visitor;

// Application layer
import uet.ndh.ddsl.ast.application.ApplicationServiceDecl;
import uet.ndh.ddsl.ast.application.UseCaseDecl;

// Behavior layer
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.behavior.clause.EmitClause;
import uet.ndh.ddsl.ast.behavior.clause.GivenClause;
import uet.ndh.ddsl.ast.behavior.clause.RequireClause;
import uet.ndh.ddsl.ast.behavior.clause.ThenClause;

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
 * Base implementation of AstVisitor with default behavior.
 * All visit methods return null by default.
 * Subclasses can override only the methods they need.
 *
 * @param <R> The return type of visit methods
 */
public abstract class BaseAstVisitor<R> implements AstVisitor<R> {
    
    /**
     * Default result when no specific handling is needed.
     * Override this to change the default behavior.
     */
    protected R defaultResult() {
        return null;
    }
    
    // ===== Model Declarations =====
    
    @Override
    public R visitDomainModel(DomainModel model) {
        return defaultResult();
    }
    
    @Override
    public R visitBoundedContext(BoundedContextDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitModule(ModuleDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitAggregate(AggregateDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitEntity(EntityDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitValueObject(ValueObjectDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitDomainService(DomainServiceDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitDomainEvent(DomainEventDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitRepository(RepositoryDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitFactory(FactoryDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitSpecification(SpecificationDecl decl) {
        return defaultResult();
    }
    
    // ===== Application Layer =====
    
    @Override
    public R visitApplicationService(ApplicationServiceDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitUseCase(UseCaseDecl decl) {
        return defaultResult();
    }
    
    // ===== Members =====
    
    @Override
    public R visitField(FieldDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitMethod(MethodDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitParameter(ParameterDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitConstructor(ConstructorDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitInvariant(InvariantDecl decl) {
        return defaultResult();
    }
    
    // ===== Behaviors =====
    
    @Override
    public R visitBehavior(BehaviorDecl decl) {
        return defaultResult();
    }
    
    @Override
    public R visitRequireClause(RequireClause clause) {
        return defaultResult();
    }
    
    @Override
    public R visitGivenClause(GivenClause clause) {
        return defaultResult();
    }
    
    @Override
    public R visitThenClause(ThenClause clause) {
        return defaultResult();
    }
    
    @Override
    public R visitEmitClause(EmitClause clause) {
        return defaultResult();
    }
    
    // ===== Statements =====
    
    @Override
    public R visitBlockStmt(BlockStmt stmt) {
        return defaultResult();
    }
    
    @Override
    public R visitExpressionStmt(ExpressionStmt stmt) {
        return defaultResult();
    }
    
    @Override
    public R visitIfStmt(IfStmt stmt) {
        return defaultResult();
    }
    
    @Override
    public R visitForEachStmt(ForEachStmt stmt) {
        return defaultResult();
    }
    
    @Override
    public R visitReturnStmt(ReturnStmt stmt) {
        return defaultResult();
    }
    
    @Override
    public R visitAssignmentStmt(AssignmentStmt stmt) {
        return defaultResult();
    }
    
    @Override
    public R visitVariableDeclarationStmt(VariableDeclarationStmt stmt) {
        return defaultResult();
    }
    
    // ===== Expressions =====
    
    @Override
    public R visitLiteralExpr(LiteralExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitVariableExpr(VariableExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitBinaryExpr(BinaryExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitUnaryExpr(UnaryExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitMethodCallExpr(MethodCallExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitFieldAccessExpr(FieldAccessExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitNewInstanceExpr(NewInstanceExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitListExpr(ListExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitMapExpr(MapExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitTernaryExpr(TernaryExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitThisExpr(ThisExpr expr) {
        return defaultResult();
    }
    
    @Override
    public R visitNullExpr(NullExpr expr) {
        return defaultResult();
    }
}
