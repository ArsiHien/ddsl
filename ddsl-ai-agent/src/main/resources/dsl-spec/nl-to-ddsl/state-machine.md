---
id: nl-to-ddsl-state-001
category: NL_TO_DDSL
category: ADVANCED_FEATURES
subcategory: state_machine
language: mixed
complexity: advanced
version: 3.0
---

# State Machine Transformation

## Overview

State machines define valid state transitions for entities with lifecycle states. Transform natural language state descriptions into DDSL state machine syntax.

## State Declaration

| Natural Language | DDSL |
|------------------|------|
| `Pending (initial)` | `- Pending (initial)` |
| `Confirmed` | `- Confirmed` |
| `Delivered (final)` | `- Delivered (final)` |
| `Cancelled (final)` | `- Cancelled (final)` |

## Transition Patterns

### Simple Transitions
| Natural Language | DDSL |
|------------------|------|
| `Pending -> Confirmed: when payment received` | `- Pending -> Confirmed: when payment received` |
| `Confirmed -> Shipped: when inventory reserved` | `- Confirmed -> Shipped: when inventory reserved` |
| `Shipped -> Delivered: always` | `- Shipped -> Delivered: always` |

### Multiple Source States
| Natural Language | DDSL |
|------------------|------|
| `[Pending, Confirmed] -> Cancelled: always` | `- [Pending, Confirmed] -> Cancelled: always` |

### Conditional Transitions
| Natural Language | DDSL |
|------------------|------|
| `Delivered -> Returned: only when delivered at is less than 30 days ago` | `- Delivered -> Returned: only when deliveredAt is less than 30 days ago` |
| `Confirmed -> Cancelled: only within 24 hours of confirmed at` | `- Confirmed -> Cancelled: only within 24 hours of confirmedAt` |

### Forbidden Transitions
| Natural Language | DDSL |
|------------------|------|
| `Shipped -> Pending: never` | `- Shipped -> Pending: never` |
| `Delivered -> Pending: never` | `- Delivered -> Pending: never` |
| `Cancelled -> any: never` | `- Cancelled -> any: never` |

## Guards

| Natural Language | DDSL |
|------------------|------|
| `cannot transition from Completed to Refunding when refund window has expired` | `- cannot transition from Completed to Refunding when refundWindowExpired` |
| `must transition from Processing to Failed when timeout exceeds 30 minutes` | `- must transition from Processing to Failed when timeout exceeds 30 minutes` |

## On Entry Actions

| Natural Language | DDSL |
|------------------|------|
| `entering Processing: record processing started at as now, set retry count to 0` | `- entering Processing: record processingStartedAt as now, set retryCount to 0` |
| `entering Completed: record completed at as now, enable receipt generation` | `- entering Completed: record completedAt as now, enable receiptGeneration` |

## On Exit Actions

| Natural Language | DDSL |
|------------------|------|
| `leaving Disputed: record dispute resolved at as now, notify customer` | `- leaving Disputed: record disputeResolvedAt as now, notify customer` |

## Complete State Machine Examples

### Example 1: Order Status
```
Natural Language:
State machine for OrderStatus:
- States: Pending (initial), Confirmed, Shipped, Delivered (final), Cancelled (final), Returned (final)
- Pending -> Confirmed: when payment received
- Confirmed -> Shipped: when inventory reserved
- Shipped -> Delivered: always
- [Pending, Confirmed] -> Cancelled: always
- Delivered -> Returned: only when delivered at is less than 30 days ago
- Confirmed -> Cancelled: only within 24 hours of confirmed at
- Shipped -> Pending: never
- Delivered -> Pending: never
- Cancelled -> any: never
```

DDSL:
```ddsl
state machine for OrderStatus {
    states:
        - Pending (initial)
        - Confirmed
        - Shipped
        - Delivered (final)
        - Cancelled (final)
        - Returned (final)

    transitions:
        - Pending -> Confirmed: when payment received
        - Confirmed -> Shipped: when inventory reserved
        - Shipped -> Delivered: always
        - [Pending, Confirmed] -> Cancelled: always
        - Delivered -> Returned: only when deliveredAt is less than 30 days ago
        - Confirmed -> Cancelled: only within 24 hours of confirmedAt
        - Shipped -> Pending: never
        - Delivered -> Pending: never
        - Cancelled -> any: never
}
```

