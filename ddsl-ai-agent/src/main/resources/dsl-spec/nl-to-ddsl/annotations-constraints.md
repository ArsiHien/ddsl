---
id: nl-to-ddsl-annotations-001
category: NL_TO_DDSL
category: TRANSFORMATION_RULE
subcategory: annotations_constraints
language: mixed
complexity: basic
version: 3.0
---

# Annotation and Constraint Transformation Guide

## Overview

Transform natural language descriptions of constraints and validations into DDSL annotations. Annotations in DDSL provide metadata about fields, including validation rules, identity markers, and behavioral hints.

## Annotation Syntax

```
fieldName: Type @annotation1 @annotation2(param) @annotation3
```

## Identity Annotation (@identity)

### Natural Language Indicators

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `is the unique identifier` | `@identity` |
| `serves as primary key` | `@identity` |
| `is UUID and unique` | `@identity` (on UUID field) |
| `is the identity field` | `@identity` |
| `uniquely identifies the entity` | `@identity` |
| `acts as the ID` | `@identity` |

### Examples

```
Natural Language:
"order ID is UUID and serves as the unique identifier for the order"

DDSL:
orderId: UUID @identity
```

```
Natural Language:
"reservation ID uniquely identifies the reservation, must be UUID"

DDSL:
reservationId: UUID @identity
```

## Required Annotation (@required)

### Natural Language Indicators

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `is required` | `@required` |
| `is mandatory` | `@required` |
| `must be provided` | `@required` |
| `cannot be null` | `@required` |
| `must not be empty` | `@required` |
| `is compulsory` | `@required` |
| `bắt buộc` (Vietnamese) | `@required` |

### Examples

```
Natural Language:
"customer name is text and required"

DDSL:
customerName: String @required
```

```
Natural Language:
"email address is mandatory and must be valid format"

DDSL:
emailAddress: String @required @email
```

## Min/Max Annotations (@min, @max)

### Natural Language Indicators for @min

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `minimum 0` | `@min(0)` |
| `at least 1` | `@min(1)` |
| `cannot be negative` | `@min(0)` |
| `must be positive` | `@min(1)` |
| `no less than 10` | `@min(10)` |
| `greater than or equal to 0` | `@min(0)` |

### Natural Language Indicators for @max

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `maximum 100` | `@max(100)` |
| `at most 100` | `@max(100)` |
| `no more than 50` | `@max(50)` |
| `less than or equal to 100` | `@max(100)` |
| `cannot exceed 1000` | `@max(1000)` |

### Examples

```
Natural Language:
"quantity is integer, at least 1, at most 100"

DDSL:
quantity: Int @min(1) @max(100)
```

```
Natural Language:
"price is decimal, cannot be negative"

DDSL:
price: Decimal @min(0)
```

```
Natural Language:
"discount percentage is between 0 and 100"

DDSL:
discountPercent: Decimal @min(0) @max(100)
```

## Length Annotations (@minLength, @maxLength)

### Natural Language Indicators for @minLength

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `minimum length 3` | `@minLength(3)` |
| `at least 3 characters` | `@minLength(3)` |
| `no shorter than 5 chars` | `@minLength(5)` |

### Natural Language Indicators for @maxLength

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `maximum length 200` | `@maxLength(200)` |
| `at most 255 characters` | `@maxLength(255)` |
| `no longer than 100 chars` | `@maxLength(100)` |
| `max chars 50` | `@maxLength(50)` |

### Examples

```
Natural Language:
"product name is text, required, max 200 characters"

DDSL:
productName: String @required @maxLength(200)
```

```
Natural Language:
"username is text, min 3 chars, max 50 chars"

DDSL:
username: String @minLength(3) @maxLength(50)
```

```
Natural Language:
"currency code is text, exactly 3 characters"

DDSL:
currencyCode: String @required @maxLength(3) @minLength(3)
```

## Unique Annotation (@unique)

### Natural Language Indicators

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `must be unique` | `@unique` |
| `cannot have duplicates` | `@unique` |
| `must be unique across all records` | `@unique` |
| `unique identifier` | `@unique` (if not identity) |

### Examples

```
Natural Language:
"email must be unique across all customers"

DDSL:
email: String @required @unique
```

```
Natural Language:
"room number is text, required, must be unique per hotel"

DDSL:
roomNumber: String @required @unique
```

## Email Annotation (@email)

### Natural Language Indicators

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `must be valid email` | `@email` |
| `email format required` | `@email` |
| `valid email address` | `@email` |
| `must contain @ symbol` | `@email` |

### Examples

```
Natural Language:
"email address is text, required, must be valid email"

DDSL:
emailAddress: String @required @email
```

## Pattern Annotation (@pattern)

