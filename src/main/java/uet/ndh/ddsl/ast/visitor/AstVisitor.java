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
import uet.ndh.ddsl.ast.common.TypeRef;
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
 * Visitor interface for traversing AST nodes.
 * Following the Crafting Interpreters pattern.
 *
 * @param <R> The return type of visit methods
 */
public interface AstVisitor<R> {
    
    // ===== Model Declarations =====
    
    R visitDomainModel(DomainModel model);
    
    R visitBoundedContext(BoundedContextDecl decl);
    
    R visitModule(ModuleDecl decl);
    
    R visitAggregate(AggregateDecl decl);
    
    R visitEntity(EntityDecl decl);
    
    R visitValueObject(ValueObjectDecl decl);
    
    R visitDomainService(DomainServiceDecl decl);
    
    R visitDomainEvent(DomainEventDecl decl);
    
    R visitRepository(RepositoryDecl decl);
    
    R visitFactory(FactoryDecl decl);
    
    R visitSpecification(SpecificationDecl decl);
    
    // ===== Application Layer =====
    
    R visitApplicationService(ApplicationServiceDecl decl);
    
    R visitUseCase(UseCaseDecl decl);
    
    // ===== Members =====
    
    R visitField(FieldDecl decl);
    
    R visitMethod(MethodDecl decl);
    
    R visitParameter(ParameterDecl decl);
    
    R visitConstructor(ConstructorDecl decl);
    
    R visitInvariant(InvariantDecl decl);
    
    // ===== Behaviors =====
    
    R visitBehavior(BehaviorDecl decl);
    
    R visitRequireClause(RequireClause clause);
    
    R visitGivenClause(GivenClause clause);
    
    R visitThenClause(ThenClause clause);
    
    R visitEmitClause(EmitClause clause);
    
    // ===== Statements =====
    
    R visitBlockStmt(BlockStmt stmt);
    
    R visitExpressionStmt(ExpressionStmt stmt);
    
    R visitIfStmt(IfStmt stmt);
    
    R visitForEachStmt(ForEachStmt stmt);
    
    R visitReturnStmt(ReturnStmt stmt);
    
    R visitAssignmentStmt(AssignmentStmt stmt);
    
    R visitVariableDeclarationStmt(VariableDeclarationStmt stmt);
    
    // ===== Expressions =====
    
    R visitLiteralExpr(LiteralExpr expr);
    
    R visitVariableExpr(VariableExpr expr);
    
    R visitBinaryExpr(BinaryExpr expr);
    
    R visitUnaryExpr(UnaryExpr expr);
    
    R visitMethodCallExpr(MethodCallExpr expr);
    
    R visitFieldAccessExpr(FieldAccessExpr expr);
    
    R visitNewInstanceExpr(NewInstanceExpr expr);
    
    R visitListExpr(ListExpr expr);
    
    R visitMapExpr(MapExpr expr);
    
    R visitTernaryExpr(TernaryExpr expr);
    
    R visitThisExpr(ThisExpr expr);
    
    R visitNullExpr(NullExpr expr);

    R visitTypeRef(TypeRef typeRef);
}
