---
id: ddsl-syntax-006
category: SYNTAX_RULE
subcategory: event
dsl_construct: DomainEvent
complexity: basic
---
DDSL Domain Event Syntax:
Domain Events capture something that happened in the domain.
Syntax: DomainEvent <Name> { <fields> }
Defined inside the 'events { ... }' section of a BoundedContext.
Example:
events {
    DomainEvent OrderPlaced {
        orderId: UUID
        customerId: UUID
        orderDate: DateTime
        items: List<OrderItemSnapshot>
    }
}
