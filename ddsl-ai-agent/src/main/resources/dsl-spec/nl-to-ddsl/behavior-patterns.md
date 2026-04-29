---
id: nl-to-ddsl-behavior-002
category: NL_TO_DDSL
category: TRANSFORMATION_RULE
subcategory: behavior_patterns_natural
language: mixed
complexity: intermediate
version: 3.0
---

# Behavior Transformation Patterns - Natural Style

## Overview

Transform natural language descriptions of domain behaviors into DDSL operations using a natural, readable format. Multiple conditions and actions can be combined in single lines for clarity.

## Natural Behavior Structure

```ddsl
operations {
    when <action description> with <parameters>:
        require that <condition> and <condition>
        <action description>
        <action description>
        emit <EventName> with <properties>
}
```

## Key Principles

1. **Combine related requires**: Use "and" to combine multiple preconditions
2. **Natural action descriptions**: Write actions as readable sentences without bullet points
3. **Preserve natural flow**: Keep the logical flow of the behavior description
4. **CamelCase for identifiers**: Convert all field and parameter names to camelCase

## When Clause

### Natural Action Descriptions

| Natural Language | DDSL When Clause |
|------------------|------------------|
| `When placing an order` | `when placing order` |
| `When order is confirmed` | `when order is confirmed` |
| `When customer places order` | `when customer places order` |
| `When confirming reservation` | `when confirming reservation` |
| `When calculating order total` | `when calculating order total` |
| `When guest checks in` | `when guest checks in` |

### With Parameters

| Natural Language | DDSL When Clause |
|------------------|------------------|
| `with customer and items` | `with customer and items` |
| `with reservationId` | `with reservationId` |
| `with guest and roomType` | `with guest and roomType` |

## Require Clause - Combined Format

### Pattern: Multiple conditions combined with "and"

| Natural Language | DDSL Require Clause |
|------------------|---------------------|
| `status is PENDING and payment is received` | `require that status is "PENDING" and payment is received` |
| `customer is provided and items are not empty` | `require that customer is not empty and items is not empty` |
| `quantity is greater than 0 and price is positive` | `require that quantity is greater than 0 and price is greater than 0` |
| `email is valid and unique` | `require that email is not empty and email is unique` |

### Pattern: Conditions with error messages

| Natural Language | DDSL Require Clause |
|------------------|---------------------|
| `items not empty, error: cart is empty` | `require that items is not empty otherwise "Cart cannot be empty"` |
| `customer exists, otherwise throw error` | `require that customer exists otherwise "Customer not found"` |

## Action Descriptions - Natural Format

### Setting Values

| Natural Language | DDSL Action |
|------------------|-------------|
| `set status to CONFIRMED` | `set status to "CONFIRMED"` |
| `change status to PENDING` | `set status to "PENDING"` |
| `record confirmed at as now` | `set confirmedAt to now` |
| `mark as active` | `set isActive to true` |
| `update total to calculated value` | `set totalAmount to calculatedTotal` |

### Calculations

| Natural Language | DDSL Action |
|------------------|-------------|
| `calculate subtotal as sum of item totals` | `calculate subtotal as sum of items total` |
| `calculate discount based on customer tier` | `calculate discount based on customerTier` |
| `calculate final total as subtotal minus discount` | `calculate finalTotal as subtotal minus discount` |
| `determine shipping cost from weight` | `calculate shippingCost from weight` |

### State Changes

| Natural Language | DDSL Action |
|------------------|-------------|
| `add item to order` | `add item to items` |
| `remove item from cart` | `remove item from items` |
| `create order line from product` | `create orderLine from product` |

## Emit Clause

### Publishing Events

| Natural Language | DDSL Emit Clause |
|------------------|------------------|
| `emit OrderPlaced with order ID` | `emit OrderPlaced with orderId` |
| `emit ReservationConfirmed with reservation ID` | `emit ReservationConfirmed with reservationId` |
| `emit OrderTotalCalculated with total` | `emit OrderTotalCalculated with totalAmount` |
| `publish OrderCancelled event` | `emit OrderCancelled with orderId` |

## Complete Natural Behavior Examples

### Example 1: Confirming Reservation

**Natural Language:**
```
When confirming reservation:
- require that status is "PENDING" and payment is received
- set status to "CONFIRMED"
- record confirmed at as now
- emit ReservationConfirmed with reservation ID
```

**DDSL:**
```ddsl
operations {
    when confirming reservation:
        require that status is "PENDING" and payment is received
        set status to "CONFIRMED"
        set confirmedAt to now
        emit ReservationConfirmed with reservationId
}
```

### Example 2: Calculating Order Total

**Natural Language:**
```
When calculating order total:
- require that items is not empty
- calculate subtotal as sum of item totals
- calculate discount based on customer tier
- calculate final total as subtotal minus discount
- set totalAmount to final total
- emit OrderTotalCalculated with total
```

