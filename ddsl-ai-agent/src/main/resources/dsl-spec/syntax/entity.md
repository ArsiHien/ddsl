---
id: ddsl-syntax-002
category: SYNTAX_RULE
subcategory: entity
dsl_construct: Entity
complexity: basic
version: 3.0
---

DDSL Entity Declaration Syntax:

An Entity has identity and lifecycle within an Aggregate. Unlike Value Objects, Entities are defined by their identity, not just their attributes.

Syntax:
```
Entity <Name> {
    <field declarations>
    <invariants>?
    <operations>?
}
```

Key Points:
- Must have exactly one field annotated with @identity
- Can have operations (behaviors) that modify state
- Belongs to an Aggregate (either as root or nested)
- Has a lifecycle: created, modified, persisted, archived

Example:
```ddsl
Entity OrderItem {
    itemId: UUID @identity
    productName: String @required
    quantity: Int @min(1)
    unitPrice: Decimal @min(0)
}
```

Example with Operations (Natural Format):
```ddsl
Entity OrderItem {
    itemId: UUID @identity
    productName: String @required
    quantity: Int @min(1)
    unitPrice: Decimal @min(0)
    totalPrice: Decimal

    operations {
        when calculating total:
            calculate totalPrice as quantity times unitPrice
    }
}
```

See Also:
- nl-to-ddsl/field-declarations.md for field transformation patterns
- nl-to-ddsl/annotations-constraints.md for constraint annotations
- patterns/entity-vs-vo.md for Entity vs Value Object decision guide
