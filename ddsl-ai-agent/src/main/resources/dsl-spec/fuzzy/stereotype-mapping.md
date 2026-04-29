---
id: ddsl-fuzzy-003
category: FUZZY_MAPPING
subcategory: stereotype_mapping
language: mixed
complexity: basic
version: 3.0
---

Fuzzy Stereotype Mapping Rules:

These mappings help transform natural language descriptions of DDD stereotypes into DDSL keywords. Both English and Vietnamese terms are supported.

English Mappings:
- "aggregate", "aggregate root", "root entity" → Aggregate
- "entity", "model object" → Entity
- "value object", "VO" → ValueObject
- "event", "domain event" → DomainEvent
- "service", "domain service" → DomainService
- "repository", "repo" → Repository
- "factory" → Factory
- "specification", "spec" → Specification

Vietnamese Mappings:
- "nhóm gốc", "gốc tổng hợp", "aggregate" → Aggregate
- "thực thể", "đối tượng có định danh", "entity" → Entity
- "đối tượng giá trị", "value object" → ValueObject
- "sự kiện", "sự kiện miền" → DomainEvent
- "dịch vụ", "dịch vụ miền" → DomainService
- "kho lưu trữ", "repository" → Repository
- "nhà máy", "tạo đối tượng" → Factory
- "đặc tả", "specification" → Specification

Usage Examples:

Input: "Create an aggregate for Order management"
→ DDSL: Aggregate Order { ... }

Input: "Thực thể OrderItem có các trường..."
→ DDSL: Entity OrderItem { ... }

Input: "Value object Money represents currency"
→ DDSL: ValueObject Money { ... }

See Also:
- nl-to-ddsl/core-patterns.md for general transformation principles
- nl-to-ddsl/type-mapping.md for type mappings
- nl-to-ddsl/field-declarations.md for field name mappings
