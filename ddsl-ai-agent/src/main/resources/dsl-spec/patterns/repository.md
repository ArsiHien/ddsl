---
id: ddsl-pattern-004
category: DDD_PATTERN
subcategory: repository
complexity: basic
---
DDD Pattern: Repository Interface Design
Standard methods for a Repository:
- findById(id: UUID): T?        — returns nullable (may not exist)
- findAll(): List<T>             — returns all entities
- findBy<Field>(value): List<T>  — query by field
- save(entity: T): Void          — persist (create or update)
- delete(entity: T): Void        — remove
- exists(id: UUID): Boolean       — check existence
- count(): Long                   — total count
Repositories only exist for Aggregate Roots, never for internal Entities.
