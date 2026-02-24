---
id: ddsl-syntax-007
category: SYNTAX_RULE
subcategory: bounded_context
dsl_construct: BoundedContext
complexity: basic
---
DDSL BoundedContext Top-Level Structure:
A BoundedContext is the top-level organizational unit.
Syntax: BoundedContext <Name> { <sections> }
Sections: domain { ... }, events { ... }, factories { ... },
          repositories { ... }, specifications { ... },
          ubiquitous-language { ... }
Example:
BoundedContext ECommerce {
    domain {
        Aggregate Order { ... }
        Entity OrderItem { ... }
        ValueObject Money { ... }
    }
    events {
        DomainEvent OrderPlaced { ... }
    }
    repositories {
        Repository OrderRepository for Order { ... }
    }
}
