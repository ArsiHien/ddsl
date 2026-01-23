# Hierarchical Symbol Table in DDSL Compiler

## Overview

The Hierarchical Symbol Table is a fundamental component of the DDSL (Domain-Driven Design Language) compiler, implementing classical compiler theory principles for name resolution and scope management. This implementation follows the academic standards outlined in "Compilers: Principles, Techniques, and Tools" by Aho, Sethi, and Ullman (Section 2.7: Symbol Tables).

## Academic Foundation

### Compiler Theory Background
The symbol table is a crucial data structure in compiler design that:
- **Manages identifiers**: Stores information about variables, functions, types, and other named entities
- **Implements scoping**: Provides hierarchical name resolution following lexical scoping rules
- **Enables semantic analysis**: Supports type checking, declaration verification, and reference resolution

### Hierarchical Scope Implementation
The DDSL symbol table implements a **stack-based scope hierarchy** where:
1. Each scope represents a naming context (global, bounded context, aggregate, etc.)
2. Scopes form a parent-child relationship creating a scope chain
3. Name resolution follows the **scope chain traversal algorithm**

## Architecture and Design

### Core Components

#### 1. SymbolTable Class
The main orchestrator that manages the entire symbol resolution system:

```java
public class SymbolTable {
    private final Map<String, SymbolEntry> globalSymbols;     // Global symbol registry
    private final Stack<Scope> scopeStack;                   // Scope hierarchy
    private final Map<String, Set<String>> dependencies;     // Dependency graph
}
```

#### 2. Scope Class (Nested)
Represents individual naming contexts in the hierarchy:

```java
public static class Scope {
    private final String name;                    // Scope identifier
    private final Scope parent;                   // Parent scope reference
    private final Map<String, SymbolEntry> symbols; // Local symbol table
}
```

#### 3. SymbolEntry Class (Nested)
Metadata container for declared symbols:

```java
public static class SymbolEntry {
    private final String name;                    // Simple name
    private final String qualifiedName;          // Fully qualified name
    private final SymbolType type;               // DDD element type
    private final SourceLocation location;       // Source position
    private final Object declaration;            // AST node reference
    private final Scope scope;                   // Declaring scope
}
```

### Symbol Types in DDD Context

The symbol table recognizes DDD-specific constructs:

```java
public enum SymbolType {
    BOUNDED_CONTEXT,    // Strategic design boundaries
    AGGREGATE,          // Consistency boundaries
    ENTITY,            // Objects with identity
    VALUE_OBJECT,      // Immutable objects
    DOMAIN_SERVICE,    // Domain logic services
    DOMAIN_EVENT,      // Domain state changes
    REPOSITORY,        // Persistence abstraction
    FACTORY,           // Complex object creation
    APPLICATION_SERVICE, // Application layer services
    SPECIFICATION       // Business rule specifications
}
```

## Key Algorithms

### 1. Scope Management

#### Entering a New Scope
```java
public void pushScope(String scopeName) {
    Scope scope = new Scope(scopeName, getCurrentScope());
    scopeStack.push(scope);
}
```
- Creates new scope with parent reference
- Maintains scope chain integrity
- Enables nested context support

#### Exiting a Scope
```java
public void popScope() {
    if (scopeStack.size() > 1) { // Preserve global scope
        scopeStack.pop();
    }
}
```
- Removes current scope from stack
- Preserves global scope invariant
- Returns to parent context

### 2. Symbol Declaration

#### Declaration Algorithm
```java
public void declare(String name, SymbolType type, SourceLocation location, Object declaration) {
    String qualifiedName = getQualifiedName(name);
    
    if (isDeclaredInCurrentScope(name)) {
        throw new SemanticException(
            String.format("Symbol '%s' already declared in current scope", name),
            location
        );
    }
    
    SymbolEntry entry = new SymbolEntry(name, qualifiedName, type, location, declaration, getCurrentScope());
    getCurrentScope().declare(name, entry);
    globalSymbols.put(qualifiedName, entry);
}
```

**Key Features:**
- **Collision Detection**: Prevents duplicate declarations in same scope
- **Qualified Name Generation**: Creates hierarchical identifiers
- **Dual Storage**: Local scope + global registry for efficient lookup
- **Metadata Preservation**: Maintains source location and AST references

### 3. Name Resolution

#### Scope Chain Traversal Algorithm
```java
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
```

**Resolution Strategy:**
1. **Local-to-Global Search**: Start from current scope, traverse upward
2. **First-Match Wins**: Return first symbol found (lexical scoping)
3. **Qualified Name Fallback**: Support fully qualified references
4. **Optional Return**: Handle undeclared symbols gracefully

### 4. Qualified Name Generation

