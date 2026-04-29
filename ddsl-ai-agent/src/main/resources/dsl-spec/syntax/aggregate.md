---
id: ddsl-syntax-001
category: SYNTAX_RULE
subcategory: aggregate
dsl_construct: Aggregate
complexity: basic
version: 3.0
---

DDSL Aggregate Declaration Syntax:

An Aggregate groups related entities under a single root and defines the consistency boundary for transactions.

Syntax:
```
Aggregate <Name> {
    <field declarations>
    <invariants>?
    <operations>?
}
```

Key Points:
- Every Aggregate implicitly creates a root Entity with the same name
- The root Entity must have exactly one @identity field
- Fields are declared as: fieldName: Type @annotations
- Operations define behaviors using natural language clauses

Example:
```ddsl
Aggregate Order {
    orderId: UUID @identity
    customerName: String @required
    items: List<OrderItem>
    totalAmount: Decimal @min(0)
    status: String @default("PENDING")

    invariants {
        items cannot be empty
        total must be positive
    }

    operations {
        when placing order with customer and items:
            require that customer is not empty and items is not empty
            calculate total as sum of items price
            set status to "PENDING"
            emit OrderPlaced with orderId
    }
}
```

Field Declaration Pattern:
- fieldName: camelCase (e.g., orderId, customerName)
- Type: String, Int, Decimal, Boolean, DateTime, UUID, or custom types
- Annotations: @identity, @required, @min(n), @max(n), @minLength(n), @maxLength(n), @unique, @email, @pattern("regex"), @default(value)
