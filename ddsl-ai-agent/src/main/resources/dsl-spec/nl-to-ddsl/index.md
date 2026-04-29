---
id: nl-to-ddsl-index-001
category: NL_TO_DDSL
category: OVERVIEW
subcategory: documentation_index
language: mixed
complexity: basic
version: 3.0
---

# Natural Language to DDSL Transformation - Documentation Index

## Purpose

This documentation set helps Large Language Models (LLMs) transform natural language descriptions of domain models into valid DDSL (Domain-Driven Specification Language) syntax.

## Core Principle

**Transform, don't invent.** Convert the natural language input to proper DDSL format without adding new entities, fields, or behaviors that weren't explicitly described.

## Documentation Structure

### 1. Core Patterns (`core-patterns.md`)
- Universal transformation rules
- Name normalization (to camelCase)
- Field declaration patterns
- Identity and required field patterns
- Structure preservation guidelines

### 2. Field Declarations (`field-declarations.md`)
- Name transformation (spaces → camelCase)
- Vietnamese to English field name mapping
- Type mapping (text → String, number → Int/Decimal, etc.)
- Complete field transformation examples

### 3. Behavior Patterns (`behavior-patterns.md`)
- Natural behavior structure
- When clause transformation
- Combined require clauses (using "and")
- Natural action descriptions without bullets
- Emit clause transformation
- Complete behavior examples

### 4. Annotations & Constraints (`annotations-constraints.md`)
- @identity annotation patterns
- @required annotation patterns
- @min, @max for numeric constraints
- @minLength, @maxLength for strings
- @unique, @email, @pattern, @default
- Combining multiple annotations

### 5. Type Mapping (`type-mapping.md`)
- Primitive type mappings (String, Int, Decimal, Boolean, etc.)
- Collection types (List, Set, Map)
- Nullable types (Type?)
- Custom types and Value Objects
- Type inference rules

### 6. Complete Examples (`complete-examples.md`)
- Hotel reservation system
- E-commerce order system
- Library management system
- Vietnamese input example (food delivery)
- Product catalog with specifications

### 7. Advanced Features

#### Temporal Logic (`temporal-logic.md`)
- Date comparisons (is before, is after)
- Relative time expressions (ago, from now)
- Time ranges (within, between)
- Event ordering

#### State Machine (`state-machine.md`)
- State declarations (initial, final)
- Transitions with conditions
- Guards (cannot/must transition)
- On entry/On exit actions

#### Error Accumulation (`error-accumulation.md`)
- Collect all errors
- Collect by group
- Error vs warning distinction
- Nested validation

#### Match Expressions (`match-expressions.md`)
- Pattern matching on values
- Guarded cases
- Match as expression
- Nested match

#### String Operations (`string-operations.md`)
- Content checks (contains, starts with)
- Pattern matching with regex
- Length validation
- Format validation (email, URL)
- String transformations

#### Nested Collections (`nested-collections.md`)
- Aggregations with filters
- Multi-level navigation
- Filter chains
- Group by operations

## Quick Start

### Basic Transformation Flow

1. **Parse natural language** to identify:
   - Bounded Context name
   - Aggregates, Entities, Value Objects
   - Fields with types and constraints
   - Behaviors (when/require/then/emit)
   - Events

2. **Normalize names** to camelCase:
   - `order id` → `orderId`
   - `customer name` → `customerName`
   - `created at` → `createdAt`

3. **Map types** to DDSL:
   - `text` → `String`
   - `number` → `Int` or `Decimal`
   - `true/false` → `Boolean`
   - `date time` → `DateTime`
   - `UUID` → `UUID`

4. **Convert behaviors** to natural DDSL format:
   ```
   when action with params:
       require that condition and condition
       set field to value
       calculate value as expression
       emit Event with props
   ```

## Common Transformation Patterns

### Field Declaration

| Natural Language | DDSL |
|------------------|------|
| `X is UUID and identity` | `X: UUID @identity` |
| `X is text and required` | `X: String @required` |
| `X is decimal, minimum 0` | `X: Decimal @min(0)` |
| `X is list of Item` | `X: List<Item>` |
| `X is optional` | `X: Type?` |

### Behavior Structure

| Natural Language | DDSL |
|------------------|------|
| `When placing order with X and Y` | `when placing order with X and Y` |
| `require A and B` | `require that A and B` |
| `set status to CONFIRMED` | `set status to "CONFIRMED"` |
| `emit Event with ID` | `emit Event with id` |

## Important Reminders

1. **Do not add entities** that weren't mentioned in the input
2. **Do not add fields** that weren't described
3. **Do not expand behaviors** beyond what was specified
4. **Use camelCase** for all identifiers
5. **Preserve the exact meaning** while changing format only
6. **Combine related requires** using "and"
7. **Write actions naturally** without bullet points

## File Locations

All documentation files are in:
```
ddsl-ai-agent/src/main/resources/dsl-spec/nl-to-ddsl/
```

Files (15 total):

Core Documentation:
- `index.md` - This index file
- `quick-reference.md` - Quick lookup guide
- `core-patterns.md` - Universal transformation rules
- `field-declarations.md` - Field transformation patterns
- `behavior-patterns.md` - Natural behavior format
- `annotations-constraints.md` - Constraint annotations
- `type-mapping.md` - Type mappings
- `complete-examples.md` - Full examples

Advanced Features:
- `advanced-features-overview.md` - Advanced features introduction
- `temporal-logic.md` - Time-based conditions
- `state-machine.md` - State transitions
- `error-accumulation.md` - Multiple error collection
- `match-expressions.md` - Pattern matching
- `string-operations.md` - String validation/transformation
- `nested-collections.md` - Complex collection queries

## Version

Version: 3.0  
Last Updated: 2026  
Compatible with: DDSL Specification v2.0
