---
id: ddsl-syntax-009
category: SYNTAX_RULE
subcategory: factory
dsl_construct: Factory
complexity: intermediate
version: 3.0
---

DDSL Factory Syntax:

Factories encapsulate complex object creation logic. They separate the creation process from the domain objects themselves, keeping domain classes focused on business logic.

Syntax:
```
factories {
    Factory <Name> for <Type> {
        when creating <Type> from <Source> [with <params>]:
            <creation steps>
            return <Type> with <initializations>
    }
}
```

Key Points:
- Encapsulates complex creation logic
- Validates preconditions before creation
- Can calculate derived values during creation
- Returns fully initialized objects
- Often used when creation involves multiple steps or complex rules

Example:
```ddsl
factories {
    Factory OrderFactory for Order {
        when creating Order from Cart with customerId:
            require that cart is not empty and customerId is not empty
            create order with new orderId and customerId
            set order status to "PENDING"
            calculate order total from cart items
            return order
    }
}
```

Alternative Return Syntax:
```ddsl
factories {
    Factory OrderFactory {
        when creating Order from Cart:
            require that cart is not empty
            return Order with:
                orderId set to new UUID
                status set to "PENDING"
                total calculated from cart
    }
}
```

See Also:
- nl-to-ddsl/behavior-patterns.md for natural behavior format
- nl-to-ddsl/complete-examples.md for full factory examples
