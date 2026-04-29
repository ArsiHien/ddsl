---
id: ddsl-syntax-006
category: SYNTAX_RULE
subcategory: event
dsl_construct: DomainEvent
complexity: basic
version: 3.0
---

DDSL Domain Event Syntax:

Domain Events capture something that happened in the domain. They are immutable records of facts and enable loose coupling between different parts of the system.

Syntax:
```
events {
    DomainEvent <Name> {
        <field declarations>
    }
}
```

Key Points:
- Name events in past tense (e.g., OrderPlaced, PaymentReceived)
- Include the Aggregate ID that emitted the event
- Include a timestamp field
- Keep events focused — include only data needed by consumers
- Events are emitted from Aggregate operations

Example:
```ddsl
events {
    DomainEvent OrderPlaced {
        orderId: UUID
        customerId: UUID
        orderDate: DateTime
        items: List<OrderItemSnapshot>
    }
}
```

Natural Format Behavior with Event:
```ddsl
Aggregate Order {
    operations {
        when placing order with customer and items:
            require that customer is not empty and items is not empty
            calculate total as sum of items price
            set status to "PLACED"
            emit OrderPlaced with orderId and customer
    }
}
```

See Also:
- nl-to-ddsl/behavior-patterns.md for natural behavior format
- nl-to-ddsl/field-declarations.md for field declarations
- patterns/domain-event.md for DDD event design patterns