**DDSL:**
```ddsl
operations {
    when calculating order total:
        require that items is not empty
        calculate subtotal as sum of items total
        calculate discount based on customerTier
        calculate finalTotal as subtotal minus discount
        set totalAmount to finalTotal
        emit OrderTotalCalculated with totalAmount
}
```

### Example 3: Placing Order

**Natural Language:**
```
When placing order with customer and items:
- require that customer is not empty and items is not empty
- calculate total as sum of items price
- set status to "PLACED"
- set created at as now
- emit OrderPlaced with orderId and customer
```

**DDSL:**
```ddsl
operations {
    when placing order with customer and items:
        require that customer is not empty and items is not empty
        calculate total as sum of items price
        set status to "PLACED"
        set createdAt to now
        emit OrderPlaced with orderId and customer
}
```

### Example 4: Checking In Guest

**Natural Language:**
```
When guest checks in:
- require that reservation status is "CONFIRMED" and room is assigned
- set status to "CHECKED_IN"
- record check in time as now
- emit GuestCheckedIn with reservationId and roomNumber
```

**DDSL:**
```ddsl
operations {
    when guest checks in:
        require that status is "CONFIRMED" and roomNumber is not empty
        set status to "CHECKED_IN"
        set checkInTime to now
        emit GuestCheckedIn with reservationId and roomNumber
}
```

### Example 5: Processing Cancellation

**Natural Language:**
```
When processing cancellation:
- require that status is not "DELIVERED" and status is not "CANCELLED"
- set status to "CANCELLED"
- record cancelled at as now
- emit OrderCancelled with orderId
```

**DDSL:**
```ddsl
operations {
    when processing cancellation:
        require that status is not "DELIVERED" and status is not "CANCELLED"
        set status to "CANCELLED"
        set cancelledAt to now
        emit OrderCancelled with orderId
}
```

### Example 6: With Conditional Logic

**Natural Language:**
```
When applying discount with customerTier:
- if customerTier is GOLD, set discount to 20
- if customerTier is SILVER, set discount to 10
- otherwise set discount to 0
- calculate finalPrice as total minus discount
```

**DDSL:**
```ddsl
operations {
    when applying discount with customerTier:
        if customerTier is "GOLD" then set discount to 20
        if customerTier is "SILVER" then set discount to 10
        otherwise set discount to 0
        calculate finalPrice as total minus discount
}
```

### Example 7: With Loop Processing

**Natural Language:**
```
When fulfilling order:
- require that status is "CONFIRMED" and items is not empty
- for each item in items, set item status to "SHIPPED"
- set status to "FULFILLED"
- emit OrderFulfilled with orderId
```

**DDSL:**
```ddsl
operations {
    when fulfilling order:
        require that status is "CONFIRMED" and items is not empty
        for each item in items set item status to "SHIPPED"
        set status to "FULFILLED"
        emit OrderFulfilled with orderId
}
```

## Comparison: Bulleted vs Natural Style

### Bulleted Style (Old)
```ddsl
when confirming reservation:
    require that:
        - status is "PENDING"
        - payment is not empty
    then:
        - set status to "CONFIRMED"
        - set confirmedAt to now
    emit ReservationConfirmed with reservationId
```

### Natural Style (New)
```ddsl
when confirming reservation:
    require that status is "PENDING" and payment is received
    set status to "CONFIRMED"
    record confirmed at as now
    emit ReservationConfirmed with reservationId
```

## Transformation Guidelines

### From Natural Language to DDSL

1. **Start with "when"**: Begin behavior with `when <action>`
2. **Combine requires**: Use "and" to join multiple preconditions
3. **Natural actions**: Write actions as readable statements
4. **CamelCase conversion**: Convert all names to camelCase:
   - `reservation ID` → `reservationId`
   - `confirmed at` → `confirmedAt`
   - `customer tier` → `customerTier`
   - `order items` → `orderItems`

### Common Transformations

| Input Pattern | DDSL Output |
|---------------|-------------|
| `status is PENDING, payment is received` | `status is "PENDING" and payment is received` |
| `record X as now` | `set X to now` |
| `calculate X as sum of Y` | `calculate X as sum of Y` |
| `emit Event with ID` | `emit Event with id` |
| `set field to value` | `set field to value` |

## Context-Specific Patterns

### Aggregate Behaviors

```ddsl
Aggregate Order {
    operations {
        when placing order with customer and items:
            require that customer is not empty and items is not empty
            calculate total as sum of items price
            set status to "PLACED"
            set createdAt to now
            emit OrderPlaced with orderId
    }
}
```

### Domain Service Behaviors

```ddsl
DomainService PricingService {
    when calculating total with items and discountPercent:
        calculate subtotal as sum of items price
        calculate discountAmount as subtotal times discountPercent divided by 100
        calculate total as subtotal minus discountAmount
        return total
}
```

### Factory Behaviors

```ddsl
factories {
    Factory OrderFactory {
        when creating Order from Cart with customerId:
            require that cart is not empty and customerId is not empty
            create order with orderId and customerId
            set order status to "PENDING"
            return order
    }
}
```
