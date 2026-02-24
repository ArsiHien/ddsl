---
id: ddsl-pattern-002
category: DDD_PATTERN
subcategory: entity_vs_vo
complexity: basic
---
DDD Pattern: Entity vs Value Object Decision
Choose Entity when:
- The object has a unique identity that persists through changes
- You need to track its lifecycle
- Two objects with same attributes are still distinct
Choose Value Object when:
- The object is defined entirely by its attributes
- It is immutable (replacing rather than mutating)
- Two objects with same attributes are interchangeable
Examples: Address, Money, DateRange are Value Objects.
Customer, Order, Product are Entities.