### Example 2: Payment Status (with Guards and Entry/Exit)
```
Natural Language:
State machine for PaymentStatus:
States: Initiated (initial), Processing, Completed (final), Failed (final), Refunding, Refunded (final), Disputed

Transitions:
- Initiated -> Processing: when payment gateway responds
- Processing -> Completed: when payment gateway confirms success
- Processing -> Failed: when payment gateway confirms failure
- Completed -> Refunding: when refund requested
- Completed -> Disputed: when dispute raised
- Refunding -> Refunded: when refund gateway confirms
- Disputed -> Completed: when dispute resolved in merchant favor
- Disputed -> Refunding: when dispute resolved in customer favor
- Failed -> Initiated: when customer retries
- [Processing, Refunding] -> Failed: when timeout exceeds 30 minutes

Guards:
- cannot transition from Completed to Refunding when refund window expired
- cannot transition from Processing to Completed when amount exceeds fraud threshold
- must transition from Processing to Failed when timeout exceeds 30 minutes

On Entry:
- entering Processing: record processing started at as now, set retry count to 0
- entering Completed: record completed at as now, enable receipt generation
- entering Failed: record failed at as now, increment failure count by 1
- entering Refunding: record refund requested at as now

On Exit:
- leaving Disputed: record dispute resolved at as now, notify customer about resolution
```

DDSL:
```ddsl
state machine for PaymentStatus {
    states:
        - Initiated (initial)
        - Processing
        - Completed (final)
        - Failed (final)
        - Refunding
        - Refunded (final)
        - Disputed

    transitions:
        - Initiated -> Processing: when paymentGateway responds
        - Processing -> Completed: when paymentGateway confirms success
        - Processing -> Failed: when paymentGateway confirms failure
        - Completed -> Refunding: when refund requested
        - Completed -> Disputed: when dispute raised
        - Refunding -> Refunded: when refundGateway confirms
        - Disputed -> Completed: when dispute resolved in merchant favor
        - Disputed -> Refunding: when dispute resolved in customer favor
        - Failed -> Initiated: when customer retries
        - [Processing, Refunding] -> Failed: when timeout exceeds 30 minutes

    guards:
        - cannot transition from Completed to Refunding when refundWindowExpired
        - cannot transition from Processing to Completed when amount exceeds fraud threshold
        - must transition from Processing to Failed when timeout exceeds 30 minutes

    on entry:
        - entering Processing:
            record processingStartedAt as now
            set retryCount to 0

        - entering Completed:
            record completedAt as now
            enable receiptGeneration

        - entering Failed:
            record failedAt as now
            increment failureCount by 1

        - entering Refunding:
            record refundRequestedAt as now

    on exit:
        - leaving Disputed:
            record disputeResolvedAt as now
            notify customer about resolution
}
```

## State Machine Inside Aggregate

```ddsl
Aggregate Order {
    orderId: UUID @identity
    status: OrderStatus @required @default("PENDING")

    state machine for status {
        states:
            - PENDING (initial)
            - CONFIRMED
            - SHIPPED
            - DELIVERED (final)
            - CANCELLED (final)

        transitions:
            - PENDING -> CONFIRMED: when payment received
            - CONFIRMED -> SHIPPED: when inventory reserved
            - SHIPPED -> DELIVERED: always
            - [PENDING, CONFIRMED] -> CANCELLED: always
            - DELIVERED -> CANCELLED: never

        guards:
            - cannot transition from CONFIRMED to CANCELLED when shippedAt is present
            - cannot transition from PENDING to CONFIRMED when totalAmount is less than minimumOrderValue

        on entry:
            - entering CONFIRMED:
                record confirmedAt as now
            - entering SHIPPED:
                record shippedAt as now
            - entering DELIVERED:
                record deliveredAt as now
            - entering CANCELLED:
                record cancelledAt as now
    }

    operations {
        when order is confirmed:
            require that transition from status to CONFIRMED is valid
            transition status to CONFIRMED
            emit OrderConfirmed with orderId

        when order is shipped:
            require that transition from status to SHIPPED is valid
            transition status to SHIPPED
            emit OrderShipped with orderId
    }
}
```

## Key Transformation Rules

1. **State names**: Use UPPERCASE or PascalCase consistently
2. **Initial state**: Mark with `(initial)`
3. **Final states**: Mark with `(final)`
4. **Transitions**: Format as `Source -> Target: condition`
5. **Multiple sources**: Use array syntax `[State1, State2]`
6. **Always transitions**: Use `always` keyword
7. **Never transitions**: Use `never` keyword
8. **Temporal guards**: Use temporal expressions (`within 24 hours`)
9. **Entry/Exit**: Use natural action descriptions without bullet points
