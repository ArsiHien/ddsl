---
id: ddsl-constraint-002
category: CONSTRAINT
subcategory: invariants
complexity: intermediate
version: 3.0
---

DDSL Invariant Expressions Reference:

Invariants are business rules that must always be true for a domain object. They are defined in an 'invariants { ... }' block and checked automatically.

Syntax:
```
invariants {
    "<description>": <condition>
}
```

Common Invariant Patterns:
- Field must not be empty: "items cannot be empty": items is not empty
- Field must be positive: "total must be positive": total is greater than 0
- Field comparison: "end must be after start": endDate is greater than startDate
- Collection size: "must have at least one item": count of items is at least 1
- Enum validation: "status must be valid": status is one of ["PENDING", "CONFIRMED"]

Examples:
```ddsl
Aggregate Order {
    items: List<OrderItem>
    totalAmount: Decimal
    status: String
    
    invariants {
        "Order must have items": items is not empty
        "Total must be positive": totalAmount is greater than 0
        "Status must be valid": status is one of ["PENDING", "CONFIRMED", "SHIPPED"]
    }
}
```

```ddsl
ValueObject DateRange {
    startDate: DateTime
    endDate: DateTime
    
    invariants {
        "End date must be after start date": endDate is greater than startDate
    }
}
```

Comparison Operators in Invariants:
- is equal to / is
- is not equal to / is not
- is greater than / exceeds
- is less than
- is at least / is greater than or equal to
- is at most / is less than or equal to
- is empty / is not empty

See Also:
- nl-to-ddsl/annotations-constraints.md for field-level constraints
- nl-to-ddsl/behavior-patterns.md for require clauses in behaviors
