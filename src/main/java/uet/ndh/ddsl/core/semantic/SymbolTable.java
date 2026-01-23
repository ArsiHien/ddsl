package uet.ndh.ddsl.core.semantic;

import lombok.Data;
import uet.ndh.ddsl.core.SourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Symbol Table for name resolution in DDD DSL.
 * Implements classical compiler theory symbol table with scoped name resolution.
 *
 * Academic reference: Aho, Sethi, Ullman - Compilers: Principles, Techniques, and Tools
 * Section 2.7: Symbol Tables
 */
@Data
public class SymbolTable {

    private final Map<String, SymbolEntry> globalSymbols;
    private final Stack<Scope> scopeStack;
    private final Map<String, Set<String>> dependencies;

    public SymbolTable() {
        this.globalSymbols = new ConcurrentHashMap<>();
        this.scopeStack = new Stack<>();
        this.dependencies = new HashMap<>();

        // Create global scope
        pushScope("global");
    }

    /**
     * Enter a new scope (BoundedContext, Aggregate, etc.)
     */
    public void pushScope(String scopeName) {
        Scope scope = new Scope(scopeName, getCurrentScope());
        scopeStack.push(scope);
    }

    /**
     * Exit current scope
     */
    public void popScope() {
        if (scopeStack.size() > 1) { // Keep global scope
            scopeStack.pop();
        }
    }

    /**
     * Declare a symbol in current scope
     */
    public void declare(String name, SymbolType type, SourceLocation location, Object declaration) {
        String qualifiedName = getQualifiedName(name);

        if (isDeclaredInCurrentScope(name)) {
            throw new SemanticException(
                String.format("Symbol '%s' already declared in current scope", name),
                location
            );
        }

        SymbolEntry entry = new SymbolEntry(name, qualifiedName, type, location, declaration, getCurrentScope());
        Objects.requireNonNull(getCurrentScope()).declare(name, entry);
        globalSymbols.put(qualifiedName, entry);
    }

    /**
     * Resolve a symbol name to its declaration
     * Implements standard scope chain lookup algorithm
     */
    public Optional<SymbolEntry> resolve(String name) {
        // Search from current scope up to global scope
        Scope current = getCurrentScope();
        while (current != null) {
            SymbolEntry entry = current.lookup(name);
            if (entry != null) {
                return Optional.of(entry);
            }
            current = current.getParent();
        }

        // Try qualified name in global table
        SymbolEntry global = globalSymbols.get(name);
        return Optional.ofNullable(global);
    }

    /**
     * Add dependency relationship for graph-based analysis
     */
    public void addDependency(String from, String to) {
        dependencies.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    /**
     * Get all dependencies for graph analysis
     */
    public Map<String, Set<String>> getDependencyGraph() {
        return Collections.unmodifiableMap(dependencies);
    }

    /**
     * Get all symbols of a specific type
     */
    public List<SymbolEntry> getSymbolsOfType(SymbolType type) {
        return globalSymbols.values().stream()
            .filter(entry -> entry.type() == type)
            .toList();
    }

    private Scope getCurrentScope() {
        return scopeStack.isEmpty() ? null : scopeStack.peek();
    }

    private String getQualifiedName(String name) {
        if (scopeStack.size() <= 1) {
            return name;
        }

        StringBuilder qualified = new StringBuilder();
        for (int i = 1; i < scopeStack.size(); i++) { // Skip global scope
            qualified.append(scopeStack.get(i).getName()).append(".");
        }
        qualified.append(name);
        return qualified.toString();
    }

    private boolean isDeclaredInCurrentScope(String name) {
        Scope current = getCurrentScope();
        return current != null && current.lookup(name) != null;
    }

    /**
     * Nested scope for hierarchical name resolution
     */
    @Data
    public static class Scope {
        private final String name;
        private final Scope parent;
        private final Map<String, SymbolEntry> symbols;

        public Scope(String name, Scope parent) {
            this.name = name;
            this.parent = parent;
            this.symbols = new HashMap<>();
        }

        public void declare(String name, SymbolEntry entry) {
            symbols.put(name, entry);
        }

        public SymbolEntry lookup(String name) {
            return symbols.get(name);
        }

        public Collection<SymbolEntry> getAllSymbols() {
            return symbols.values();
        }
    }

    /**
         * Symbol entry with metadata
     * @param declaration  Reference to actual AST node
     */
        public record SymbolEntry(String name, String qualifiedName, SymbolType type, SourceLocation location,
                                  Object declaration, Scope scope) {
    }

    /**
     * Types of symbols in DDD DSL
     */
    public enum SymbolType {
        BOUNDED_CONTEXT,
        AGGREGATE,
        ENTITY,
        VALUE_OBJECT,
        DOMAIN_SERVICE,
        DOMAIN_EVENT,
        REPOSITORY,
        FACTORY,
        APPLICATION_SERVICE,
        SPECIFICATION
    }
}
