---
id: nl-to-ddsl-quickref-001
category: NL_TO_DDSL
category: QUICK_REFERENCE
subcategory: transformation_lookup
language: mixed
complexity: basic
version: 3.0
---

# Quick Reference: Natural Language to DDSL

## Field Name Normalization

| Input | Output |
|-------|--------|
| `order id` | `orderId` |
| `customer name` | `customerName` |
| `created at` | `createdAt` |
| `email address` | `emailAddress` |
| `phone number` | `phoneNumber` |
| `total amount` | `totalAmount` |
| `booking date` | `bookingDate` |
| `is active` | `isActive` |

## Type Mapping

| Natural Language | DDSL Type |
|------------------|-----------|
| `text`, `string`, `characters` | `String` |
| `integer`, `whole number`, `count` | `Int` |
| `long`, `big number` | `Long` |
| `decimal`, `float`, `price`, `money` | `Decimal` |
| `boolean`, `flag`, `true/false` | `Boolean` |
| `date time`, `timestamp` | `DateTime` |
| `date` | `Date` |
| `UUID`, `identifier`, `ID` | `UUID` |
| `list of X` | `List<X>` |
| `set of X` | `Set<X>` |
| `map from K to V` | `Map<K, V>` |
| `optional X` | `X?` |

## Annotations

| Natural Language | DDSL |
|------------------|------|
| `is identity` / `unique identifier` | `@identity` |
| `is required` / `mandatory` | `@required` |
| `minimum N` / `at least N` | `@min(N)` |
| `maximum N` / `at most N` | `@max(N)` |
| `min length N` | `@minLength(N)` |
| `max length N` | `@maxLength(N)` |
| `must be unique` | `@unique` |
| `valid email` | `@email` |
| `matches pattern` | `@pattern("...")` |
| `default is X` | `@default(X)` |
| `immutable` | `@immutable` |

## Behavior Structure

### Template
```ddsl
when <action> with <params>:
    require that <condition> and <condition>
    <action description>
    <action description>
    emit <Event> with <props>
```

### When Clause

| Input | Output |
|-------|--------|
| `When placing order` | `when placing order` |
| `When confirming reservation` | `when confirming reservation` |
| `When calculating total` | `when calculating total` |
| `with customer and items` | `with customer and items` |

### Require Clause

| Input | Output |
|-------|--------|
| `require A and B` | `require that A and B` |
| `status is PENDING` | `status is "PENDING"` |
| `items not empty` | `items is not empty` |
| `price > 0` | `price is greater than 0` |

### Actions

| Input | Output |
|-------|--------|
| `set status to CONFIRMED` | `set status to "CONFIRMED"` |
| `record confirmed at as now` | `set confirmedAt to now` |
| `calculate total as sum of items` | `calculate total as sum of items` |
| `emit Event with ID` | `emit Event with id` |

## Structure Templates

### Aggregate
```ddsl
Aggregate Name {
    fieldName: Type @annotations
    
    operations {
        when action with params:
            require that condition and condition
            set field to value
            emit Event with props
    }
}
```

### Entity
```ddsl
Entity Name {
    fieldName: Type @annotations
}
```

### Value Object
```ddsl
ValueObject Name {
    fieldName: Type @annotations
}
```

### Domain Event
```ddsl
DomainEvent EventName {
    fieldName: Type
}
```

### Specification
```ddsl
Specification Name {
    matches Type where:
        - condition
}
```

## Vietnamese Mappings

| Vietnamese | English DDSL |
|------------|--------------|
| `mã` / `mã định danh` | `id` / `UUID` |
| `tên` | `name` |
| `chuỗi` | `String` |
| `số nguyên` | `Int` |
| `số thực` | `Decimal` |
| `đúng/sai` | `Boolean` |
| `ngày` | `Date` |
| `thờ gian` | `DateTime` |
| `danh sách` | `List` |
| `bắt buộc` | `@required` |
| `ngày tạo` | `createdAt` |
| `trạng thái` | `status` |
| `giá` / `tiền` | `price` / `amount` |

## Common Patterns

### Identity Field
```
Input:  "X is UUID and serves as identity"
Output: "X: UUID @identity"
```

### Required String
```
Input:  "name is text, required, max 200 chars"
Output: "name: String @required @maxLength(200)"
```

### Collection
```
Input:  "items is list of OrderItem, at least 1"
Output: "items: List<OrderItem> @min(1)"
```

### Behavior
```
Input:
  "When confirming:
   - require status is PENDING and payment received
   - set status to CONFIRMED
   - emit Confirmed with id"

Output:
  "when confirming:
      require that status is "PENDING" and payment is received
      set status to "CONFIRMED"
      emit Confirmed with id"
```

## Dos and Don'ts

### DO
- Transform names to camelCase
- Map natural types to DDSL types
- Combine multiple requires with "and"
- Write actions as natural sentences
- Preserve exact meaning from input

### DON'T
- Add entities not mentioned
- Add fields not described
- Expand behavior logic
- Use bullet points in then clauses
- Change the domain meaning

## One-Page Example

**Natural Language Input:**
```
Order aggregate with:
- order ID is UUID, identity
- customer name, required text
- items is list of LineItem
- total is decimal, min 0
- status defaults to PENDING

When placing with customer and items:
- require customer not empty and items not empty
- calculate total from items
- set status to PENDING
- emit Placed with orderId
```

**DDSL Output:**
```ddsl
Aggregate Order {
    orderId: UUID @identity
    customerName: String @required
    items: List<LineItem>
    total: Decimal @min(0)
    status: String @default("PENDING")

    operations {
        when placing with customer and items:
            require that customer is not empty and items is not empty
            calculate total from items
            set status to "PENDING"
            emit Placed with orderId
    }
}
```