### Natural Language Indicators

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `must match pattern X` | `@pattern("X")` |
| `format: XXX-0000` | `@pattern("[A-Z]{3}-[0-9]{4}")` |
| `regex pattern required` | `@pattern("regex")` |
| `must follow format X` | `@pattern("X")` |

### Examples

```
Natural Language:
"product code must match pattern: 3 letters, hyphen, 4 digits"

DDSL:
productCode: String @required @pattern("[A-Z]{3}-[0-9]{4}")
```

```
Natural Language:
"phone number must match format: starts with 0 or +84, then 9-10 digits"

DDSL:
phoneNumber: String @required @pattern("^(0|\\+84)[0-9]{9,10}$")
```

## Default Annotation (@default)

### Natural Language Indicators

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `default value is X` | `@default(X)` |
| `initial value is X` | `@default(X)` |
| `defaults to X` | `@default(X)` |
| `automatically set to X` | `@default(X)` |

### Examples

```
Natural Language:
"status defaults to PENDING"

DDSL:
status: String @default("PENDING")
```

```
Natural Language:
"is active flag, default is true"

DDSL:
isActive: Boolean @default(true)
```

```
Natural Language:
"created at is timestamp, automatically set to current time"

DDSL:
createdAt: DateTime
```

## Immutable Annotation (@immutable)

### Natural Language Indicators

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `cannot be changed` | `@immutable` |
| `is immutable` | `@immutable` |
| `read-only after creation` | `@immutable` |
| `cannot be modified` | `@immutable` |

### Examples

```
Natural Language:
"created at timestamp, immutable after creation"

DDSL:
createdAt: DateTime @immutable
```

```
Natural Language:
"order ID is immutable once set"

DDSL:
orderId: UUID @identity @immutable
```

## Precision Annotation (@precision)

### Natural Language Indicators

| Natural Language | DDSL Annotation |
|------------------|-----------------|
| `precision 10, scale 2` | `@precision(10, 2)` |
| `2 decimal places` | `@precision(10, 2)` (typical) |
| `money format with 2 decimals` | `@precision(19, 4)` |

### Examples

```
Natural Language:
"amount is decimal with 2 decimal places for currency"

DDSL:
amount: Decimal @precision(19, 4)
```

## Nullable Types (Type?)

### Natural Language Indicators

| Natural Language | DDSL Syntax |
|------------------|-------------|
| `is optional` | `Type?` |
| `may be null` | `Type?` |
| `can be empty` | `Type?` |
| `nullable` | `Type?` |
| `not required` | `Type?` |

### Examples

```
Natural Language:
"middle name is text and optional"

DDSL:
middleName: String?
```

```
Natural Language:
"description may be null"

DDSL:
description: String?
```

## Combining Multiple Annotations

Natural language often describes multiple constraints that translate to multiple annotations:

### Example 1: Complex Field

```
Natural Language:
"email address is text, required, must be valid email format, 
 max 255 characters, must be unique"

DDSL:
emailAddress: String @required @email @maxLength(255) @unique
```

### Example 2: Numeric Field

```
Natural Language:
"quantity is integer, required, minimum 1, maximum 1000, default is 1"

DDSL:
quantity: Int @required @min(1) @max(1000) @default(1)
```

### Example 3: Identity Field

```
Natural Language:
"order ID is UUID, serves as unique identifier, immutable"

DDSL:
orderId: UUID @identity @immutable
```

## Annotation Order Convention

While order doesn't affect functionality, use this convention for consistency:

1. `@identity` (if applicable)
2. `@required` (if applicable)
3. Validation annotations (`@min`, `@max`, `@minLength`, `@maxLength`)
4. Format annotations (`@email`, `@pattern`)
5. Business rule annotations (`@unique`)
6. Default annotations (`@default`)
7. Lifecycle annotations (`@immutable`)

## Quick Reference Table

| Constraint Type | Natural Language Keywords | DDSL Annotation |
|-----------------|---------------------------|-----------------|
| Identity | unique identifier, primary key, ID | `@identity` |
| Required | required, mandatory, compulsory, bắt buộc | `@required` |
| Minimum value | minimum, at least, no less than | `@min(n)` |
| Maximum value | maximum, at most, no more than | `@max(n)` |
| Minimum length | min length, at least N chars | `@minLength(n)` |
| Maximum length | max length, at most N chars | `@maxLength(n)` |
| Uniqueness | unique, no duplicates | `@unique` |
| Email format | valid email, email format | `@email` |
| Pattern | matches pattern, format | `@pattern("...")` |
| Default | default is, defaults to | `@default(value)` |
| Immutable | cannot change, read-only | `@immutable` |
| Nullable | optional, may be null | `Type?` |
