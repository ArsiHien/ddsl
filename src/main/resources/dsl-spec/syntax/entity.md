---
id: ddsl-syntax-002
category: SYNTAX_RULE
subcategory: entity
dsl_construct: Entity
complexity: basic
---
DDSL Entity Declaration Syntax:
An Entity has identity and lifecycle within an Aggregate.
Syntax: Entity <Name> { <fields> <invariants>? <operations>? }
Must have exactly one field annotated with @identity.
Example:
Entity OrderItem {
    @identity itemId: UUID
    product: Product
    quantity: Int @min(1)
    unitPrice: Decimal @min(0)
}
