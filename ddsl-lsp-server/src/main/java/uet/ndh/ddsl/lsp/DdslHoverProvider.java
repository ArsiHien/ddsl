package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.List;
import java.util.Map;

/**
 * Hover provider for DDSL Language Server.
 * 
 * Provides hover information (documentation) when the user hovers over:
 * - Keywords (with explanations)
 * - Types (with type information)
 * - Identifiers (with definitions if available)
 * - Annotations (with constraint documentation)
 */
public class DdslHoverProvider {
    
    /** Documentation for DDSL keywords */
    private static final Map<TokenType, String> KEYWORD_DOCS = Map.ofEntries(
        // Domain constructs
        Map.entry(TokenType.BOUNDED_CONTEXT, """
            ## Bounded Context
            
            A **Bounded Context** is a central pattern in Domain-Driven Design that defines
            the boundary within which a particular domain model is defined and applicable.
            
            Each bounded context has its own ubiquitous language and domain model.
            
            ```ddsl
            BoundedContext ECommerce {
                ubiquitous-language { ... }
                domain { ... }
                events { ... }
            }
            ```
            """),
        
        Map.entry(TokenType.AGGREGATE, """
            ## Aggregate
            
            An **Aggregate** is a cluster of domain objects that can be treated as a single unit.
            It has a root entity (the aggregate root) and a boundary that defines what is inside.
            
            - All external references go through the aggregate root
            - Invariants are enforced within the aggregate boundary
            - Aggregates are the unit of consistency
            
            ```ddsl
            Aggregate Order {
                @identity orderId: UUID
                customer: Customer
                items: List<OrderItem>
                
                invariants {
                    items must not be empty
                }
            }
            ```
            """),
        
        Map.entry(TokenType.ENTITY, """
            ## Entity
            
            An **Entity** is an object that is defined by its identity rather than its attributes.
            Two entities with the same attributes but different identities are different objects.
            
            ```ddsl
            Entity Customer {
                @identity customerId: UUID
                name: String
                email: Email
            }
            ```
            """),
        
        Map.entry(TokenType.VALUE_OBJECT, """
            ## Value Object
            
            A **Value Object** is an immutable object that describes some characteristic or
            attribute but has no conceptual identity. Two value objects with the same
            attributes are considered equal.
            
            ```ddsl
            ValueObject Money {
                amount: Decimal @min(0)
                currency: CurrencyCode
            }
            ```
            """),
        
        Map.entry(TokenType.DOMAIN_SERVICE, """
            ## Domain Service
            
            A **Domain Service** encapsulates domain logic that doesn't naturally fit within
            an entity or value object. It operates on domain objects but is stateless.
            
            ```ddsl
            DomainService PricingService {
                operations {
                    when calculating price for order {
                        ...
                    }
                }
            }
            ```
            """),
        
        Map.entry(TokenType.DOMAIN_EVENT, """
            ## Domain Event
            
            A **Domain Event** represents something that happened in the domain that domain
            experts care about. Events are immutable and named in past tense.
            
            ```ddsl
            DomainEvent OrderPlaced {
                orderId: UUID
                customerId: UUID
                orderDate: DateTime
            }
            ```
            """),
        
        Map.entry(TokenType.FACTORY, """
            ## Factory
            
            A **Factory** encapsulates the logic for creating complex objects or aggregates.
            It ensures that objects are created in a valid state.
            
            ```ddsl
            Factory OrderFactory for Order {
                creating Order from customer, items {
                    create Order with new UUID, customer, items
                    return the created order
                }
            }
            ```
            """),
        
        Map.entry(TokenType.REPOSITORY, """
            ## Repository
            
            A **Repository** provides a collection-like interface for accessing domain objects.
            It mediates between the domain and data mapping layers.
            
            ```ddsl
            Repository OrderRepository for Order {
                findById(id: UUID): Order?
                findByCustomer(customerId: UUID): List<Order>
                save(order: Order): Void
            }
            ```
            """),
        
        Map.entry(TokenType.SPECIFICATION, """
            ## Specification
            
            A **Specification** encapsulates a business rule that can be combined with other
            specifications using logical operators (and, or, not).
            
            ```ddsl
            Specification ActiveCustomer for Customer {
                matches when customer is active and has orders
            }
            ```
            """),
        
        // Behavioral keywords
        Map.entry(TokenType.WHEN, """
            ## when
            
            The `when` keyword starts a behavior definition, specifying the trigger
            that activates the behavior.
            
            ```ddsl
            when adding item with product, quantity {
                require that quantity is greater than 0
                then add item to items
            }
            ```
            """),
        
        Map.entry(TokenType.REQUIRE, """
            ## require
            
            The `require` keyword specifies a precondition that must be satisfied
            before the behavior can execute. If not satisfied, an error is raised.
            
            ```ddsl
            require that quantity is greater than 0
            require that customer is active
            ```
            """),
        
        Map.entry(TokenType.GIVEN, """
            ## given
            
            The `given` keyword specifies context or setup for a behavior,
            similar to preconditions but more for state setup.
            
            ```ddsl
            given existing order in the system
            given customer has valid payment method
            ```
            """),
        
        Map.entry(TokenType.THEN, """
            ## then
            
            The `then` keyword specifies the action to take when the behavior
            is triggered and all preconditions are met.
            
            ```ddsl
            then set status to "CONFIRMED"
            then add item to items
            then emit OrderPlaced event
            ```
            """),
        
        Map.entry(TokenType.EMIT, """
            ## emit
            
            The `emit` keyword publishes a domain event. Events are used to
            communicate that something significant happened in the domain.
            
            ```ddsl
            emit event OrderPlaced with orderId, customerId
            ```
            """),
        
        Map.entry(TokenType.RETURN, """
            ## return
            
            The `return` keyword specifies the value to return from an operation.
            
            ```ddsl
            return the calculated total
            return success with order
            return failure with "Invalid quantity"
            ```
            """),
        
        // Annotations
        Map.entry(TokenType.IDENTITY, """
            ## @identity
            
            Marks a field as the identity field of an entity. Each entity must have
            exactly one identity field that uniquely identifies it.
            
            ```ddsl
            @identity orderId: UUID
            ```
            """),
        
        Map.entry(TokenType.REQUIRED, """
            ## @required
            
            Marks a field as required (non-null). The field must have a value.
            
            ```ddsl
            name: String @required
            ```
            """),
        
        Map.entry(TokenType.UNIQUE, """
            ## @unique
            
            Marks a field as unique within the context. No two instances can have
            the same value for this field.
            
            ```ddsl
            email: Email @unique
            ```
            """),
        
        Map.entry(TokenType.MIN, """
            ## @min
            
            Specifies the minimum value for a numeric field.
            
            ```ddsl
            quantity: Int @min(1)
            price: Decimal @min(0)
            ```
            """),
        
        Map.entry(TokenType.MAX, """
            ## @max
            
            Specifies the maximum value for a numeric field.
            
            ```ddsl
            quantity: Int @max(100)
            discount: Decimal @max(0.5)
            ```
            """),
        
        Map.entry(TokenType.MIN_LENGTH, """
            ## @minLength
            
            Specifies the minimum length for a string field.
            
            ```ddsl
            password: String @minLength(8)
            ```
            """),
        
        Map.entry(TokenType.MAX_LENGTH, """
            ## @maxLength
            
            Specifies the maximum length for a string field.
            
            ```ddsl
            name: String @maxLength(100)
            ```
            """)
    );
    
