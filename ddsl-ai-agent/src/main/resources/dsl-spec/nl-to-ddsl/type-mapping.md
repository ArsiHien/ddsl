---
id: nl-to-ddsl-types-001
category: NL_TO_DDSL
category: TRANSFORMATION_RULE
subcategory: type_mapping
language: mixed
complexity: basic
version: 3.0
---

# Type Mapping and Normalization Guide

## Overview

Transform natural language type descriptions into DDSL type declarations. This guide covers primitive types, collection types, custom types, and nullable types.

## Primitive Type Mappings

### String Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `text` | `String` | General text |
| `string` | `String` | General text |
| `varchar` | `String` | Database term |
| `characters` | `String` | Character sequence |
| `word` | `String` | Short text |
| `sentence` | `String` | Medium text |
| `paragraph` | `String` | Long text |
| `chuỗi` | `String` | Vietnamese |
| `văn bản` | `String` | Vietnamese |

### Integer Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `integer` | `Int` | Whole number |
| `int` | `Int` | Short form |
| `whole number` | `Int` | No decimals |
| `count` | `Int` | Countable quantity |
| `number without decimals` | `Int` | Explicit no decimals |
| `số nguyên` | `Int` | Vietnamese |
| `long` | `Long` | Large integer |
| `big number` | `Long` | Very large integer |
| `số nguyên dài` | `Long` | Vietnamese |

### Decimal Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `decimal` | `Decimal` | Precise decimal |
| `float` | `Decimal` | Floating point |
| `double` | `Decimal` | Double precision |
| `number with decimals` | `Decimal` | Has decimal point |
| `real number` | `Decimal` | Mathematical real |
| `price` | `Decimal` | Monetary value |
| `amount` | `Decimal` | Numeric amount |
| `money value` | `Decimal` | Financial amount |
| `số thực` | `Decimal` | Vietnamese |
| `tiền` | `Decimal` | Vietnamese money |

### Boolean Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `boolean` | `Boolean` | True/false |
| `bool` | `Boolean` | Short form |
| `true/false` | `Boolean` | Binary value |
| `flag` | `Boolean` | Indicator |
| `yes/no` | `Boolean` | Binary choice |
| `đúng/sai` | `Boolean` | Vietnamese |
| `logic` | `Boolean` | Logical value |

### Date and Time Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `datetime` | `DateTime` | Date + time |
| `date and time` | `DateTime` | Combined |
| `timestamp` | `DateTime` | Point in time |
| `thờ gian` | `DateTime` | Vietnamese |
| `ngày giờ` | `DateTime` | Vietnamese |
| `date` | `Date` | Calendar date |
| `calendar date` | `Date` | Date only |
| `day` | `Date` | Day date |
| `ngày` | `Date` | Vietnamese |

### Identifier Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `UUID` | `UUID` | Universal unique ID |
| `unique identifier` | `UUID` | Generic ID |
| `identifier` | `UUID` | ID field |
| `ID` | `UUID` | Short form |
| `GUID` | `UUID` | Microsoft term |
| `mã định danh` | `UUID` | Vietnamese |
| `mã` | `UUID` | Vietnamese (context dependent) |

## Collection Type Mappings

### List Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `list of X` | `List<X>` | Ordered collection |
| `collection of X` | `List<X>` | Generic collection |
| `array of X` | `List<X>` | Array-like |
| `multiple X` | `List<X>` | Many items |
| `danh sách X` | `List<X>` | Vietnamese |
| `sequence of X` | `List<X>` | Ordered sequence |

### Set Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `set of X` | `Set<X>` | Unique items |
| `unique collection of X` | `Set<X>` | No duplicates |
| `distinct X` | `Set<X>` | Unique values |

### Map Types

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `map from X to Y` | `Map<X, Y>` | Key-value pairs |
| `dictionary of X to Y` | `Map<X, Y>` | Lookup table |
| `key-value pairs` | `Map<X, Y>` | Associative array |
| `hash map` | `Map<X, Y>` | Implementation |

## Nullable Type Mappings

| Natural Language | DDSL Type | Notes |
|------------------|-----------|-------|
| `optional X` | `X?` | May be null |
| `nullable X` | `X?` | Can be null |
| `X or null` | `X?` | Either/or |
| `X may be empty` | `X?` | Optional |
| `not required` | `X?` | Optional field |

## Custom Type Handling

