---
id: ddsl-syntax-008
category: SYNTAX_RULE
subcategory: repository
dsl_construct: Repository
complexity: intermediate
---
DDSL Repository Syntax:
Repositories provide persistence abstraction for Aggregates.
Syntax: Repository <Name> for <AggregateType> { <methods> }
Standard methods: findById, findAll, findBy<Field>, save, delete, exists, count
Example:
repositories {
    Repository OrderRepository for Order {
        findById(id: UUID): Order?
        findByCustomer(customerId: UUID): List<Order>
        findByStatus(status: OrderStatus): List<Order>
        save(order: Order): Void
        delete(order: Order): Void
    }
}
