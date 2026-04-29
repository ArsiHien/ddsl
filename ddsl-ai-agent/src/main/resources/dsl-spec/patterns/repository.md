---
id: ddsl-pattern-004
category: DDD_PATTERN
subcategory: repository
complexity: basic
version: 3.0
---

DDD Pattern: Repository Interface Design

Repositories act as collections of domain objects and provide abstraction over persistence mechanisms. They allow the domain model to remain persistence-ignorant.

Standard Repository Methods:
- findById(id: UUID): T? — returns nullable (may not exist)
- findAll(): List<T> — returns all entities
- findBy<Field>(value): List<T> — query by field
- save(entity: T): Void — persist (create or update)
- delete(entity: T): Void — remove
- exists(id: UUID): Boolean — check existence
- count(): Long — total count

Design Rules:
1. Repositories only exist for Aggregate Roots, never for internal Entities
2. Return types use nullable (?) for single item lookups that may fail
3. Method names follow findBy<Field> pattern for custom queries
4. Repositories are interfaces — implementation is generated or provided by infrastructure
5. Don't put business logic in repositories — keep them simple collection-like interfaces

Example:
```ddsl
repositories {
    Repository OrderRepository for Order {
        findById(id: UUID): Order?
        findAll(): List<Order>
        findByCustomer(customerId: UUID): List<Order>
        findByStatus(status: OrderStatus): List<Order>
        save(order: Order): Void
        delete(order: Order): Void
        exists(id: UUID): Boolean
        count(): Long
    }
}
```

Example with Specifications:
```ddsl
repositories {
    Repository OrderRepository for Order {
        findById(id: UUID): Order?
        findAll(): List<Order>
        findBySpecification(spec: Specification<Order>): List<Order>
        save(order: Order): Void
    }
}
```

See Also:
- syntax/repository.md for syntax reference
- syntax/specification.md for specification syntax
- nl-to-ddsl/complete-examples.md for repository examples
