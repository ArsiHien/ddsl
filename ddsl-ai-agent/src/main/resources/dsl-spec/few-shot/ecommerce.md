---
id: ddsl-fewshot-001
category: FEW_SHOT
subcategory: informal_field_declarations
language: en
complexity: intermediate
version: 3.0
---

This example shows how to translate very informal, natural-language-style field declarations and behaviors into valid DDSL syntax using the natural format.

User input (highly informal pseudocode):
```
BoundedContext ECommerce {
    domain {
        Aggregate Order {
            order id is UUID for identity
            customer name is text, required
            items is a list of OrderItem
            total amount as money
            status is text
            created at is a timestamp

            invariants {
                items cannot be empty
                total must be positive
            }

            operations {
                placing an order with customer and items {
                    items must not be empty
                    change status to "PLACED"
                    calculate total from item prices
                    publish event OrderPlaced containing orderId and customer
                }
            }
        }

        Entity OrderItem {
            item id is UUID for identity
            product name is text, mandatory, max 200 chars
            quantity is a number, at least 1
            unit price is decimal, minimum 0
        }

        ValueObject Money {
            amount is decimal, no negatives
            currency code is text, max 3 characters
        }
    }
    events {
        DomainEvent OrderPlaced {
            order id is UUID
            customer is text
            order date is timestamp
        }
    }
    repositories {
        Repository OrderRepository for Order {
            findById(id: UUID): Order?
            save(order: Order): Void
        }
    }
}
```

Corrected DDSL output:
```ddsl
BoundedContext ECommerce {
    domain {
        Aggregate Order {
            orderId: UUID @identity
            customerName: String @required
            items: List<OrderItem>
            totalAmount: Money
            status: String
            createdAt: DateTime

            invariants {
                items cannot be empty
                total must be positive
            }

            operations {
                when placing order with customer and items:
                    require that items is not empty
                    set status to "PLACED"
                    calculate total from item prices
                    emit OrderPlaced with orderId and customer
            }
        }

        Entity OrderItem {
            itemId: UUID @identity
            productName: String @required @maxLength(200)
            quantity: Int @min(1)
            unitPrice: Decimal @min(0)
        }

        ValueObject Money {
            amount: Decimal @min(0)
            currencyCode: String @maxLength(3)
        }
    }
    events {
        DomainEvent OrderPlaced {
            orderId: UUID
            customer: String
            orderDate: DateTime
        }
    }
    repositories {
        Repository OrderRepository for Order {
            findById(id: UUID): Order?
            save(order: Order): Void
        }
    }
}
```

Key transformations:
- `order id is UUID for identity` → `orderId: UUID @identity` — spaces→camelCase, "is"→colon, "for identity"→@identity annotation
- `customer name is text, required` → `customerName: String @required` — spaces→camelCase, text→String, required→@required
- `items is a list of OrderItem` → `items: List<OrderItem>` — "is a list of"→List<>
- `total amount as money` → `totalAmount: Money` — "as"→colon, spaces→camelCase
- `created at is a timestamp` → `createdAt: DateTime` — timestamp→DateTime
- `product name is text, mandatory, max 200 chars` → `productName: String @required @maxLength(200)` — mandatory→@required, "max 200 chars"→@maxLength(200)
- `quantity is a number, at least 1` → `quantity: Int @min(1)` — "a number"→Int, "at least 1"→@min(1)
- `amount is decimal, no negatives` → `amount: Decimal @min(0)` — "no negatives"→@min(0)
- `placing an order with customer and items {` → `when placing order with customer and items:` — add `when`, convert to natural format
- `items must not be empty` → `require that items is not empty` — wrap in require that
- `change status to "PLACED"` → `set status to "PLACED"` — "change"→"set"
- `calculate total from item prices` → `calculate total from item prices` — keep as is
- `publish event OrderPlaced containing orderId and customer` → `emit OrderPlaced with orderId and customer` — "publish"→"emit", "containing"→"with"
- **Natural format**: No bullet points, actions flow as readable sentences
