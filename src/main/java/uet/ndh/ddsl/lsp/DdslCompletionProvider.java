package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.*;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.*;

/**
 * Completion provider for DDSL Language Server.
 * 
 * Provides intelligent auto-completion based on:
 * - Current context (inside Aggregate, Entity, etc.)
 * - DDSL keywords and constructs
 * - Type references
 * - Field names
 * - Natural language patterns
 */
public class DdslCompletionProvider {
    
    // ========== Keyword Completion Items ==========
    
    /** Top-level constructs */
    private static final List<CompletionItem> TOP_LEVEL_COMPLETIONS = List.of(
        createKeywordCompletion("BoundedContext", "Define a bounded context", 
            "BoundedContext ${1:Name} {\n\t$0\n}"),
        createKeywordCompletion("Aggregate", "Define an aggregate root",
            "Aggregate ${1:Name} {\n\t$0\n}"),
        createKeywordCompletion("Entity", "Define an entity",
            "Entity ${1:Name} {\n\t$0\n}"),
        createKeywordCompletion("ValueObject", "Define a value object",
            "ValueObject ${1:Name} {\n\t$0\n}"),
        createKeywordCompletion("DomainService", "Define a domain service",
            "DomainService ${1:Name} {\n\t$0\n}"),
        createKeywordCompletion("DomainEvent", "Define a domain event",
            "DomainEvent ${1:Name} {\n\t$0\n}"),
        createKeywordCompletion("Factory", "Define a factory",
            "Factory ${1:Name} for ${2:Type} {\n\t$0\n}"),
        createKeywordCompletion("Repository", "Define a repository",
            "Repository ${1:Name} for ${2:AggregateType} {\n\t$0\n}"),
        createKeywordCompletion("Specification", "Define a specification",
            "Specification ${1:Name} for ${2:Type} {\n\t$0\n}")
    );
    
    /** Section keywords */
    private static final List<CompletionItem> SECTION_COMPLETIONS = List.of(
        createKeywordCompletion("domain", "Domain section containing aggregates and entities",
            "domain {\n\t$0\n}"),
        createKeywordCompletion("events", "Events section for domain events",
            "events {\n\t$0\n}"),
        createKeywordCompletion("factories", "Factories section",
            "factories {\n\t$0\n}"),
        createKeywordCompletion("repositories", "Repositories section",
            "repositories {\n\t$0\n}"),
        createKeywordCompletion("specifications", "Specifications section",
            "specifications {\n\t$0\n}"),
        createKeywordCompletion("use-cases", "Use cases section",
            "use-cases {\n\t$0\n}"),
        createKeywordCompletion("ubiquitous-language", "Ubiquitous language definitions",
            "ubiquitous-language {\n\t$0\n}")
    );
    
    /** Type-related completions */
    private static final List<CompletionItem> TYPE_COMPLETIONS = List.of(
        createTypeCompletion("String", "Text type"),
        createTypeCompletion("Int", "Integer type"),
        createTypeCompletion("Decimal", "Decimal number type"),
        createTypeCompletion("Boolean", "Boolean type"),
        createTypeCompletion("DateTime", "Date and time type"),
        createTypeCompletion("UUID", "Universally unique identifier type"),
        createTypeCompletion("List", "List collection type", "List<${1:Type}>"),
        createTypeCompletion("Set", "Set collection type", "Set<${1:Type}>"),
        createTypeCompletion("Map", "Map collection type", "Map<${1:KeyType}, ${2:ValueType}>"),
        createTypeCompletion("Void", "No return type")
    );
    
    /** Constraint annotations */
    private static final List<CompletionItem> CONSTRAINT_COMPLETIONS = List.of(
        createAnnotationCompletion("@identity", "Mark as identity field"),
        createAnnotationCompletion("@required", "Mark as required field"),
        createAnnotationCompletion("@unique", "Mark as unique field"),
        createAnnotationCompletion("@min", "Minimum value constraint", "@min(${1:value})"),
        createAnnotationCompletion("@max", "Maximum value constraint", "@max(${1:value})"),
        createAnnotationCompletion("@minLength", "Minimum length constraint", "@minLength(${1:value})"),
        createAnnotationCompletion("@maxLength", "Maximum length constraint", "@maxLength(${1:value})"),
        createAnnotationCompletion("@precision", "Decimal precision", "@precision(${1:value})"),
        createAnnotationCompletion("@default", "Default value", "@default(${1:value})"),
        createAnnotationCompletion("@computed", "Computed field"),
        createAnnotationCompletion("@pattern", "Regex pattern constraint", "@pattern(\"${1:regex}\")")
    );
    
