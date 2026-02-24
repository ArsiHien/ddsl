---
id: ddsl-constraint-002
category: CONSTRAINT
subcategory: invariants
complexity: intermediate
---
DDSL Invariant Expressions Reference:
Inside 'invariants { ... }' block, express rules in natural language:
- "<field> must not be empty"
- "<field> must not be negative"
- "<field> must be greater than <value>"
- "<field> must be less than <value>"
- "<field> must be one of <value1>, <value2>, ..."
- "<field> must be valid <format>"
- "<field> must match <pattern>"
These are checked at the domain model level and generate validation code.
