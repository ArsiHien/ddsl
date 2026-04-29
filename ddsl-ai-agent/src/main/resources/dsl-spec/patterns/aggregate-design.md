---
id: ddsl-pattern-001
category: DDD_PATTERN
subcategory: aggregate
complexity: intermediate
version: 3.0
---

DDD Pattern: Aggregate Design Rules

Aggregates are consistency boundaries that encapsulate entities and value objects. They ensure that all invariants are maintained within their boundary.

Core Rules:
1. Every Aggregate must have exactly one root Entity
2. The root Entity must have an @identity field
3. External references should only reference the Aggregate root, never internal entities
4. Aggregates define transactional boundaries — all changes within an Aggregate are atomic
5. Invariants within an Aggregate are always consistent after any operation
6. Aggregates communicate with each other via Domain Events

Size Guidelines:
- Keep Aggregates small — large Aggregates hurt performance
- An Aggregate should fit in memory as a single unit
- If you have more than 5-7 entities in an Aggregate, reconsider the design
- Reference other Aggregates by ID only, not by object reference

Example Well-Designed Aggregate:
```ddsl
Aggregate Order {
    orderId: UUID @identity
    customerId: UUID @required — reference to Customer Aggregate by ID
    items: List<OrderItem>
    totalAmount: Decimal
    status: String
    
    invariants {
        items cannot be empty
        totalAmount equals sum of items subtotals
    }
    
    operations {
        when placing order with customerId and items:
            require that customerId is not empty and items is not empty
            calculate totalAmount as sum of items subtotals
            set status to "PENDING"
            emit OrderPlaced with orderId
    }
}
```

See Also:
- nl-to-ddsl/core-patterns.md for transformation principles
- nl-to-ddsl/complete-examples.md for complete aggregate examples
- syntax/aggregate.md for syntax reference
