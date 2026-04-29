---
id: ddsl-syntax-010
category: SYNTAX_RULE
subcategory: specification
dsl_construct: Specification
complexity: intermediate
version: 3.0
---

DDSL Specification Syntax:

Specifications express domain predicates for querying and validation. They encapsulate business rules in a reusable, composable way.

Syntax:
```
specifications {
    Specification <Name> [for <Type>] {
        matches <Type> where:
            <condition>
    }
    
    Specification <Name> given <params> {
        matches <Type> where:
            <condition>
    }
}
```

Key Points:
- Encapsulates query criteria as reusable objects
- Can be combined (AND, OR, NOT)
- Used by repositories for complex queries
- Named after the business concept they represent
- Can accept parameters for dynamic specifications

Example - Simple Specification:
```ddsl
specifications {
    Specification ActiveOrders {
        matches orders where:
            status is not "DELIVERED" and status is not "CANCELLED"
    }
    
    Specification HighValueOrders {
        matches orders where:
            totalAmount is greater than 1000
    }
}
```

Example - Parameterized Specification:
```ddsl
specifications {
    Specification OrdersForCustomer given customerId {
        matches orders where:
            customerId is customerId
    }
    
    Specification RecentOrders given days {
        matches orders where:
            createdAt is within last days
    }
}
```

Example - Using Specifications in Behaviors:
```ddsl
Aggregate Order {
    operations {
        when processing order:
            require that order satisfies ActiveOrders
            set status to "PROCESSING"
    }
}
```

See Also:
- nl-to-ddsl/complete-examples.md for specification examples
- nl-to-ddsl/behavior-patterns.md for using specifications in behaviors
