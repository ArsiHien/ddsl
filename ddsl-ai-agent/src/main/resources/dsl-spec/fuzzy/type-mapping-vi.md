---
id: ddsl-fuzzy-002
category: FUZZY_MAPPING
subcategory: type_mapping
language: vi
complexity: basic
---
Fuzzy Type Mapping Rules (Vietnamese):
- "số nguyên", "kiểu số", "số đếm" → Int
- "chuỗi", "văn bản", "chữ", "ký tự" → String
- "đúng/sai", "logic", "có/không" → Boolean
- "số thực", "số thập phân", "tiền" → Decimal
- "thời gian", "ngày giờ", "dấu thời gian" → DateTime
- "ngày", "ngày tháng" → Date
- "mã", "mã định danh", "khóa" → UUID
- "tiền tệ", "giá", "số tiền" → Money
- "danh sách", "tập hợp" → List<X>
- "có thể null", "tùy chọn", "không bắt buộc" → X?
- "bắt buộc", "không được trống" → @required
- "khóa chính", "định danh" → @identity
