---
id: nl-to-ddsl-error-001
category: NL_TO_DDSL
category: ADVANCED_FEATURES
subcategory: error_accumulation
language: mixed
complexity: intermediate
version: 3.0
---

# Error Accumulation Transformation

## Overview

Error accumulation allows collecting multiple validation errors at once instead of failing on the first error. Transform natural language validation descriptions into DDSL error accumulation syntax.

## Collect All Errors

### Basic Pattern
```
Natural Language:
When validating order placement with customerId and items:
Collect all errors:
- customerId is present, error: Customer ID is required
- items is not empty, error: Cart cannot be empty
- all items have quantity greater than 0, error: All items must have positive quantity
- all items have unit price greater than 0, error: All items must have valid price
- count of items is at most 100, error: Cart cannot exceed 100 items
- customer exists in system, error: Customer not found
- all items have product exists in system, error: Some products do not exist
Fail if any errors
```

DDSL:
```ddsl
when validating order placement with customerId and items:
    collect all errors:
        customerId is present otherwise "Customer ID is required"
        items is not empty otherwise "Cart cannot be empty"
        all items have quantity greater than 0 otherwise "All items must have positive quantity"
        all items have unitPrice greater than 0 otherwise "All items must have valid price"
        count of items is at most 100 otherwise "Cart cannot exceed 100 items"
        customer exists in system otherwise "Customer not found"
        all items have product exists in system otherwise "Some products do not exist"
    fail if any errors
```

## Collect with Warnings

```
Natural Language:
When validating customer registration with email, phone, name:
Collect all errors:
Hard errors:
- email is present, error: Email is required
- email contains @, error: Invalid email format
- email does not exist in system, error: Email already registered
- name is present, error: Name is required
- name has length at least 2, error: Name too short
Warnings:
- phone is present, warning: Phone number recommended for delivery
- name has length at most 50, warning: Name seems unusually long
Fail if critical errors
```

DDSL:
```ddsl
when validating customer registration with email and phone and name:
    collect all errors:
        email is present otherwise "Email is required"
        email contains "@" otherwise "Invalid email format"
        email does not exist in system otherwise "Email already registered"
        name is present otherwise "Name is required"
        name has length at least 2 otherwise "Name too short"
        phone is present warning "Phone number recommended for delivery"
        name has length at most 50 warning "Name seems unusually long"
    fail if critical errors
```

## Collect Errors by Group

```
Natural Language:
When validating order with order:
Collect errors by group:
Customer errors:
- order customer ID is present, error: Customer ID required
- customer exists in system, error: Customer not found
- customer account is active, error: Customer account is inactive
Items errors:
- order items is not empty, error: Order must have items
- all items have quantity greater than 0, error: Items must have positive quantity
- all items have valid product ID, error: Items must have valid product
- count of items is at most 100, error: Too many items
Pricing errors:
- order total amount is greater than 0, error: Total amount must be positive
- order total amount is at least minimum order value, error: Order total below minimum
Fail if any errors
```

DDSL:
```ddsl
when validating order with order:
    collect errors by group:
        customerErrors:
            order customerId is present otherwise "Customer ID required"
            customer exists in system otherwise "Customer not found"
            customer account is active otherwise "Customer account is inactive"
        itemsErrors:
            order items is not empty otherwise "Order must have items"
            all items have quantity greater than 0 otherwise "Items must have positive quantity"
            all items have valid productId otherwise "Items must have valid product"
            count of items is at most 100 otherwise "Too many items"
        pricingErrors:
            order totalAmount is greater than 0 otherwise "Total amount must be positive"
            order totalAmount is at least minimumOrderValue otherwise "Order total below minimum"
    fail if any errors
```

## Collect Up To N Errors

```
Natural Language:
When validating batch import with records:
Collect up to 10 errors:
- records is not empty, error: No records to import
- all records have valid format, error: Some records have invalid format
- all records have unique identifier, error: Duplicate records found
- all records have required fields, error: Some records missing required fields
Return errors if any
```

DDSL:
```ddsl
when validating batch import with records:
    collect up to 10 errors:
        records is not empty otherwise "No records to import"
        all records have valid format otherwise "Some records have invalid format"
        all records have unique identifier otherwise "Duplicate records found"
        all records have required fields otherwise "Some records missing required fields"
    return errors if any
```

## Nested Error Collection

```
Natural Language:
When validating complex order with order:
Collect all errors:
Top-level checks:
- order is present, error: Order required
- order ID is present, error: Order ID required
Nested collection check:
- all order lines have:
  - quantity greater than 0, error: Invalid quantity in line
  - unit price greater than 0, error: Invalid price in line
  - product ID is present, error: Missing product in line
Aggregate checks:
- sum of order lines subtotals equals order total amount, error: Total amount mismatch
Fail if any errors
```

DDSL:
```ddsl
when validating complex order with order:
    collect all errors:
        order is present otherwise "Order required"
        order orderId is present otherwise "Order ID required"
        all orderLines have:
            quantity greater than 0 otherwise "Invalid quantity in line"
            unitPrice greater than 0 otherwise "Invalid price in line"
            productId is present otherwise "Missing product in line"
        sum of orderLines subtotals is equal to order totalAmount otherwise "Total amount mismatch"
    fail if any errors
```

## Error Accumulation in Use Cases

```
Natural Language:
UseCase PlaceOrder:
Input: PlaceOrderCommand with customerId and items
Output: Result of OrderId
Flow:
- Validate with error collection:
  - customerId is present, error: Customer ID required
  - items is not empty, error: Cart is empty
  - all items have quantity greater than 0, error: Invalid item quantities
  - customer exists in system, error: Customer not found
- Fail if any errors
- Create order from cart items with customer ID
- Save order to repository
- Return success with order ID
```

DDSL:
```ddsl
useCases {
    UseCase PlaceOrder {
        input: PlaceOrderCommand {
            customerId: UUID @required
            items: List<CartItem> @required
        }
        output: Result<OrderId>

        flow:
            collect all errors:
                customerId is present otherwise "Customer ID required"
                items is not empty otherwise "Cart is empty"
                all items have quantity greater than 0 otherwise "Invalid item quantities"
                customer exists in system otherwise "Customer not found"

            fail if any errors

            order created from cart items with customerId
            save order to repository
            return success with orderId
    }
}
```

## Key Transformation Rules

1. **Collect all errors**: Use `collect all errors:` block
2. **Error messages**: Use `otherwise "error message"` after condition
3. **Warnings**: Use `warning "warning message"` instead of `otherwise`
4. **By group**: Use `collect errors by group:` with named groups
5. **Limit errors**: Use `collect up to N errors:`
6. **Fail condition**: Use `fail if any errors` or `fail if critical errors`
7. **Return errors**: Use `return errors if any`
8. **Nested**: Can nest checks for collection items with `all <collection> have:`
9. **Natural format**: No bullet points, conditions flow as readable sentences
