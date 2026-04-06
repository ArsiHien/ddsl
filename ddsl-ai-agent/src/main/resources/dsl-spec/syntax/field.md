---
id: ddsl-syntax-004
category: SYNTAX_RULE
subcategory: field
dsl_construct: Field
complexity: basic
---
DDSL Field Declaration Syntax:
Fields define the attributes of Aggregates, Entities, and Value Objects.
Syntax: [annotations] <name>: <TypeRef> [constraints]
Supported types: String, Int, Long, Decimal, Boolean, DateTime, Date, UUID, Money, Void
Collection types: List<T>, Set<T>, Map<K,V>
Nullable: append ? to type (e.g. String?)
Annotations: @identity, @required, @immutable, @computed
Constraints: @min(n), @max(n), @minLength(n), @maxLength(n), @pattern("regex"), @email
Example:
@identity orderId: UUID
name: String @required @maxLength(200)
price: Decimal @min(0)
email: String? @email
tags: List<String>