    /** Behavioral keywords */
    private static final List<CompletionItem> BEHAVIOR_COMPLETIONS = List.of(
        createKeywordCompletion("when", "Define a behavior trigger",
            "when ${1:action} with ${2:params} {\n\t$0\n}"),
        createKeywordCompletion("require", "Add a precondition",
            "require that ${1:condition}"),
        createKeywordCompletion("given", "Add a precondition with context",
            "given ${1:context}"),
        createKeywordCompletion("then", "Add an action",
            "then ${1:action}"),
        createKeywordCompletion("emit", "Emit a domain event",
            "emit event ${1:EventName}"),
        createKeywordCompletion("return", "Return a value",
            "return ${1:value}")
    );
    
    /** Statement keywords */
    private static final List<CompletionItem> STATEMENT_COMPLETIONS = List.of(
        createKeywordCompletion("set", "Set a field value",
            "set ${1:field} to ${2:value}"),
        createKeywordCompletion("change", "Change a field value",
            "change ${1:field} to ${2:value}"),
        createKeywordCompletion("record", "Record a value",
            "record ${1:field} as ${2:value}"),
        createKeywordCompletion("calculate", "Calculate a value",
            "calculate ${1:field} as ${2:expression}"),
        createKeywordCompletion("create", "Create a new instance",
            "create ${1:Type} with ${2:params}"),
        createKeywordCompletion("add", "Add to collection",
            "add ${1:item} to ${2:collection}"),
        createKeywordCompletion("remove", "Remove from collection",
            "remove ${1:item} from ${2:collection}"),
        createKeywordCompletion("save", "Save to repository",
            "save ${1:entity} to ${2:repository}"),
        createKeywordCompletion("if", "Conditional statement",
            "if ${1:condition} {\n\t${2:action}\n}"),
        createKeywordCompletion("otherwise", "Else branch",
            "otherwise {\n\t$0\n}"),
        createKeywordCompletion("for", "Loop statement",
            "for each ${1:item} in ${2:collection} {\n\t$0\n}")
    );
    
    /** Block keywords inside domain constructs */
    private static final List<CompletionItem> BLOCK_COMPLETIONS = List.of(
        createKeywordCompletion("invariants", "Define invariants",
            "invariants {\n\t$0\n}"),
        createKeywordCompletion("operations", "Define operations",
            "operations {\n\t$0\n}")
    );
    
    // ========== Completion Logic ==========
    
    /**
     * Get completion items for the current position.
     */
    public List<CompletionItem> getCompletions(String content, List<Token> tokens,
                                                Position position, CompletionContext context) {
        List<CompletionItem> items = new ArrayList<>();
        
        // Determine context
        CompletionContextInfo ctxInfo = analyzeContext(content, tokens, position);
        
        // Handle trigger character
        String triggerChar = context != null ? context.getTriggerCharacter() : null;
        
        if ("@".equals(triggerChar)) {
            // Annotation context
            items.addAll(CONSTRAINT_COMPLETIONS);
        } else if (".".equals(triggerChar)) {
            // Member access - need semantic analysis
            items.addAll(getMemberCompletions(ctxInfo));
        } else if ("<".equals(triggerChar)) {
            // Generic type parameter
            items.addAll(TYPE_COMPLETIONS);
        } else {
            // General completions based on context
            switch (ctxInfo.scope) {
                case TOP_LEVEL -> {
                    items.addAll(TOP_LEVEL_COMPLETIONS);
                    items.addAll(SECTION_COMPLETIONS);
                }
                case BOUNDED_CONTEXT -> {
                    items.addAll(SECTION_COMPLETIONS);
                }
                case DOMAIN_SECTION -> {
                    items.addAll(TOP_LEVEL_COMPLETIONS.stream()
                        .filter(i -> !i.getLabel().equals("BoundedContext"))
                        .toList());
                }
                case AGGREGATE, ENTITY, VALUE_OBJECT -> {
                    items.addAll(TYPE_COMPLETIONS);
                    items.addAll(CONSTRAINT_COMPLETIONS);
                    items.addAll(BLOCK_COMPLETIONS);
                }
                case OPERATIONS -> {
                    items.addAll(BEHAVIOR_COMPLETIONS);
                }
                case BEHAVIOR_BLOCK -> {
                    items.addAll(STATEMENT_COMPLETIONS);
                    items.addAll(BEHAVIOR_COMPLETIONS);
                }
                case INVARIANTS -> {
                    // Natural language predicates
                    items.add(createKeywordCompletion("must", "Invariant assertion",
                        "${1:field} must ${2:condition}"));
                    items.add(createKeywordCompletion("should", "Soft invariant",
                        "${1:field} should ${2:condition}"));
                }
                default -> {
                    // Include common completions
                    items.addAll(TYPE_COMPLETIONS);
                    items.addAll(CONSTRAINT_COMPLETIONS);
                }
            }
            
            // Add identifier completions from the document
            items.addAll(getIdentifierCompletions(tokens, ctxInfo));
        }
        
        // Sort and return
        return items;
    }
    
