---
id: nl-to-ddsl-temporal-001
category: NL_TO_DDSL
category: ADVANCED_FEATURES
subcategory: temporal_logic
language: mixed
complexity: advanced
version: 3.0
---

# Temporal Logic Transformation

## Overview

Temporal logic enables time-based conditions in DDSL. Transform natural language time expressions into proper temporal operators.

## Basic Temporal Comparisons

| Natural Language | DDSL Expression |
|------------------|-----------------|
| `created at is before now` | `createdAt is before now` |
| `confirmed at is after created at` | `confirmedAt is after createdAt` |
| `expires at is after now` | `expiresAt is after now` |
| `payment due date is not before today` | `paymentDueDate is not before today` |

## Relative Time Expressions (ago)

| Natural Language | DDSL Expression |
|------------------|-----------------|
| `created at is more than 24 hours ago` | `createdAt is more than 24 hours ago` |
| `last updated is more than 30 days ago` | `lastUpdated is more than 30 days ago` |
| `last login is less than 7 days ago` | `lastLogin is less than 7 days ago` |
| `payment received at is at most 1 hour ago` | `paymentReceivedAt is at most 1 hour ago` |
| `order placed at is at least 5 minutes ago` | `orderPlacedAt is at least 5 minutes ago` |
| `subscription started at is exactly 1 year ago` | `subscriptionStartedAt is exactly 1 year ago` |

## Relative Time Expressions (from now)

| Natural Language | DDSL Expression |
|------------------|-----------------|
| `expires at is less than 7 days from now` | `expiresAt is less than 7 days from now` |
| `delivery date is more than 2 days from now` | `deliveryDate is more than 2 days from now` |
| `trial ends at is within next 3 days` | `trialEndsAt is within next 3 days` |

## Range Expressions

| Natural Language | DDSL Expression |
|------------------|-----------------|
| `created at is within last 24 hours` | `createdAt is within last 24 hours` |
| `expires at is within next 7 days` | `expiresAt is within next 7 days` |
| `last payment is within last 30 days` | `lastPayment is within last 30 days` |
| `delivery date is between today and 30 days from now` | `deliveryDate is between today and 30 days from now` |
| `created at is between start of month and end of month` | `createdAt is between startOfMonth and endOfMonth` |

## Event Ordering

| Natural Language | DDSL Expression |
|------------------|-----------------|
| `confirmed at is after created at` | `confirmedAt is after createdAt` |
| `shipped at occurred after confirmed at` | `shippedAt occurred after confirmedAt` |
| `payment received at is before order confirmed at` | `paymentReceivedAt is before orderConfirmedAt` |

## Complete Behavior Examples

### Example 1: Order Expiration
```
Natural Language:
When order expires:
- require that created at is more than 24 hours ago
- require that status is Pending
- require that payment received at is absent
- set status to Expired
- record expired at as now
- emit OrderExpired with orderId
```

DDSL:
```ddsl
when order expires:
    require that createdAt is more than 24 hours ago and status is "PENDING" and paymentReceivedAt is empty
    set status to "EXPIRED"
    set expiredAt to now
    emit OrderExpired with orderId
```

### Example 2: Subscription Renewal Check
```
Natural Language:
When checking subscription renewal:
- require that subscription expires at is within next 7 days
- require that auto renewal is enabled
- require that last payment is more than 25 days ago
- initiate renewal process
- emit RenewalInitiated with subscriptionId
```

DDSL:
```ddsl
when checking subscription renewal:
    require that subscriptionExpiresAt is within next 7 days and autoRenewal is true and lastPayment is more than 25 days ago
    initiate renewal process
    emit RenewalInitiated with subscriptionId
```

### Example 3: Return Request Validation
```
Natural Language:
When validating return request with returnedAt:
- require that delivered at is more than 0 days ago
- require that delivered at is less than 30 days ago (error: Return window has expired)
- require that returnedAt is after delivered at
- set status to ReturnRequested
- record return requested at as now
- emit ReturnRequested with orderId
```

DDSL:
```ddsl
when validating return request with returnedAt:
    require that deliveredAt is more than 0 days ago and deliveredAt is less than 30 days ago otherwise "Return window has expired" and returnedAt is after deliveredAt
    set status to "RETURN_REQUESTED"
    set returnRequestedAt to now
    emit ReturnRequested with orderId
```

## Temporal Specifications

```ddsl
specifications {
    Specification ExpiredOrders {
        matches orders where:
            createdAt is more than 24 hours ago and status is "PENDING"
    }

    Specification ExpiringSubscriptions given days {
        matches subscriptions where:
            subscriptionExpiresAt is within next days and status is "ACTIVE"
    }

    Specification RecentlyDeliveredOrders {
        matches orders where:
            deliveredAt is within last 7 days and status is "DELIVERED"
    }
}
```

## Duration Units

Valid duration units in DDSL:
- `second` / `seconds`
- `minute` / `minutes`
- `hour` / `hours`
- `day` / `days`
- `week` / `weeks`
- `month` / `months`
- `year` / `years`

## Temporal Anchors

Special time references:
- `now` - current date/time
- `today` - current date
- `yesterday` - previous day
- `tomorrow` - next day

## Key Transformation Rules

1. **Field names**: Convert to camelCase (`created at` → `createdAt`)
2. **Comparisons**: Use `is before`, `is after`, `is within`, `is between`
3. **Relative time**: Use `ago` for past, `from now` for future
4. **Durations**: Use number + unit (`24 hours`, `7 days`)
5. **Combine conditions**: Use `and` to combine temporal conditions
