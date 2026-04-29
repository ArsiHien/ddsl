---
id: ddsl-syntax-008
category: SYNTAX_RULE
subcategory: repository
dsl_construct: Repository
complexity: intermediate
version: 3.0
---

DDSL Repository Syntax:

Repositories provide persistence abstraction for Aggregates. They act as collections of domain objects and hide the details of database access.

Syntax:
```
repositories {
    Repository <Name> for <AggregateType> {
        <method signatures>
    }
}
```

Standard Methods:
- findById(id: UUID): T? — returns nullable (may not exist)
- findAll(): List<T> — returns all entities
- findBy<Field>(value): List<T> — query by field
- save(entity: T): Void — persist (create or update)
- delete(entity: T): Void — remove
- exists(id: UUID): Boolean — check existence
- count(): Long — total count

Key Points:
- Repositories only exist for Aggregate Roots, never for internal Entities
- Return types use nullable (?) for single item lookups
- Method names follow findBy<Field>, findAllBy<Field> patterns
- Repositories are interfaces — implementation is generated

Example:
```ddsl
repositories {
    Repository OrderRepository for Order {
        findById(id: UUID): Order?
        findByCustomer(customerId: UUID): List<Order>
        findByStatus(status: OrderStatus): List<Order>
        save(order: Order): Void
        delete(order: Order): Void
        exists(id: UUID): Boolean
        count(): Long
    }
}
```

See Also:
- patterns/repository.md for DDD repository design patterns
- nl-to-ddsl/complete-examples.md for examples with repositories