    /** Documentation for types */
    private static final Map<TokenType, String> TYPE_DOCS = Map.of(
        TokenType.STRING_TYPE, "**String** - A sequence of characters (text)",
        TokenType.INT_TYPE, "**Int** - A whole number (32-bit integer)",
        TokenType.DECIMAL_TYPE, "**Decimal** - A decimal number with arbitrary precision",
        TokenType.BOOLEAN_TYPE, "**Boolean** - A true/false value",
        TokenType.DATETIME_TYPE, "**DateTime** - A date and time value",
        TokenType.UUID_TYPE, "**UUID** - A universally unique identifier",
        TokenType.LIST_TYPE, "**List<T>** - An ordered collection of elements",
        TokenType.SET_TYPE, "**Set<T>** - An unordered collection of unique elements",
        TokenType.MAP_TYPE, "**Map<K,V>** - A collection of key-value pairs",
        TokenType.VOID_TYPE, "**Void** - No return value"
    );
    
    /**
     * Get hover information for the token at the given position.
     */
    public Hover getHover(String content, List<Token> tokens, Position position) {
        // Find the token at the position
        Token token = findTokenAtPosition(tokens, position);
        
        if (token == null) {
            return null;
        }
        
        // Get documentation for the token
        String documentation = getDocumentation(token);
        
        if (documentation == null || documentation.isEmpty()) {
            return null;
        }
        
        // Create hover result
        MarkupContent markupContent = new MarkupContent();
        markupContent.setKind(MarkupKind.MARKDOWN);
        markupContent.setValue(documentation);
        
        // Create range for the token
        Range range = new Range(
            new Position(token.getLine() - 1, token.getColumn() - 1),
            new Position(token.getLine() - 1, token.getColumn() - 1 + token.getLexeme().length())
        );
        
        return new Hover(markupContent, range);
    }
    
    /**
     * Find the token at the given position.
     */
    private Token findTokenAtPosition(List<Token> tokens, Position position) {
        int line = position.getLine() + 1; // Convert to 1-based
        int column = position.getCharacter() + 1;
        
        for (Token token : tokens) {
            if (token.getLine() == line) {
                int tokenStart = token.getColumn();
                int tokenEnd = tokenStart + token.getLexeme().length();
                
                if (column >= tokenStart && column <= tokenEnd) {
                    return token;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get documentation for a token.
     */
    private String getDocumentation(Token token) {
        TokenType type = token.getType();
        
        // Check keyword documentation
        if (KEYWORD_DOCS.containsKey(type)) {
            return KEYWORD_DOCS.get(type);
        }
        
        // Check type documentation
        if (TYPE_DOCS.containsKey(type)) {
            return TYPE_DOCS.get(type);
        }
        
        // For identifiers, we would need semantic analysis to provide
        // documentation about the specific symbol
        if (type == TokenType.IDENTIFIER) {
            return "**" + token.getLexeme() + "** - User-defined identifier";
        }
        
        // For literals, show the value
        if (type == TokenType.STRING_LITERAL) {
            return "String literal: `" + token.getLexeme() + "`";
        }
        if (type == TokenType.INTEGER_LITERAL) {
            return "Integer literal: `" + token.getLexeme() + "`";
        }
        if (type == TokenType.DECIMAL_LITERAL) {
            return "Decimal literal: `" + token.getLexeme() + "`";
        }
        
        return null;
    }
}
