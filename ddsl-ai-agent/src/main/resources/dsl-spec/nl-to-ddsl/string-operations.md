---
id: nl-to-ddsl-string-001
category: NL_TO_DDSL
category: ADVANCED_FEATURES
subcategory: string_operations
language: mixed
complexity: intermediate
version: 3.0
---

# String Operations Transformation

## Overview

String operations enable validation and transformation of string fields. Transform natural language string descriptions into DDSL string conditions and operations.

## String Conditions in Require

### Content Checks

| Natural Language | DDSL |
|------------------|------|
| `email contains @` | `email contains "@"` |
| `email does not contain spaces` | `email does not contain " "` |
| `phone starts with 0 or +84` | `phone starts with "0" or phone starts with "+84"` |
| `name does not contain admin` | `name does not contain "admin"` |
| `code ends with XYZ` | `code ends with "XYZ"` |

### Pattern Matching

| Natural Language | DDSL |
|------------------|------|
| `email matches pattern for email` | `email matches "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"` |
| `phone matches format 0 or +84 followed by 9-10 digits` | `phone matches "^(0|\+84)[0-9]{9,10}$"` |
| `code matches format XXX-0000` | `code matches "[A-Z]{3}-[0-9]{4}"` |
| `name does not match invalid characters` | `name does not match "[^a-zA-Z\\s]"` |

### Length Checks

| Natural Language | DDSL |
|------------------|------|
| `name has length at least 2` | `name has length at least 2` |
| `name has length at most 100` | `name has length at most 100` |
| `name has length between 2 and 100` | `name has length between 2 and 100` |
| `code has length at least 3` | `code has length at least 3` |

### Empty/Blank Checks

| Natural Language | DDSL |
|------------------|------|
| `name is not blank` | `name is not blank` |
| `description is not empty` | `description is not empty` |
| `name is blank` | `name is blank` |
| `name is empty` | `name is empty` |

### Format Validation

| Natural Language | DDSL |
|------------------|------|
| `email is valid email` | `email is valid email` |
| `phone is valid phone number` | `phone is valid phone number` |
| `website is valid URL` | `website is valid URL` |
| `product code is valid alphanumeric` | `productCode is valid alphanumeric` |
| `uuid is valid UUID` | `uuid is valid UUID` |

## String Operations in Given/Then

### Transformations

| Natural Language | DDSL |
|------------------|------|
| `normalized name as name trimmed` | `normalizedName as name trimmed` |
| `upper code as code converted to uppercase` | `upperCode as code converted to uppercase` |
| `short description as description truncated to 200 characters` | `shortDescription as description truncated to 200 characters` |
| `slug as name converted to lowercase` | `slug as name converted to lowercase` |
| `clean description as description replaced "<script>" with ""` | `cleanDescription as description replaced "<script>" with ""` |
| `preview as first 100 characters of description` | `preview as first 100 characters of description` |
| `suffix as last 4 characters of code` | `suffix as last 4 characters of code` |
| `full name as first name concatenated with last name` | `fullName as firstName concatenated with lastName` |

## Complete Examples

### Example 1: Customer Registration Validation
```
Natural Language:
When registering customer with email, phone, name, code:
- require that email contains @, error: Email must contain @ symbol
- require that email does not contain spaces, error: Email cannot contain spaces
- require that phone starts with 0 or +84, error: Invalid phone format
- require that email matches email pattern, error: Invalid email format
- require that phone matches phone pattern, error: Invalid phone number
- require that code matches format XXX-0000, error: Code must follow format XXX-0000
- require that name has length at least 2, error: Name too short
- require that name has length at most 100, error: Name too long
- require that name is not blank, error: Name cannot be blank
- require that email is valid email, error: Invalid email format
- require that phone is valid phone number, error: Invalid phone number
- set status to PendingVerification
- emit CustomerRegistered
```

