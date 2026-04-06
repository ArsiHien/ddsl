---
id: ddsl-syntax-003
category: SYNTAX_RULE
subcategory: value_object
dsl_construct: ValueObject
complexity: basic
---
DDSL Value Object Declaration Syntax:
A Value Object is immutable and has no identity — defined by its attributes.
Syntax: ValueObject <Name> { <fields> <invariants>? }
Should have at least one field. Cannot have @identity.
Example:
ValueObject Money {
    amount: Decimal @min(0)
    currency: String @maxLength(3)
}
