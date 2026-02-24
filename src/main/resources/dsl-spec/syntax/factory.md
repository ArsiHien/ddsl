---
id: ddsl-syntax-009
category: SYNTAX_RULE
subcategory: factory
dsl_construct: Factory
complexity: intermediate
---
DDSL Factory Syntax:
Factories encapsulate complex object creation logic.
Syntax: Factory <Name> for <Type> { creating <Type> from <params> { <steps> } }
Example:
factories {
    Factory OrderFactory for Order {
        creating Order from customer, items {
            create Order with new UUID, customer, items
            set status to "PLACED"
            calculate totalAmount from items
            return the created order
        }
    }
}
