package uet.ndh.ddsl.analysis.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Global symbol table that tracks all symbols across the compilation.
 * Provides utilities for type resolution and symbol lookup.
 */
public class SymbolTable {
    
    private final Scope globalScope;
    private Scope currentScope;
    private final Map<String, Symbol> typeRegistry;
    
    public SymbolTable() {
        this.globalScope = Scope.global();
        this.currentScope = globalScope;
        this.typeRegistry = new HashMap<>();
        initializeBuiltinTypes();
    }
    
    private void initializeBuiltinTypes() {
        // Register primitive types
        registerType("String", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("Int", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("Long", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("Double", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("Boolean", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("Date", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("DateTime", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("UUID", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("Money", Symbol.SymbolKind.VALUE_OBJECT);
        registerType("Void", Symbol.SymbolKind.VALUE_OBJECT);
    }
    
    private void registerType(String name, Symbol.SymbolKind kind) {
        Symbol symbol = new Symbol(name, kind, null, globalScope, Symbol.TypeInfo.simple(name));
        typeRegistry.put(name, symbol);
    }
    
    /**
     * Enter a new scope.
     */
    public void enterScope(String name, Scope.ScopeKind kind) {
        currentScope = currentScope.child(name, kind);
    }
    
    /**
     * Exit the current scope, returning to parent.
     */
    public void exitScope() {
        if (currentScope.parent() != null) {
            currentScope = currentScope.parent();
        }
    }
    
    /**
     * Define a symbol in the current scope.
     */
    public boolean define(Symbol symbol) {
        return currentScope.define(symbol);
    }
    
    /**
     * Register a type (Entity, ValueObject, etc.) for global resolution.
     */
    public void registerType(Symbol symbol) {
        typeRegistry.put(symbol.name(), symbol);
    }
    
    /**
     * Resolve a symbol by name in the current scope chain.
     */
    public Optional<Symbol> resolve(String name) {
        return currentScope.resolve(name);
    }
    
    /**
     * Resolve a type by name.
     */
    public Optional<Symbol> resolveType(String typeName) {
        return Optional.ofNullable(typeRegistry.get(typeName));
    }
    
    /**
     * Check if a type is defined.
     */
    public boolean isTypeDefined(String typeName) {
        return typeRegistry.containsKey(typeName);
    }
    
    public Scope globalScope() {
        return globalScope;
    }
    
    public Scope currentScope() {
        return currentScope;
    }
}
