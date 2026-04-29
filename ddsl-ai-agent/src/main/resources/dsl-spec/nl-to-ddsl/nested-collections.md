---
id: nl-to-ddsl-collections-001
category: NL_TO_DDSL
category: ADVANCED_FEATURES
subcategory: nested_collections
language: mixed
complexity: advanced
version: 3.0
---

# Nested Collection Operations Transformation

## Overview

Nested collection operations enable complex queries and aggregations over nested data structures. Transform natural language collection descriptions into DDSL collection expressions.

## Aggregation with Filter

| Natural Language | DDSL |
|------------------|------|
| `sum of orders total amounts where status is Confirmed` | `sum of orders totalAmounts where status is "CONFIRMED"` |
| `count of orders where status is Active` | `count of orders where status is "ACTIVE"` |
| `maximum of orders total amounts where customer tier is Gold` | `maximum of orders totalAmounts where customerTier is "GOLD"` |
| `average of products ratings where status is Active` | `average of products ratings where status is "ACTIVE"` |

## Multi-level Navigation

| Natural Language | DDSL |
|------------------|------|
| `all order items across orders` | `all items across orders` |
| `all lines from orders where status is Confirmed` | `all lines from orders where status is "CONFIRMED"` |
| `sum of all quantities from order lines where status is Active` | `sum of all quantities from orderLines where status is "ACTIVE"` |

## Filter Chains

| Natural Language | DDSL |
|------------------|------|
| `orders where status is Confirmed and created at is within last 30 days and total amount exceeds 1000000` | `orders where status is "CONFIRMED" and createdAt is within last 30 days and totalAmount exceeds 1000000` |
| `high value confirmed orders where status is Confirmed and total amount exceeds 10000000 and customer satisfies VIP` | `orders where status is "CONFIRMED" and totalAmount exceeds 10000000 and customer satisfies VIPCustomer` |

## Group By Operations

| Natural Language | DDSL |
|------------------|------|
| `orders grouped by status` | `orders grouped by status` |
| `count of orders grouped by status` | `count of orders grouped by status` |
| `revenue by category as sum of products price grouped by category` | `revenueByCategory as sum of products price grouped by category` |
| `orders grouped by customer tier` | `orders grouped by customerTier` |

## Complete Examples

### Example 1: Basic Aggregations
```
Natural Language:
Calculate:
- confirmed total as sum of orders total amounts where status is Confirmed
- active item count as count of orders where status is Active
- max order value as maximum of orders total amounts where customer tier is Gold
- avg rating as average of products ratings where status is Active
```

DDSL:
```ddsl
given:
    confirmedTotal as sum of orders totalAmounts where status is "CONFIRMED"
    activeItemCount as count of orders where status is "ACTIVE"
    maxOrderValue as maximum of orders totalAmounts where customerTier is "GOLD"
    avgRating as average of products ratings where status is "ACTIVE"
```

### Example 2: Multi-level Nested
```
Natural Language:
Calculate:
- all order items as all items across orders
- confirmed order lines as all lines from orders where status is Confirmed
- VIP confirmed total as sum of VIP customer orders total amounts where status is Confirmed
- pending items count as count of items across orders where order status is Pending
- total quantity as sum of all quantities from order lines where status is Active
```

DDSL:
```ddsl
given:
    allOrderItems as all items across orders
    confirmedOrderLines as all lines from orders where status is "CONFIRMED"
    vipConfirmedTotal as sum of vipCustomerOrders totalAmounts where status is "CONFIRMED"
    pendingItemsCount as count of items across orders where order status is "PENDING"
    totalQuantity as sum of all quantities from orderLines where status is "ACTIVE"
```

### Example 3: Filter Chains
```
Natural Language:
Calculate:
- eligible orders as orders where status is Confirmed and created at is within last 30 days and total amount exceeds 1000000
- high value confirmed orders as orders where status is Confirmed and total amount exceeds 10000000 and customer satisfies VIP
```

DDSL:
```ddsl
given:
    eligibleOrders as orders where status is "CONFIRMED" and createdAt is within last 30 days and totalAmount exceeds 1000000
    highValueConfirmedOrders as orders where status is "CONFIRMED" and totalAmount exceeds 10000000 and customer satisfies VIPCustomer
```

### Example 4: Group By
```
Natural Language:
Calculate:
- orders by status as orders grouped by status
- order count by status as count of orders grouped by status
- revenue by category as sum of products price grouped by category
- orders grouped by customer tier
- pending count as count of orders where status is Pending
- confirmed revenue as sum of orders total amounts where status is Confirmed
```