#### Hierarchical Naming
```java
private String getQualifiedName(String name) {
    if (scopeStack.size() <= 1) {
        return name;  // Global scope
    }
    
    StringBuilder qualified = new StringBuilder();
    for (int i = 1; i < scopeStack.size(); i++) { // Skip global scope
        qualified.append(scopeStack.get(i).getName()).append(".");
    }
    qualified.append(name);
    return qualified.toString();
}
```

**Examples:**
- Global scope: `User`
- Bounded context: `Blog.User`
- Aggregate level: `Blog.Content.Post`
- Nested entity: `Blog.Content.Post.Comment`

## DDD-Specific Features

### 1. Domain Model Hierarchy Support
The symbol table naturally maps to DDD's hierarchical structure:

```
BoundedContext (e.g., "Blog")
├── Aggregate (e.g., "Content")
│   ├── Entity (e.g., "Post")
│   ├── ValueObject (e.g., "Title")
│   └── DomainEvent (e.g., "PostPublished")
└── DomainService (e.g., "ContentValidator")
```

### 2. Dependency Graph Construction
```java
public void addDependency(String from, String to) {
    dependencies.computeIfAbsent(from, k -> new HashSet<>()).add(to);
}
```
- Tracks inter-component dependencies
- Enables circular dependency detection
- Supports architectural analysis

### 3. Type-Based Symbol Queries
```java
public List<SymbolEntry> getSymbolsOfType(SymbolType type) {
    return globalSymbols.values().stream()
        .filter(entry -> entry.getType() == type)
        .toList();
}
```
- Retrieve all entities, value objects, etc.
- Support architectural validation rules
- Enable code generation patterns

## Implementation Benefits

### 1. Academic Correctness
- Follows classical compiler theory principles
- Implements proven scope resolution algorithms
- Maintains standard symbol table invariants

### 2. DDD Domain Alignment
- Natural mapping to DDD hierarchical concepts
- Support for strategic and tactical design patterns
- Domain-specific symbol types and relationships

### 3. Performance Characteristics
- **O(d)** name resolution (d = scope depth)
- **O(1)** qualified name lookup via global registry
- **Concurrent-safe** global symbol storage

### 4. Error Handling and Diagnostics
- Precise source location tracking
- Semantic error reporting with context
- Symbol collision detection and reporting

## Integration with Semantic Analysis

### Two-Pass Analysis Support
The symbol table integrates with the `TwoPassSemanticAnalyzer`:

1. **Pass 1 (Declaration Collection)**:
   - Build complete symbol table
   - Establish scope hierarchy
   - Detect naming conflicts

2. **Pass 2 (Reference Resolution)**:
   - Resolve all symbol references
   - Validate cross-references
   - Build dependency graph

### Semantic Validation Features
- **Reference Resolution**: Ensure all used symbols are declared
- **Scope Validation**: Verify access to symbols based on scope rules
- **Type Consistency**: Support type-based semantic analysis
- **Architectural Constraints**: Validate DDD design principles

## Usage Examples

### Basic Symbol Declaration and Resolution
```java
SymbolTable symbolTable = new SymbolTable();

// Enter bounded context scope
symbolTable.pushScope("Blog");

// Declare aggregate
symbolTable.declare("Content", SymbolType.AGGREGATE, location, contentNode);

// Enter aggregate scope
symbolTable.pushScope("Content");

// Declare entity
symbolTable.declare("Post", SymbolType.ENTITY, location, postNode);

// Resolve symbol (finds "Post" in current scope)
Optional<SymbolEntry> post = symbolTable.resolve("Post");

// Resolve qualified name (finds from global registry)
Optional<SymbolEntry> qualifiedPost = symbolTable.resolve("Blog.Content.Post");
```

### Dependency Tracking
```java
// Track dependencies between components
symbolTable.addDependency("Blog.Content.Post", "Blog.User.User");
symbolTable.addDependency("Blog.Content.Comment", "Blog.Content.Post");

// Get dependency graph for analysis
Map<String, Set<String>> deps = symbolTable.getDependencyGraph();
```

### Type-Based Queries
```java
// Get all entities in the model
List<SymbolEntry> entities = symbolTable.getSymbolsOfType(SymbolType.ENTITY);

// Get all aggregates for architectural validation
List<SymbolEntry> aggregates = symbolTable.getSymbolsOfType(SymbolType.AGGREGATE);
```

## Conclusion

The Hierarchical Symbol Table in DDSL represents a sophisticated implementation of classical compiler theory applied to domain-driven design contexts. By combining proven academic algorithms with domain-specific requirements, it provides:

- **Robust name resolution** following lexical scoping principles
- **DDD-aware symbol management** supporting strategic and tactical design
- **Efficient lookup algorithms** with optimal performance characteristics
- **Comprehensive metadata tracking** for advanced semantic analysis

This implementation serves as the foundation for semantic analysis, code generation, and architectural validation in the DDSL compiler, demonstrating how classical compiler techniques can be effectively adapted for domain-specific language design.
