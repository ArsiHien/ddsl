---
id: ddsl-pattern-001
category: DDD_PATTERN
subcategory: aggregate
complexity: intermediate
---
DDD Pattern: Aggregate Design Rules
1. Every Aggregate must have exactly one root Entity
2. The root Entity must have an @identity field
3. External references should only reference the Aggregate root, never internal entities
4. Aggregates define transactional boundaries
5. Invariants within an Aggregate are always consistent
6. Aggregates communicate via Domain Events
