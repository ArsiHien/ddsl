---
id: ddsl-fuzzy-004
category: FUZZY_MAPPING
subcategory: field_normalization
language: mixed
complexity: basic
---
Fuzzy Field Name Normalization Rules:
- snake_case → camelCase: "order_id" → "orderId", "first_name" → "firstName"
- Space-separated → camelCase: "order id" → "orderId", "first name" → "firstName"
- Vietnamese field names → English camelCase:
  "tên" → "name", "mã" → "code/id", "giá" → "price"
  "ngày tạo" → "createdAt", "trạng thái" → "status"
  "số lượng" → "quantity", "mô tả" → "description"
