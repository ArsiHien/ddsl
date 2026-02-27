package uet.ndh.ddsl.analysis.resolver;

import uet.ndh.ddsl.analysis.scope.SymbolTable;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.expr.*;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.ast.visitor.TreeWalkingVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Second pass: Resolves all type references to their declarations.
 * Checks that all referenced types exist and are accessible.
 */
public class TypeResolver extends TreeWalkingVisitor<Void> {
    
    private final SymbolTable symbolTable;
    private final List<TypeResolutionError> errors;
    
    public TypeResolver(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.errors = new ArrayList<>();
    }
    
    public List<TypeResolutionError> errors() {
        return List.copyOf(errors);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    private void resolveType(TypeRef typeRef, uet.ndh.ddsl.ast.AstNode context) {
        if (typeRef == null) return;
        
        String typeName = typeRef.name();
        
        // Skip 'Object' - it's a placeholder for untyped natural language params
        // These are validated separately during code generation
        if ("Object".equals(typeName)) return;
        
        if (!symbolTable.isTypeDefined(typeName)) {
            errors.add(new TypeResolutionError(
                context.span(),
                "Unknown type: '" + typeName + "'"
            ));
        }
        
        // Resolve generic type arguments
        for (TypeRef arg : typeRef.typeArguments()) {
            resolveType(arg, context);
        }
    }
    
    @Override
    public Void visitField(FieldDecl decl) {
        resolveType(decl.type(), decl);
        return super.visitField(decl);
    }
    
    @Override
    public Void visitMethod(MethodDecl decl) {
        resolveType(decl.returnType(), decl);
        return super.visitMethod(decl);
    }
    
    @Override
    public Void visitParameter(ParameterDecl decl) {
        resolveType(decl.type(), decl);
        return super.visitParameter(decl);
    }
    
    @Override
    public Void visitAggregate(AggregateDecl decl) {
        // Check that root type exists
        if (decl.root() != null) {
            // Root entity is defined within the aggregate itself
        }
        return super.visitAggregate(decl);
    }
    
    @Override
    public Void visitEntity(EntityDecl decl) {
        // Entity fields are visited by the parent
        return super.visitEntity(decl);
    }
    
    @Override
    public Void visitValueObject(ValueObjectDecl decl) {
        // Value object fields are visited by the parent
        return super.visitValueObject(decl);
    }
    
    @Override
    public Void visitDomainService(DomainServiceDecl decl) {
        // Resolve dependency types
        for (var dep : decl.dependencies()) {
            resolveType(dep.type(), dep);
        }
        return super.visitDomainService(decl);
    }
    
    @Override
    public Void visitNewInstanceExpr(NewInstanceExpr expr) {
        resolveType(expr.type(), expr);
        return super.visitNewInstanceExpr(expr);
    }
    
    @Override
    public Void visitVariableExpr(VariableExpr expr) {
        // Check that variable is defined
        if (symbolTable.resolve(expr.name()).isEmpty()) {
            errors.add(new TypeResolutionError(
                expr.span(),
                "Undefined variable: '" + expr.name() + "'"
            ));
        }
        return super.visitVariableExpr(expr);
    }
    
    /**
     * Error during type resolution.
     */
    public record TypeResolutionError(
        uet.ndh.ddsl.ast.SourceSpan location,
        String message
    ) {}
}
