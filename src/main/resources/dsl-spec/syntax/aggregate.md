---
id: ddsl-syntax-001
category: SYNTAX_RULE
subcategory: aggregate
dsl_construct: Aggregate
complexity: basic
---
DDSL Aggregate Declaration Syntax:
An Aggregate groups related entities under a single root.
Syntax: Aggregate <Name> { <fields> <invariants>? <operations>? }
Every Aggregate implicitly creates a root Entity with the same name.
The root Entity must have exactly one @identity field.
Example:
Aggregate Order {
    @identity orderId: UUID
    customer: Customer
    items: List<OrderItem>
    status: OrderStatus
}
