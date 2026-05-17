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

Parser Requirements:
- The first token of each invariant must be a quoted string message.
- The quoted message must be followed by `:`.
- Do not write bare natural-language invariant lines such as `balance must not be negative`.
- Prefer simple parser-compatible expressions such as `balance >= 0`, `totalAmount is greater than 0`, or `items is not empty`.

Common Invariant Patterns:
- Field must not be empty: "items cannot be empty": items is not empty
- Field must be positive: "total must be positive": total is greater than 0
- Field comparison: "end must be after start": endDate is greater than startDate
- Collection size: "must have at least one item": count of items is at least 1
- Status validation: use field constraints or operation preconditions when enum/list syntax is uncertain.

Examples:
```ddsl
Aggregate Order {
    items: List<OrderItem>
    totalAmount: Decimal
    status: String
    
    invariants {
        "Order must have items": items is not empty
        "Total must be positive": totalAmount is greater than 0
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