DDSL:
```ddsl
when registering customer with email and phone and name and code:
    require that email contains "@" otherwise "Email must contain @ symbol"
    require that email does not contain " " otherwise "Email cannot contain spaces"
    require that phone starts with "0" or phone starts with "+84" otherwise "Invalid phone format"
    require that email matches "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}" otherwise "Invalid email format"
    require that phone matches "^(0|\+84)[0-9]{9,10}$" otherwise "Invalid phone number"
    require that code matches "[A-Z]{3}-[0-9]{4}" otherwise "Code must follow format: XXX-0000"
    require that name has length at least 2 otherwise "Name too short"
    require that name has length at most 100 otherwise "Name too long"
    require that name is not blank otherwise "Name cannot be blank"
    require that email is valid email otherwise "Invalid email format"
    require that phone is valid phone number otherwise "Invalid phone number"
    set status to "PENDING_VERIFICATION"
    emit CustomerRegistered
```

### Example 2: Product Update with Transformations
```
Natural Language:
When updating product with name, description, code:
- normalized name as name trimmed
- upper code as code converted to uppercase
- short description as description truncated to 200 characters
- slug as name converted to lowercase
- clean description as description replaced "<script>" with ""
- preview as first 100 characters of description
- suffix as last 4 characters of code
- set product name to normalized name
- set product code to upper code
- emit ProductUpdated
```

DDSL:
```ddsl
when updating product with name and description and code:
    normalizedName as name trimmed
    upperCode as code converted to uppercase
    shortDescription as description truncated to 200 characters
    slug as name converted to lowercase
    cleanDescription as description replaced "<script>" with ""
    preview as first 100 characters of description
    suffix as last 4 characters of code
    set productName to normalizedName
    set productCode to upperCode
    emit ProductUpdated
```

## String Operations in Specifications

```ddsl
specifications {
    Specification ValidEmailCustomers {
        matches customers where:
            customer email is valid email
            customer email does not contain "spam"
    }

    Specification PremiumProductCodes {
        matches products where:
            product code starts with "PRE"
            product code matches "PRE-[0-9]{4}-[A-Z]{2}"
    }

    Specification ActiveUsernames {
        matches users where:
            username is not blank
            username has length between 3 and 50
            username matches "[a-zA-Z0-9_]+"
            username does not start with "_"
    }
}
```

## Combined String + Business Logic

```
Natural Language:
When applying promotion code with promotionCode:
- require that promotionCode is not blank, error: Promotion code cannot be empty
- require that promotionCode has length between 4 and 20, error: Invalid length
- require that promotionCode matches pattern, error: Must be uppercase alphanumeric
- require that promotionCode exists in system, error: Not found
- require that promotion is not expired, error: Has expired
- require that order does not have existing promotion, error: Only one allowed
- apply discount amount to total
- record promotion code
- emit PromotionApplied
```

DDSL:
```ddsl
when applying promotion code with promotionCode:
    require that promotionCode is not blank otherwise "Promotion code cannot be empty"
    require that promotionCode has length between 4 and 20 otherwise "Invalid promotion code length"
    require that promotionCode matches "[A-Z0-9]{4,20}" otherwise "Promotion code must be uppercase alphanumeric"
    require that promotionCode exists in system otherwise "Promotion code not found"
    require that promotion is not expired otherwise "Promotion code has expired"
    require that order does not have existing promotion otherwise "Only one promotion code allowed"
    apply discountAmount to totalAmount
    record promotionCode
    emit PromotionApplied
```

## Key Transformation Rules

1. **String literals**: Use double quotes for string values (`"@"`, `" "`)
2. **Pattern matching**: Use regex patterns in double quotes
3. **Escaping**: Escape special regex characters (`\+`, `\.`)
4. **Combined conditions**: Use `and` and `or` for multiple string checks
5. **Error messages**: Add `otherwise "message"` for validation failures
6. **Transformations**: Use `as` for creating transformed variables
7. **Format types**: Use `is valid email`, `is valid URL`, `is valid UUID`
