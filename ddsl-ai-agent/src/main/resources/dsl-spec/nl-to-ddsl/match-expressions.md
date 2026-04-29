---
id: nl-to-ddsl-match-001
category: NL_TO_DDSL
category: ADVANCED_FEATURES
subcategory: match_expressions
language: mixed
complexity: advanced
version: 3.0
---

# Match Expression Transformation

## Overview

Match expressions provide pattern matching for conditional logic. Transform natural language conditional descriptions into DDSL match expressions.

## Match as Statement

### Basic Match
| Natural Language | DDSL |
|------------------|------|
| `if customer tier is Gold, apply 15% discount` | `match customerTier with: Gold: apply 15% discount` |
| `if Silver, apply 10% discount` | `Silver: apply 10% discount` |
| `otherwise no discount` | `default: no discount applied` |

### Complete Example
```
Natural Language:
Match customer tier:
- Gold: apply 15% discount, enable free shipping, assign dedicated support
- Silver: apply 10% discount, enable free shipping
- Bronze: apply 5% discount
- default: no discount
```

DDSL:
```ddsl
match customerTier with:
    Gold:
        apply 15% discount
        enable free shipping
        assign dedicated support
    Silver:
        apply 10% discount
        enable free shipping
    Bronze:
        apply 5% discount
    default:
        no discount applied
```

## Match with Multiple Values

```
Natural Language:
Match order status:
- Pending or Confirmed: mark order as modifiable
- Shipped or Delivered: mark order as read only
- Cancelled: mark order as archived
```

DDSL:
```ddsl
match orderStatus with:
    [Pending, Confirmed]:
        mark order as modifiable
    [Shipped, Delivered]:
        mark order as read only
    Cancelled:
        mark order as archived
    default:
        mark order as unknown state
```

## Match with Guarded Cases

```
Natural Language:
Match customer tier with conditions:
- Gold when total > 50M: apply 20% discount, enable VIP shipping
- Gold: apply 15% discount, enable free shipping
- Silver when total > 20M: apply 12% discount
- Silver: apply 10% discount
- default: apply 5% discount
```

DDSL:
```ddsl
match customerTier with:
    Gold when totalAmount exceeds 50000000:
        apply 20% discount
        enable VIP express shipping
    Gold:
        apply 15% discount
        enable free shipping
    Silver when totalAmount exceeds 20000000:
        apply 12% discount
    Silver:
        apply 10% discount
    default:
        apply 5% discount
```

## Match as Expression (in Given)

### Example 1: Discount Rate
```
Natural Language:
Set discount rate by matching customer tier:
- Gold: 0.15
- Silver: 0.10
- Bronze: 0.05
- default: 0.00
```

DDSL:
```ddsl
given:
    discountRate as match customerTier with:
        Gold: 0.15
        Silver: 0.10
        Bronze: 0.05
        default: 0.00
```

### Example 2: Shipping Cost
```
Natural Language:
Set shipping cost by matching method:
- Express: 50000
- Standard: 20000
- Economy: 10000
- default: 30000
```

DDSL:
```ddsl
given:
    shippingCost as match shippingMethod with:
        Express: 50000
        Standard: 20000
        Economy: 10000
        default: 30000
```

### Example 3: Priority by Amount
```
Natural Language:
Set priority by total amount:
- when amount > 10M: High
- when amount > 5M: Medium
- default: Normal
```

DDSL:
```ddsl
given:
    priority as match totalAmount with:
        amount when amount exceeds 10000000: High
        amount when amount exceeds 5000000: Medium
        default: Normal
```

## Match with Type Patterns

```
Natural Language:
Match payment method:
- CreditCard: apply 1.5% fee, validate expiry
- BankTransfer: generate reference, set deadline to 24 hours from now
- Wallet: verify balance, deduct from wallet
- default: reject unsupported
```

DDSL:
```ddsl
match paymentMethod with:
    CreditCard:
        apply 1.5% processing fee
        validate card expiry date
    BankTransfer:
        generate transfer reference
        set payment deadline to 24 hours from now
    Wallet:
        verify wallet balance exceeds total amount
        deduct from wallet balance
    default:
        reject unsupported payment method
```

## Nested Match

```
Natural Language:
Match order type:
- Subscription:
  - Match billing cycle:
    - Monthly: set next billing 30 days from now
    - Yearly: set next billing 365 days from now, apply yearly discount
    - default: set next billing 30 days from now
- OneTime: set priority to Normal
- default: set priority to Low
```

DDSL:
```ddsl
match orderType with:
    Subscription:
        match billingCycle with:
            Monthly:
                set next billing as 30 days from now
            Yearly:
                set next billing as 365 days from now
                apply yearly discount
            default:
                set next billing as 30 days from now
    OneTime:
        set fulfillment priority to Normal
    default:
        set fulfillment priority to Low
```

## Match in Specifications

```ddsl
specifications {
    Specification OrdersWithDiscount {
        matches orders where:
            match order customerTier with:
                [Gold, Silver]: order has discount applied
                default: false
    }
}
```

## Match in Domain Service

```ddsl
DomainService ShippingCalculationService {
    when calculating shipping cost with order and shippingMethod:
        given:
            baseCost as match shippingMethod with:
                Express: 50000
                Standard: 20000
                Economy: 10000
                default: 30000

            weightFactor as match order weightCategory with:
                Heavy when order totalWeight exceeds 10: 2.0
                Medium when order totalWeight exceeds 5: 1.5
                default: 1.0

            finalCost as baseCost times weightFactor

        return finalCost
}
```

## Key Transformation Rules

1. **Match keyword**: Always start with `match <expression> with:`
2. **Cases**: List each case on new line with value followed by colon
3. **Multiple values**: Use array syntax `[Value1, Value2]`
4. **Guards**: Add `when <condition>` after case value
5. **Default**: Always include `default:` case
6. **Body**: Indent actions under each case
7. **As expression**: Use `as match ... with:` for assignment
8. **Return value**: For match as expression, use direct value after colon
