---
id: ddsl-syntax-007
category: SYNTAX_RULE
subcategory: bounded_context
dsl_construct: BoundedContext
complexity: basic
version: 3.0
---

DDSL BoundedContext Top-Level Structure:

A BoundedContext is the top-level organizational unit that defines a consistent domain model with its own Ubiquitous Language. It provides a semantic boundary within which a domain model is valid.

Syntax:
```
BoundedContext <Name> {
    ubiquitous-language { ... }
    domain { ... }
    events { ... }
    factories { ... }
    repositories { ... }
    specifications { ... }
}
```

Sections:
- ubiquitous-language: Definitions of domain terms
- domain: Aggregates, Entities, ValueObjects, DomainServices
- events: DomainEvent definitions
- factories: Factory definitions for object creation
- repositories: Repository interfaces for persistence
- specifications: Query specification definitions

Example:
```ddsl
BoundedContext ECommerce {
    ubiquitous-language {
        Order: "A customer's request to purchase products"
    }
    
    domain {
        Aggregate Order { ... }
        Entity OrderItem { ... }
        ValueObject Money { ... }
        DomainService PricingService { ... }
    }
    
    events {
        DomainEvent OrderPlaced { ... }
    }
    
    repositories {
        Repository OrderRepository for Order { ... }
    }
    
    specifications {
        Specification ActiveOrders { ... }
    }
}
```

See Also:
- nl-to-ddsl/core-patterns.md for transformation overview
- nl-to-ddsl/complete-examples.md for full examples
