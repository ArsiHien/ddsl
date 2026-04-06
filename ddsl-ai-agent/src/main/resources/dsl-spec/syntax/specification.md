---
id: ddsl-syntax-010
category: SYNTAX_RULE
subcategory: specification
dsl_construct: Specification
complexity: intermediate
---
DDSL Specification Syntax:
Specifications express domain predicates for querying and validation.
Syntax: Specification <Name> for <Type> { matches when <condition> }
Example:
specifications {
    Specification ActiveOrder for Order {
        matches when order status is not "DELIVERED" and not "CANCELLED"
    }
    Specification HighValueOrder for Order {
        matches when order totalAmount is greater than 1000
    }
}