    /**
     * Resolve additional details for a completion item.
     */
    public CompletionItem resolveCompletionItem(CompletionItem item) {
        // Add detailed documentation based on the item
        String label = item.getLabel();
        
        MarkupContent documentation = new MarkupContent();
        documentation.setKind(MarkupKind.MARKDOWN);
        
        documentation.setValue(switch (label) {
            case "Aggregate" -> """
                # Aggregate
                
                An aggregate is a cluster of domain objects that can be treated as a single unit.
                Each aggregate has a root entity through which all external references should pass.
                
                ```ddsl
                Aggregate Order {
                    @identity orderId: UUID
                    customer: Customer
                    items: List<OrderItem>
                    
                    invariants {
                        items must not be empty
                        total must be greater than 0
                    }
                    
                    operations {
                        when adding item with product, quantity {
                            then add OrderItem to items
                        }
                    }
                }
                ```
                """;
            case "Entity" -> """
                # Entity
                
                An entity is an object defined primarily by its identity rather than its attributes.
                
                ```ddsl
                Entity OrderItem {
                    @identity itemId: UUID
                    product: Product
                    quantity: Int @min(1)
                    unitPrice: Decimal
                }
                ```
                """;
            case "ValueObject" -> """
                # Value Object
                
                A value object is an immutable object that describes some characteristic or attribute
                but has no conceptual identity.
                
                ```ddsl
                ValueObject Money {
                    amount: Decimal @min(0)
                    currency: String @maxLength(3)
                    
                    invariants {
                        currency must be valid ISO code
                    }
                }
                ```
                """;
            default -> "No documentation available for " + label;
        });
        
        item.setDocumentation(documentation);
        return item;
    }
    
    // ========== Context Analysis ==========
    
    private CompletionContextInfo analyzeContext(String content, List<Token> tokens, Position position) {
        CompletionContextInfo info = new CompletionContextInfo();
        info.scope = CompletionScope.TOP_LEVEL;
        
        if (tokens == null || tokens.isEmpty()) {
            return info;
        }
        
        // Find the token at or before the position
        int line = position.getLine() + 1; // Convert to 1-based
        int column = position.getCharacter() + 1;
        
        // Track nesting to determine scope
        Deque<TokenType> scopeStack = new ArrayDeque<>();
        
        for (Token token : tokens) {
            // Stop if we've passed the cursor position
            if (token.getLine() > line || 
                (token.getLine() == line && token.getColumn() > column)) {
                break;
            }
            
            TokenType type = token.getType();
            
            // Track scope entry
            if (type == TokenType.BOUNDED_CONTEXT) {
                scopeStack.push(type);
                info.scope = CompletionScope.BOUNDED_CONTEXT;
            } else if (type == TokenType.DOMAIN) {
                info.scope = CompletionScope.DOMAIN_SECTION;
            } else if (type == TokenType.AGGREGATE) {
                scopeStack.push(type);
                info.scope = CompletionScope.AGGREGATE;
                info.currentTypeName = getNextIdentifier(tokens, token);
            } else if (type == TokenType.ENTITY) {
                scopeStack.push(type);
                info.scope = CompletionScope.ENTITY;
                info.currentTypeName = getNextIdentifier(tokens, token);
            } else if (type == TokenType.VALUE_OBJECT) {
                scopeStack.push(type);
                info.scope = CompletionScope.VALUE_OBJECT;
                info.currentTypeName = getNextIdentifier(tokens, token);
            } else if (type == TokenType.INVARIANTS) {
                info.scope = CompletionScope.INVARIANTS;
            } else if (type == TokenType.OPERATIONS) {
                info.scope = CompletionScope.OPERATIONS;
            } else if (type == TokenType.WHEN) {
                info.scope = CompletionScope.BEHAVIOR_BLOCK;
            } else if (type == TokenType.LEFT_BRACE) {
                // Opening brace - already tracked by keyword
            } else if (type == TokenType.RIGHT_BRACE) {
                // Closing brace - pop scope
                if (!scopeStack.isEmpty()) {
                    scopeStack.pop();
                    // Reset scope based on remaining stack
                    info.scope = scopeStack.isEmpty() ? 
                        CompletionScope.TOP_LEVEL : 
                        mapTokenToScope(scopeStack.peek());
                }
            }
        }
        
        return info;
    }
    
