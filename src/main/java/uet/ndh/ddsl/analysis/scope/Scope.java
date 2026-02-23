package uet.ndh.ddsl.analysis.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a scope in the symbol table hierarchy.
 * Scopes can be nested (e.g., method inside entity inside aggregate).
 */
public class Scope {
    
    private final String name;
    private final ScopeKind kind;
    private final Scope parent;
    private final Map<String, Symbol> symbols;
    
    public enum ScopeKind {
        GLOBAL,
        BOUNDED_CONTEXT,
        MODULE,
        AGGREGATE,
        ENTITY,
        VALUE_OBJECT,
        SERVICE,
        METHOD,
        BLOCK
    }
    
    public Scope(String name, ScopeKind kind, Scope parent) {
        this.name = name;
        this.kind = kind;
        this.parent = parent;
        this.symbols = new HashMap<>();
    }
    
    /**
     * Create the global scope (no parent).
     */
    public static Scope global() {
        return new Scope("global", ScopeKind.GLOBAL, null);
    }
    
    /**
     * Create a child scope with this scope as parent.
     */
    public Scope child(String name, ScopeKind kind) {
        return new Scope(name, kind, this);
    }
    
    /**
     * Define a symbol in this scope.
     * Returns false if symbol already exists in this scope.
     */
    public boolean define(Symbol symbol) {
        if (symbols.containsKey(symbol.name())) {
            return false;
        }
        symbols.put(symbol.name(), symbol);
        return true;
    }
    
    /**
     * Resolve a symbol by name, searching parent scopes if not found.
     */
    public Optional<Symbol> resolve(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return Optional.of(symbol);
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        return Optional.empty();
    }
    
    /**
     * Resolve a symbol only in this scope (not parent scopes).
     */
    public Optional<Symbol> resolveLocal(String name) {
        return Optional.ofNullable(symbols.get(name));
    }
    
    public String name() {
        return name;
    }
    
    public ScopeKind kind() {
        return kind;
    }
    
    public Scope parent() {
        return parent;
    }
    
    public Map<String, Symbol> symbols() {
        return Map.copyOf(symbols);
    }
    
    /**
     * Get the fully qualified name of this scope.
     */
    public String qualifiedName() {
        if (parent == null || parent.kind == ScopeKind.GLOBAL) {
            return name;
        }
        return parent.qualifiedName() + "." + name;
    }
}
