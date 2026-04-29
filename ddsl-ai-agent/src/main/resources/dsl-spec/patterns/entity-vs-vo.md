---
id: ddsl-pattern-002
category: DDD_PATTERN
subcategory: entity_vs_vo
complexity: basic
version: 3.0
---

DDD Pattern: Entity vs Value Object Decision

Choosing between Entity and Value Object is one of the most important design decisions in DDD. It affects identity, equality, lifecycle, and persistence.

Choose Entity when:
- The object has a unique identity that persists through changes
- You need to track its lifecycle (created, modified, deleted)
- Two objects with the same attributes are still distinct
- The object changes state over time while maintaining identity
- Examples: Customer, Order, Product, Account

Choose Value Object when:
- The object is defined entirely by its attributes
- It is immutable (replacing rather than mutating)
- Two objects with the same attributes are interchangeable
- No need to track identity or lifecycle
- Examples: Money, Address, DateRange, Email, PhoneNumber

Comparison:

| Aspect | Entity | Value Object |
|--------|--------|--------------|
| Identity | Has @identity field | No identity |
| Equality | Based on identity | Based on all attributes |
| Mutability | Mutable (can change state) | Immutable (replace, don't change) |
| Lifecycle | Tracked (created, modified) | None (just values) |
| Repository | Yes (has repository) | No (embedded in entities) |

Examples:

Entity:
```ddsl
Entity Customer {
    customerId: UUID @identity
    name: String
    email: String
    — Changes to name/email don't change identity
}
```

Value Object:
```ddsl
ValueObject Money {
    amount: Decimal
    currency: String
    — $100 USD is always equal to $100 USD, regardless of instance
}
```

See Also:
- syntax/entity.md for Entity syntax
- syntax/value-object.md for Value Object syntax
- nl-to-ddsl/complete-examples.md for real-world examples
