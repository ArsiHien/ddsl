---
id: ddsl-fuzzy-001
category: FUZZY_MAPPING
subcategory: type_mapping
language: en
complexity: basic
---
Fuzzy Type Mapping Rules (English):
- "int", "integer", "whole number", "count" → Int
- "string", "text", "varchar", "char" → String
- "bool", "boolean", "flag", "true/false" → Boolean
- "decimal", "float", "double", "number", "real", "price" → Decimal
- "datetime", "timestamp", "date and time" → DateTime
- "date", "day" → Date
- "uuid", "id", "identifier", "guid" → UUID
- "money", "currency", "amount" → Money (ValueObject)
- "list of X", "collection of X", "array of X", "multiple X" → List<X>
- "optional X", "nullable X", "X or null" → X?
