---
id: nl-to-ddsl-advanced-001
category: NL_TO_DDSL
category: ADVANCED_FEATURES
subcategory: overview
language: mixed
complexity: advanced
version: 3.0
---

# Advanced DDSL Features

## Overview

This documentation covers advanced DDSL features beyond basic aggregates, entities, and behaviors. These features enable more expressive domain modeling for complex scenarios.

## Feature Categories

### 1. Temporal Logic
Time-based conditions for behaviors and specifications.
- Date comparisons (is before, is after)
- Relative time (is more than N days ago)
- Ranges (is within next N days)
- Duration expressions

**See**: temporal-logic.md

### 2. State Machines
Define state transitions with guards and entry/exit actions.
- State declarations (initial, final)
- Transitions with conditions
- Guards (cannot transition, must transition)
- On entry/On exit actions

**See**: state-machine.md

### 3. Error Accumulation
Collect multiple validation errors at once.
- Collect all errors
- Collect by group
- Error vs warning distinction

**See**: error-accumulation.md

### 4. Match Expressions
Pattern matching for conditional logic.
- Match on values
- Guarded cases
- Default cases
- Match as expression

**See**: match-expressions.md

### 5. String Operations
Advanced string validation and transformation.
- Content checks (contains, starts with)
- Pattern matching
- Length checks
- Format validation (email, URL)
- Transformations (uppercase, trim)

**See**: string-operations.md

### 6. Nested Collection Operations
Complex queries over nested collections.
- Aggregations with filters
- Multi-level navigation
- Filter chains
- Group by operations

**See**: nested-collections.md

### 7. Specifications in Conditions
Use specifications within behaviors.
- satisfies operator
- Composite specifications (and, or, not)
- Parameterized specifications

**See**: complete-examples.md (Specification examples)

## Quick Reference

| Feature | Use Case | Key Syntax |
|---------|----------|------------|
| Temporal Logic | Time-based validation | `is more than 24 hours ago` |
| State Machine | Entity lifecycle states | `state machine for status` |
| Error Accumulation | Form validation | `collect all errors` |
| Match Expression | Conditional logic | `match X with: Case: action` |
| String Operations | Text validation | `contains`, `matches` |
| Nested Collections | Complex queries | `sum of X where condition` |

## Complete Documentation Files

Advanced Features (7 files):
1. advanced-features-overview.md (this file)
2. temporal-logic.md
3. state-machine.md
4. error-accumulation.md
5. match-expressions.md
6. string-operations.md
7. nested-collections.md

Core Documentation (8 files):
1. index.md
2. quick-reference.md
3. core-patterns.md
4. field-declarations.md
5. behavior-patterns.md
6. annotations-constraints.md
7. type-mapping.md
8. complete-examples.md

Total: 15 comprehensive documentation files for natural language to DDSL transformation
