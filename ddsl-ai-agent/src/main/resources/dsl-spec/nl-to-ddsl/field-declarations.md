---
id: nl-to-ddsl-fields-001
category: NL_TO_DDSL
category: TRANSFORMATION_RULE
subcategory: field_declarations
language: mixed
complexity: basic
version: 3.0
---

# Field Declaration Transformation Patterns

## Overview

Transform natural language field descriptions into proper DDSL field declarations. The input provides field information in various natural formats—your task is to extract the field name, type, and constraints, then format them as valid DDSL.

## Field Structure Template

```
fieldName: Type [@annotation1] [@annotation2] ...
```

## Name Transformation

### From Natural Language to camelCase

| Natural Language | DDSL Field Name |
|------------------|-----------------|
| `order id` | `orderId` |
| `order_id` | `orderId` |
| `customer name` | `customerName` |
| `first name` | `firstName` |
| `created at` | `createdAt` |
| `total amount` | `totalAmount` |
| `email address` | `emailAddress` |
| `phone number` | `phoneNumber` |
| `booking date` | `bookingDate` |
| `item count` | `itemCount` |

### Vietnamese to English

| Vietnamese | English DDSL |
|------------|--------------|
| `mã đơn hàng` | `orderId` |
| `tên khách hàng` | `customerName` |
| `ngày tạo` | `createdAt` |
| `trạng thái` | `status` |
| `số lượng` | `quantity` |
| `giá` | `price` |
| `mô tả` | `description` |
| `địa chỉ` | `address` |

## Type Mapping

### Primitive Types

| Natural Language Description | DDSL Type |
|------------------------------|-----------|
| `text`, `string`, `varchar`, `characters` | `String` |
| `integer`, `whole number`, `count`, `number without decimals` | `Int` |
| `long integer`, `big number` | `Long` |
| `decimal`, `float`, `double`, `number with decimals`, `money amount` | `Decimal` |
| `true/false`, `boolean`, `flag`, `yes/no` | `Boolean` |
| `date and time`, `timestamp`, `datetime` | `DateTime` |
| `date`, `calendar date`, `day` | `Date` |
| `UUID`, `unique identifier`, `GUID`, `ID` | `UUID` |

### Complex Types

| Natural Language Description | DDSL Type |
|------------------------------|-----------|
| `list of X`, `collection of X`, `multiple X`, `array of X` | `List<X>` |
| `set of X`, `unique collection of X` | `Set<X>` |
| `map from X to Y`, `dictionary of X to Y` | `Map<X, Y>` |
| `optional X`, `nullable X`, `X or null` | `X?` |

## Constraint Annotations

### Presence Constraints

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `is required` | `@required` |
| `is mandatory` | `@required` |
| `cannot be null` | `@required` |
| `must be provided` | `@required` |
| `is the identity` | `@identity` |
| `serves as identifier` | `@identity` |
| `is unique` | `@unique` |

### Numeric Constraints

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `minimum 0`, `cannot be negative`, `at least 0` | `@min(0)` |
| `minimum 1`, `at least 1` | `@min(1)` |
| `maximum 100`, `at most 100` | `@max(100)` |
| `between 1 and 100` | `@min(1) @max(100)` |
| `greater than 0` | `@min(0)` (implicit exclusive) |

### String Constraints

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `max length 200`, `at most 200 characters` | `@maxLength(200)` |
| `min length 3`, `at least 3 characters` | `@minLength(3)` |
| `length between 3 and 50` | `@minLength(3) @maxLength(50)` |
| `is email format` | `@email` |
| `matches pattern X` | `@pattern("X")` |

### Default Values

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `default is "PENDING"` | `@default("PENDING")` |
| `default value 0` | `@default(0)` |
| `initial value is true` | `@default(true)` |

## Complete Field Transformation Examples

### Example 1: Simple Field
```
Natural Language:
"order ID is UUID and serves as the unique identifier"

DDSL:
orderId: UUID @identity
```

### Example 2: Required String
```
Natural Language:
"customer name is text, required, max length 100 characters"

DDSL:
customerName: String @required @maxLength(100)
```

### Example 3: Numeric with Constraints
```
Natural Language:
"quantity is a whole number, at least 1, at most 100"

DDSL:
quantity: Int @min(1) @max(100)
```

### Example 4: Decimal with Validation
```
Natural Language:
"unit price is decimal, cannot be negative"

DDSL:
unitPrice: Decimal @min(0)
```

### Example 5: Collection
```
Natural Language:
"order items is a list of OrderItem, at least one item required"

DDSL:
orderItems: List<OrderItem> @min(1)
```

### Example 6: Nullable Field
```
Natural Language:
"middle name is text and optional"

DDSL:
middleName: String?
```

### Example 7: Complex Field
```
Natural Language:
"email address is text, required, must be valid email format, max 255 characters"

DDSL:
emailAddress: String @required @email @maxLength(255)
```

### Example 8: DateTime Field
```
Natural Language:
"created at is timestamp, automatically set to current time"

DDSL:
createdAt: DateTime
```

### Example 9: Boolean Field
```
Natural Language:
"is active is a flag that indicates if the record is active, default is true"

DDSL:
isActive: Boolean @default(true)
```

### Example 10: Money/Decimal
```
Natural Language:
"total amount is money, must be positive, represents the order total"

DDSL:
totalAmount: Decimal @min(0)
```

## Common Transformation Patterns

### Pattern: "X is Y and Z"
```
Input:  "name is text and required"
Output: "name: String @required"
```

### Pattern: "X as Y with constraints"
```
Input:  "price as decimal with minimum 0"
Output: "price: Decimal @min(0)"
```

### Pattern: "X of type Y"
```
Input:  "items of type OrderItem"
Output: "items: OrderItem"

Input:  "list of OrderItem"
Output: "items: List<OrderItem>"
```

### Pattern: "X field which is Y"
```
Input:  "order ID field which is UUID"
Output: "orderId: UUID"
```

## Field Declaration in Different Constructs

### In Aggregates
```ddsl
Aggregate Order {
    orderId: UUID @identity
    customerName: String @required
    items: List<OrderItem>
}
```

### In Entities
```ddsl
Entity OrderItem {
    itemId: UUID @identity
    productName: String @required
    quantity: Int @min(1)
}
```

### In Value Objects
```ddsl
ValueObject Money {
    amount: Decimal @min(0)
    currency: String @required @maxLength(3)
}
```

### In Domain Events
```ddsl
DomainEvent OrderPlaced {
    orderId: UUID
    customerName: String
    occurredAt: DateTime
}
```

### In Use Case Inputs
```ddsl
UseCase PlaceOrder {
    input: PlaceOrderRequest {
        customerId: UUID @required
        items: List<OrderItem> @required
    }
}
```
