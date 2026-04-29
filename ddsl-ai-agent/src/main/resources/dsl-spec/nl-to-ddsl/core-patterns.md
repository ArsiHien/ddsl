---
id: nl-to-ddsl-core-patterns-001
category: NL_TO_DDSL
category: TRANSFORMATION_RULE
subcategory: core_patterns
language: mixed
complexity: basic
version: 3.0
---

# Core Patterns: Natural Language to DDSL Transformation

## Overview

This guide helps transform natural language descriptions into valid DDSL syntax. The input describes domain concepts in plain language with all the correct information—your job is to normalize the format, convert to camelCase, and apply proper DDSL syntax WITHOUT adding or inferring new entities.

## Key Principle

**DO NOT invent new entities, fields, or behaviors.** Only transform what is explicitly described in the natural language input. Your role is format conversion, not domain expansion.

## Universal Transformation Rules

### 1. Name Normalization

Convert ALL names to camelCase:
- Space-separated → camelCase: `order id` → `orderId`
- snake_case → camelCase: `order_id` → `orderId`
- kebab-case → camelCase: `order-id` → `orderId`
- PascalCase for types: `Order Item` → `OrderItem`

### 2. Field Declaration Pattern

Natural language variations → DDSL field syntax:

| Natural Language Input | DDSL Output |
|------------------------|-------------|
| `fieldName is Type` | `fieldName: Type` |
| `fieldName as Type` | `fieldName: Type` |
| `fieldName of Type` | `fieldName: Type` |
| `fieldName: Type` | `fieldName: Type` (already valid) |

### 3. Identity Field Pattern

Natural language identity descriptions → DDSL identity annotation:

| Natural Language Input | DDSL Output |
|------------------------|-------------|
| `X is UUID and unique` | `X: UUID @identity` |
| `X is the identifier` | `X: UUID @identity` |
| `X is primary key` | `X: UUID @identity` |
| `X serves as identity` | `X: UUID @identity` |
| `unique identifier X` | `X: UUID @identity` |

### 4. Required/Optional Pattern

| Natural Language Input | DDSL Output |
|------------------------|-------------|
| `X is required` | `X: Type @required` |
| `X is mandatory` | `X: Type @required` |
| `X cannot be null` | `X: Type @required` |
| `X must be provided` | `X: Type @required` |
| `X is optional` | `X: Type?` (nullable type) |
| `X may be null` | `X: Type?` |

### 5. Collection Pattern

| Natural Language Input | DDSL Output |
|------------------------|-------------|
| `list of X` | `List<X>` |
| `collection of X` | `List<X>` |
| `multiple X` | `List<X>` |
| `array of X` | `List<X>` |
| `set of X` | `Set<X>` |

## Structure Preservation

When transforming, preserve these structural elements exactly as described:

1. **Bounded Context names** - Keep the context name as provided
2. **Aggregate names** - Transform to PascalCase but keep the concept
3. **Entity names** - Transform to PascalCase but keep the concept
4. **Value Object names** - Transform to PascalCase but keep the concept
5. **Field names** - Transform to camelCase
6. **Behavior descriptions** - Convert to when/then/emit structure

## Example: Complete Transformation

### Natural Language Input:
```
Bounded Context for Order Management:

The Order aggregate has:
- order ID which is UUID and serves as unique identifier
- customer name which is text and required
- items which is a list of OrderItem
- total amount as money, cannot be negative
- status is text
- created at is a timestamp

The Order has these invariants:
- items cannot be empty
- total must be positive

When placing an order:
- customer and items must be provided
- calculate total from item prices
- set status to "PLACED"
- emit OrderPlaced event with order ID
```

### DDSL Output:
```ddsl
BoundedContext OrderManagement {
    domain {
        Aggregate Order {
            orderId: UUID @identity
            customerName: String @required
            items: List<OrderItem>
            totalAmount: Money
            status: String
            createdAt: DateTime

            invariants {
                "items cannot be empty": items is not empty
                "total must be positive": totalAmount is greater than 0
            }

            operations {
                when placing order with customer and items:
                    require that:
                        - customer is not empty
                        - items is not empty
                    given:
                        - totalAmount calculated by sum of items unitPrice
                    then:
                        - set status to "PLACED"
                    emit OrderPlaced with orderId
            }
        }
    }
}
```

## Important Reminders

1. **Preserve meaning, change format only**: Convert natural language to DDSL syntax without changing the semantics
2. **Use exact field names**: If input says "order ID", output must be `orderId`, not `id` or `orderIdentifier`
3. **Don't add constraints not mentioned**: If input doesn't specify @required, don't add it
4. **Keep behaviors minimal**: Convert only the described behavior steps, don't expand logic
5. **Use standard types**: Map natural language types (text, number, timestamp) to DDSL types (String, Int, DateTime)
