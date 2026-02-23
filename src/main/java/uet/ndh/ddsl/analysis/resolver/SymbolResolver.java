package uet.ndh.ddsl.analysis.resolver;

import uet.ndh.ddsl.analysis.scope.Scope;
import uet.ndh.ddsl.analysis.scope.Symbol;
import uet.ndh.ddsl.analysis.scope.SymbolTable;
import uet.ndh.ddsl.ast.model.BoundedContextDecl;
import uet.ndh.ddsl.ast.model.ModuleDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.factory.FactoryDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.model.specification.SpecificationDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.visitor.TreeWalkingVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * First pass: Collects all declarations and builds the symbol table.
 * This pass doesn't validate types yet - just registers symbols.
 */
public class SymbolResolver extends TreeWalkingVisitor<Void> {
    
    private final SymbolTable symbolTable;
    private final List<ResolutionError> errors;
    
    public SymbolResolver(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.errors = new ArrayList<>();
    }
    
    public List<ResolutionError> errors() {
        return List.copyOf(errors);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    private void defineSymbol(String name, Symbol.SymbolKind kind, 
                               uet.ndh.ddsl.ast.AstNode node, Symbol.TypeInfo type) {
        Symbol symbol = new Symbol(name, kind, node, symbolTable.currentScope(), type);
        if (!symbolTable.define(symbol)) {
            errors.add(new ResolutionError(
                node.span(),
                "Symbol '" + name + "' is already defined in this scope"
            ));
        }
        
        // Register types globally
        if (isTypeSymbol(kind)) {
            symbolTable.registerType(symbol);
        }
    }
    
    private boolean isTypeSymbol(Symbol.SymbolKind kind) {
        return kind == Symbol.SymbolKind.ENTITY ||
               kind == Symbol.SymbolKind.VALUE_OBJECT ||
               kind == Symbol.SymbolKind.DOMAIN_EVENT ||
               kind == Symbol.SymbolKind.AGGREGATE;
    }
    
    @Override
    public Void visitBoundedContext(BoundedContextDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.BOUNDED_CONTEXT, decl, 
                     Symbol.TypeInfo.simple(decl.name()));
        
        symbolTable.enterScope(decl.name(), Scope.ScopeKind.BOUNDED_CONTEXT);
        super.visitBoundedContext(decl);
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitModule(ModuleDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.MODULE, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        
        symbolTable.enterScope(decl.name(), Scope.ScopeKind.MODULE);
        super.visitModule(decl);
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitAggregate(AggregateDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.AGGREGATE, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        
        symbolTable.enterScope(decl.name(), Scope.ScopeKind.AGGREGATE);
        super.visitAggregate(decl);
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitEntity(EntityDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.ENTITY, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        
        symbolTable.enterScope(decl.name(), Scope.ScopeKind.ENTITY);
        super.visitEntity(decl);
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitValueObject(ValueObjectDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.VALUE_OBJECT, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        
        symbolTable.enterScope(decl.name(), Scope.ScopeKind.VALUE_OBJECT);
        super.visitValueObject(decl);
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitDomainService(DomainServiceDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.DOMAIN_SERVICE, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        
        symbolTable.enterScope(decl.name(), Scope.ScopeKind.SERVICE);
        super.visitDomainService(decl);
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitDomainEvent(DomainEventDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.DOMAIN_EVENT, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        return null;
    }
    
    @Override
    public Void visitRepository(RepositoryDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.REPOSITORY, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        return null;
    }
    
    @Override
    public Void visitFactory(FactoryDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.FACTORY, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        return null;
    }
    
    @Override
    public Void visitSpecification(SpecificationDecl decl) {
        defineSymbol(decl.name(), Symbol.SymbolKind.SPECIFICATION, decl,
                     Symbol.TypeInfo.simple(decl.name()));
        return null;
    }
    
    @Override
    public Void visitField(FieldDecl decl) {
        String typeName = decl.type().name();
        Symbol.TypeInfo typeInfo = decl.type().isCollection()
            ? Symbol.TypeInfo.collection(decl.type().collectionKind().name(), typeName)
            : Symbol.TypeInfo.simple(typeName);
            
        defineSymbol(decl.name(), Symbol.SymbolKind.FIELD, decl, typeInfo);
        return null;
    }
    
    @Override
    public Void visitMethod(MethodDecl decl) {
        String returnTypeName = decl.returnType() != null ? decl.returnType().name() : "Void";
        defineSymbol(decl.name(), Symbol.SymbolKind.METHOD, decl, 
                     Symbol.TypeInfo.simple(returnTypeName));
        
        symbolTable.enterScope(decl.name(), Scope.ScopeKind.METHOD);
        super.visitMethod(decl);
        symbolTable.exitScope();
        
        return null;
    }
    
    @Override
    public Void visitParameter(ParameterDecl decl) {
        String typeName = decl.type().name();
        defineSymbol(decl.name(), Symbol.SymbolKind.PARAMETER, decl,
                     Symbol.TypeInfo.simple(typeName));
        return null;
    }
    
    /**
     * Error during symbol resolution.
     */
    public record ResolutionError(
        uet.ndh.ddsl.ast.SourceSpan location,
        String message
    ) {}
}