### Value Objects

When the natural language describes a concept that should be a Value Object:

```
Natural Language:
"Money with amount and currency"

DDSL ValueObject:
ValueObject Money {
    amount: Decimal @min(0)
    currency: String @required @maxLength(3)
}

Usage:
totalAmount: Money
```

### Domain Types

Custom types defined in the domain:

```
Natural Language:
"OrderStatus as enumeration: PENDING, CONFIRMED, SHIPPED"

DDSL (implied enum):
status: OrderStatus
```

## Type Transformation Examples

### Example 1: Simple Types

```
Natural Language:
"The Order has:
- order ID which is UUID
- customer name which is text
- total amount which is decimal
- is confirmed which is true/false
- created at which is timestamp"

DDSL:
Aggregate Order {
    orderId: UUID
    customerName: String
    totalAmount: Decimal
    isConfirmed: Boolean
    createdAt: DateTime
}
```

### Example 2: Collection Types

```
Natural Language:
"The Order has:
- items which is a list of OrderItem
- tags which is a set of String
- metadata which is a map from String to String"

DDSL:
Aggregate Order {
    items: List<OrderItem>
    tags: Set<String>
    metadata: Map<String, String>
}
```

### Example 3: Nullable Types

```
Natural Language:
"The Customer has:
- name which is text and required
- middle name which is text and optional
- phone which may be null"

DDSL:
Aggregate Customer {
    name: String @required
    middleName: String?
    phone: String?
}
```

### Example 4: Mixed Types with Constraints

```
Natural Language:
"The Product has:
- product ID which is UUID and unique
- name which is text, required, max 200 chars
- price which is decimal, cannot be negative
- quantity which is whole number, at least 0
- is active which is flag, default true
- categories which is list of String"

DDSL:
Aggregate Product {
    productId: UUID @identity
    name: String @required @maxLength(200)
    price: Decimal @min(0)
    quantity: Int @min(0)
    isActive: Boolean @default(true)
    categories: List<String>
}
```

### Example 5: Vietnamese Input

```
Natural Language (Vietnamese):
"Đơn hàng có:
- mã đơn hàng là UUID
- tên khách hàng là chuỗi, bắt buộc
- ngày tạo là thờ gian
- tổng tiền là số thực, không âm
- trạng thái là chuỗi"

DDSL:
Aggregate Order {
    orderId: UUID
    customerName: String @required
    createdAt: DateTime
    totalAmount: Decimal @min(0)
    status: String
}
```

## Type Inference Rules

When the natural language is ambiguous, use these rules:

### Rule 1: ID Fields
If the field name contains "id", "code", or "identifier", use `UUID`:
- `order id` → `UUID`
- `product code` → `UUID` or `String`
- `customer identifier` → `UUID`

### Rule 2: Date Fields
If the field name contains date/time words, use `DateTime`:
- `created at` → `DateTime`
- `updated date` → `DateTime`
- `expiry` → `DateTime`

### Rule 3: Flag Fields
If the field name starts with "is" or "has", use `Boolean`:
- `is active` → `Boolean`
- `has discount` → `Boolean`
- `is verified` → `Boolean`

### Rule 4: Amount/Price Fields
If the field name contains money words, use `Decimal`:
- `total amount` → `Decimal`
- `price` → `Decimal`
- `discount` → `Decimal`

### Rule 5: Count/Quantity Fields
If the field name implies counting, use `Int`:
- `quantity` → `Int`
- `count` → `Int`
- `number of items` → `Int`

## Quick Reference: Natural Language → DDSL Types

| Category | Natural Language | DDSL |
|----------|------------------|------|
| Text | text, string, characters, chuỗi | `String` |
| Integer | integer, whole number, count, số nguyên | `Int` |
| Long | long, big number, số nguyên dài | `Long` |
| Decimal | decimal, float, price, amount, tiền | `Decimal` |
| Boolean | boolean, flag, true/false, đúng/sai | `Boolean` |
| DateTime | datetime, timestamp, thờ gian | `DateTime` |
| Date | date, day, ngày | `Date` |
| UUID | UUID, ID, identifier, mã | `UUID` |
| List | list of, collection of, danh sách | `List<X>` |
| Set | set of, unique collection | `Set<X>` |
| Map | map, dictionary, key-value | `Map<K,V>` |
| Nullable | optional, nullable, may be null | `X?` |
