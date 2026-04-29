---
id: ddsl-syntax-003
category: SYNTAX_RULE
subcategory: value_object
dsl_construct: ValueObject
complexity: basic
version: 3.0
---

DDSL Value Object Declaration Syntax:

A Value Object is immutable and has no identity — it is defined entirely by its attributes. Two Value Objects with the same attributes are considered equal.

Syntax:
```
ValueObject <Name> {
    <field declarations>
    <invariants>?
}
```

Key Points:
- Cannot have @identity annotation
- Should be immutable (fields don't change after creation)
- Defined by attributes, not identity
- Often used for: Money, Address, DateRange, Email, etc.
- Can have invariants (validation rules)

Example:
```ddsl
ValueObject Money {
    amount: Decimal @min(0)
    currency: String @required @maxLength(3)
}
```

Example with Invariants:
```ddsl
ValueObject DateRange {
    startDate: DateTime @required
    endDate: DateTime @required

    invariants {
        endDate must be after startDate
    }
}
```

See Also:
- nl-to-ddsl/field-declarations.md for field transformation patterns
- nl-to-ddsl/annotations-constraints.md for constraint annotations
- patterns/entity-vs-vo.md for Entity vs Value Object decision guide