DDSL:
```ddsl
given:
    ordersByStatus as orders grouped by status
    orderCountByStatus as count of orders grouped by status
    revenueByCategory as sum of products price grouped by category
    ordersByTier as orders grouped by customerTier
    pendingCount as count of orders where status is "PENDING"
    confirmedRevenue as sum of orders totalAmounts where status is "CONFIRMED"
```

### Example 5: Domain Service Analytics
```
Natural Language:
DomainService OrderAnalyticsService:

When calculating customer lifetime value for customer:
- all customer orders as orders where customer ID equals customer ID
- completed orders as all customer orders where status is Delivered
- total spent as sum of completed orders total amounts
- average order value as average of completed orders total amounts
- last order date as maximum of completed orders created at
- order count as count of completed orders
- all purchased products as all product IDs across completed orders order lines
- unique product count as count of all purchased products
- return customer lifetime value with total spent, average order value, order count, unique product count, last order date

When generating monthly report for month and year:
- monthly orders as orders where created at is within month of year
- revenue by category as sum of monthly orders total amounts grouped by primary category
- top customers as monthly orders where total amount exceeds 10000000 grouped by customer ID
- daily order counts as count of monthly orders grouped by created at date
- return monthly report with total revenue, order count, revenue by category, top customers
```

DDSL:
```ddsl
DomainService OrderAnalyticsService {
    when calculating customer lifetime value for customer:
        given:
            allCustomerOrders as orders where customerId is equal to customer
            completedOrders as allCustomerOrders where status is "DELIVERED"
            totalSpent as sum of completedOrders totalAmounts
            averageOrderValue as average of completedOrders totalAmounts
            lastOrderDate as maximum of completedOrders createdAt
            orderCount as count of completedOrders
            allPurchasedProducts as all productIds across completedOrders orderLines
            uniqueProductCount as count of allPurchasedProducts

        return customerLifetimeValue with:
            totalSpent set to totalSpent
            averageOrderValue set to averageOrderValue
            orderCount set to orderCount
            uniqueProductCount set to uniqueProductCount
            lastOrderDate set to lastOrderDate

    when generating monthly report for month and year:
        given:
            monthlyOrders as orders where createdAt is within month of year
            revenueByCategory as sum of monthlyOrders totalAmounts grouped by primaryCategory
            topCustomers as monthlyOrders where totalAmount exceeds 10000000 grouped by customerId
            dailyOrderCounts as count of monthlyOrders grouped by createdAt date

        return monthlyReport with:
            totalRevenue set to sum of monthlyOrders totalAmounts
            orderCount set to count of monthlyOrders
            revenueByCategory set to revenueByCategory
            topCustomers set to topCustomers
}
```

### Example 6: Nested in Invariants
```
Natural Language:
ShoppingCart Aggregate:
- id: CartId
- items: List of CartItem

Invariants:
- Cart value must match items: total value equals sum of items unit prices times quantities
- No duplicate products: count of items grouped by product ID has all counts equal to 1
```

DDSL:
```ddsl
Aggregate ShoppingCart {
    cartId: UUID @identity
    items: List<CartItem>

    invariants {
        "Cart value must match items": totalValue is equal to sum of items unitPrices times quantities
        "No duplicate products": count of items grouped by productId has all counts equal to 1
    }
}
```

### Example 7: Nested in Specifications
```
Natural Language:
Specifications:
- HighVolumeCustomers: customers where sum of customer orders total amounts where status is Delivered is greater than 100000000
- ActiveProductCategories: categories where count of products where category ID equals category ID and status is Active is greater than 0
- ProfitableOrders: orders where sum of order lines subtotals where product margin is greater than 20 percent is greater than order total amount times 0.5
```

DDSL:
```ddsl
specifications {
    Specification HighVolumeCustomers {
        matches customers where:
            sum of customer orders totalAmounts where status is "DELIVERED" is greater than 100000000
    }

    Specification ActiveProductCategories {
        matches categories where:
            count of products where categoryId is equal to categoryId and status is "ACTIVE" is greater than 0
    }

    Specification ProfitableOrders {
        matches orders where:
            sum of orderLines subtotals where product margin is greater than 20 percent is greater than order totalAmount times 0.5
    }
}
```

## Key Transformation Rules

1. **Aggregation functions**: `sum of`, `count of`, `maximum of`, `minimum of`, `average of`
2. **Navigation**: Use `all <field> across <collection>` or `all <field> from <collection>`
3. **Filtering**: Add `where <condition>` after the collection
4. **Multiple filters**: Chain with `and`
5. **Grouping**: Use `grouped by <field>`
6. **Field paths**: Use dot notation for nested fields (`orderLines subtotals`)
7. **Comparisons**: Use `is equal to`, `is greater than`, `is less than`
8. **Specifications**: Can use `satisfies <Specification>` in where clauses
