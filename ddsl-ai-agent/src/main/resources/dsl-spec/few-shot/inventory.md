---
id: ddsl-fewshot-003
category: FEW_SHOT
subcategory: informal_types_and_constraints
language: en
complexity: basic
version: 3.0
---

This example shows how to translate very informal type names, constraints, and shorthand descriptions into valid DDSL using the natural format.

User input (heavily informal types and constraints):
```
BoundedContext Inventory {
    domain {
        Aggregate Product {
            product id is UUID for identity
            name is text, mandatory, up to 200 characters
            sku is text, mandatory, format "AAA-000000"
            price is a number with decimals, cannot be negative
            stock is a whole number, no negatives
            category is text, required
            tags is a collection of text
            metadata is a key-value map of text to text
            discontinued is yes/no flag
            last updated is optional timestamp

            invariants {
                stock should never go negative
                price has to be positive
            }

            operations {
                adding stock with quantity {
                    quantity must be positive, then increase stock by quantity amount
                }
                removing stock with quantity {
                    quantity must be positive and stock must have enough, decrease stock by quantity, fire StockLow alert with productId and remaining stock
                }
            }
        }
    }
    events {
        DomainEvent StockLow {
            product id is UUID
            remaining stock is a whole number
            occurred at is timestamp
        }
    }
    repositories {
        Repository ProductRepository for Product {
            findById(id: UUID): Product?
            findBySku(sku: String): Product?
            findByCategory(category: String): List<Product>
            save(product: Product): Void
        }
    }
}
```

Corrected DDSL output:
```ddsl
BoundedContext Inventory {
    domain {
        Aggregate Product {
            productId: UUID @identity
            name: String @required @maxLength(200)
            sku: String @required @pattern("[A-Z]{3}-[0-9]{6}")
            price: Decimal @min(0)
            stock: Int @min(0)
            category: String @required
            tags: List<String>
            metadata: Map<String, String>
            discontinued: Boolean
            lastUpdated: DateTime?

            invariants {
                stock should never go negative
                price has to be positive
            }

            operations {
                when adding stock with quantity:
                    require that quantity is greater than 0
                    set stock to stock plus quantity

                when removing stock with quantity:
                    require that quantity is greater than 0 and stock is at least quantity
                    set stock to stock minus quantity
                    emit StockLow with productId and stock
            }
        }
    }
    events {
        DomainEvent StockLow {
            productId: UUID
            remainingStock: Int
            occurredAt: DateTime
        }
    }
    repositories {
        Repository ProductRepository for Product {
            findById(id: UUID): Product?
            findBySku(sku: String): Product?
            findByCategory(category: String): List<Product>
            save(product: Product): Void
        }
    }
}
```

Key transformations:
- `a number with decimals` → `Decimal` — informal numeric description to canonical type
- `a whole number` → `Int` — "whole number" means integer
- `yes/no flag` → `Boolean`
- `a collection of text` → `List<String>` — "collection of"→List<>, "text"→String
- `a key-value map of text to text` → `Map<String, String>` — natural description→generic syntax
- `optional timestamp` → `DateTime?` — "optional"→nullable ?, "timestamp"→DateTime
- `up to 200 characters` → `@maxLength(200)` — natural constraint→formal
- `format "AAA-000000"` → `@pattern("[A-Z]{3}-[0-9]{6}")` — format description→regex pattern
- `cannot be negative` / `no negatives` → `@min(0)`
- `mandatory` → `@required`
- Compressed `quantity must be positive and stock must have enough, decrease stock by quantity, fire StockLow alert...` → natural format: `require that quantity is greater than 0 and stock is at least quantity` (combined with "and"), then `set stock to stock minus quantity`, then `emit StockLow with productId and stock`
- `fire StockLow alert with productId and remaining stock` → `emit StockLow with productId and stock` — "fire"→"emit", "alert"→"event"
- `increase stock by quantity amount` → `set stock to stock plus quantity`
- `decrease stock by quantity` → `set stock to stock minus quantity`
- **Natural format**: Combined requires with "and", no bullet points, actions as readable sentences
