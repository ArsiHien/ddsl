---
id: ddsl-constraint-001
category: CONSTRAINT
subcategory: annotations
complexity: basic
---
DDSL Constraint Annotations Reference:
@required     — field must not be null/empty
@min(n)       — numeric minimum value
@max(n)       — numeric maximum value
@minLength(n) — string minimum length
@maxLength(n) — string maximum length
@size(min,max)— collection size bounds
@pattern("r") — string must match regex
@email        — string must be valid email format
@identity     — marks the identity field of an Entity
@immutable    — field cannot be changed after creation
@computed     — field is derived, not directly set
Multiple constraints can be combined: name: String @required @minLength(2) @maxLength(100)