    private String getNextIdentifier(List<Token> tokens, Token current) {
        int idx = tokens.indexOf(current);
        if (idx >= 0 && idx + 1 < tokens.size()) {
            Token next = tokens.get(idx + 1);
            if (next.getType() == TokenType.IDENTIFIER) {
                return next.getLexeme();
            }
        }
        return null;
    }
    
    private CompletionScope mapTokenToScope(TokenType type) {
        return switch (type) {
            case BOUNDED_CONTEXT -> CompletionScope.BOUNDED_CONTEXT;
            case AGGREGATE -> CompletionScope.AGGREGATE;
            case ENTITY -> CompletionScope.ENTITY;
            case VALUE_OBJECT -> CompletionScope.VALUE_OBJECT;
            default -> CompletionScope.TOP_LEVEL;
        };
    }
    
    private List<CompletionItem> getMemberCompletions(CompletionContextInfo ctxInfo) {
        // Would need semantic analysis to provide accurate member completions
        // For now, return empty
        return Collections.emptyList();
    }
    
    private List<CompletionItem> getIdentifierCompletions(List<Token> tokens, 
                                                           CompletionContextInfo ctxInfo) {
        List<CompletionItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        if (tokens == null) return items;
        
        for (Token token : tokens) {
            if (token.getType() == TokenType.IDENTIFIER) {
                String name = token.getLexeme();
                if (!seen.contains(name)) {
                    seen.add(name);
                    CompletionItem item = new CompletionItem(name);
                    item.setKind(CompletionItemKind.Variable);
                    items.add(item);
                }
            }
        }
        
        return items;
    }
    
    // ========== Factory Methods ==========
    
    private static CompletionItem createKeywordCompletion(String label, String detail, String snippet) {
        CompletionItem item = new CompletionItem(label);
        item.setKind(CompletionItemKind.Keyword);
        item.setDetail(detail);
        item.setInsertTextFormat(InsertTextFormat.Snippet);
        item.setInsertText(snippet);
        return item;
    }
    
    private static CompletionItem createKeywordCompletion(String label, String detail) {
        return createKeywordCompletion(label, detail, label);
    }
    
    private static CompletionItem createTypeCompletion(String label, String detail) {
        CompletionItem item = new CompletionItem(label);
        item.setKind(CompletionItemKind.TypeParameter);
        item.setDetail(detail);
        return item;
    }
    
    private static CompletionItem createTypeCompletion(String label, String detail, String snippet) {
        CompletionItem item = createTypeCompletion(label, detail);
        item.setInsertTextFormat(InsertTextFormat.Snippet);
        item.setInsertText(snippet);
        return item;
    }
    
    private static CompletionItem createAnnotationCompletion(String label, String detail) {
        CompletionItem item = new CompletionItem(label);
        item.setKind(CompletionItemKind.Property);
        item.setDetail(detail);
        return item;
    }
    
    private static CompletionItem createAnnotationCompletion(String label, String detail, String snippet) {
        CompletionItem item = createAnnotationCompletion(label, detail);
        item.setInsertTextFormat(InsertTextFormat.Snippet);
        item.setInsertText(snippet);
        return item;
    }
    
    // ========== Inner Classes ==========
    
    private static class CompletionContextInfo {
        CompletionScope scope = CompletionScope.TOP_LEVEL;
        String currentTypeName;
    }
    
    private enum CompletionScope {
        TOP_LEVEL,
        BOUNDED_CONTEXT,
        DOMAIN_SECTION,
        AGGREGATE,
        ENTITY,
        VALUE_OBJECT,
        INVARIANTS,
        OPERATIONS,
        BEHAVIOR_BLOCK
    }
}
