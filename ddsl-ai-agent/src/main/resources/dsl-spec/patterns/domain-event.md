---
id: ddsl-pattern-003
category: DDD_PATTERN
subcategory: domain_event
complexity: intermediate
version: 3.0
---

DDD Pattern: Domain Event Design

Domain Events capture something that happened in the domain. They are the primary mechanism for loose coupling between Aggregates and subsystems.

Design Rules:
1. Name events in past tense: OrderPlaced, PaymentReceived, CustomerRegistered
2. Events are immutable records of something that happened — they cannot be changed
3. Include the Aggregate ID that emitted the event
4. Include a timestamp field (usually DateTime)
5. Include only the data needed by consumers — don't include everything
6. Events should be emitted from Aggregate operations
7. Event consumers should not fail — use eventual consistency

Event Structure:
```ddsl
DomainEvent OrderPlaced {
    orderId: UUID — the Aggregate that emitted the event
    customerId: UUID — reference to related Aggregate
    totalAmount: Decimal — data consumers need
    occurredAt: DateTime — when it happened
}
```

Emitting Events (Natural Format):
```ddsl
Aggregate Order {
    operations {
        when placing order:
            require that items is not empty
            calculate total as sum of items price
            set status to "PLACED"
            emit OrderPlaced with orderId, customerId, and total
    }
}
```

Common Event Names:
- OrderPlaced, OrderConfirmed, OrderShipped, OrderDelivered
- CustomerRegistered, CustomerActivated, CustomerDeactivated
- PaymentReceived, PaymentFailed, PaymentRefunded
- InventoryReserved, InventoryReleased

See Also:
- syntax/domain-event.md for event syntax
- nl-to-ddsl/behavior-patterns.md for natural emit patterns
