# Minh Họa Cú Pháp Trừu Tượng của Ngôn Ngữ DDSL
## Domain-Driven Design Specification Language - Một Phương Tiếp Cận Hình Thức

### Tóm Tắt Nghiên Cứu

Tài liệu này trình bày minh họa chi tiết về cú pháp trừu tượng (Abstract Syntax) của DDSL - một ngôn ngữ chuyên dụng (Domain-Specific Language - DSL) được thiết kế để chỉ định các mô hình thiết kế hướng miền (Domain-Driven Design - DDD) một cách hình thức và có cấu trúc. DDSL cung cấp một khuôn khổ toàn diện để mô tả các khái niệm cơ bản của DDD như Aggregate, Entity, Value Object, Domain Service, Repository, Factory, và Application Service.

---

## 1. Giới Thiệu

### 1.1 Bối Cảnh Nghiên Cứu

Thiết kế hướng miền (Domain-Driven Design) là một phương pháp quản lý phức tạp trong phát triển phần mềm bằng cách tập trung vào logic kinh doanh và mô hình miền. Tuy nhiên, các khái niệm DDD thường được biểu diễn thông qua mã nguồn, tài liệu tự do, hoặc các sơ đồ vô cấu trúc. DDSL được đề xuất như một giải pháp để:

1. **Chính thức hóa** các cấu trúc DDD
2. **Tự động hóa** việc sinh mã từ các mô hình miền
3. **Xác thực** tính nhất quán của các mô hình
4. **Cải thiện** giao tiếp giữa các nhà phát triển và chuyên gia miền

### 1.2 Mục Đích Tài Liệu

Tài liệu này cung cấp:
- Giải thích chi tiết về cơ cấu AST (Abstract Syntax Tree) của DDSL
- Các ví dụ cụ thể về cách sử dụng ngôn ngữ
- Minh họa các quan hệ giữa các phần tử trong mô hình
- Hướng dẫn về việc áp dụng DDSL trong thực tiễn

---

## 2. Cơ Cấu Cú Pháp Trừu Tượng (Abstract Syntax Tree - AST)

### 2.1 Cơ Sở Hạ Tầng Lõi (Core Infrastructure)

#### 2.1.1 Lớp ASTNode Trừu Tượng

Tất cả các phần tử trong DDSL đều kế thừa từ lớp `ASTNode` trừu Tượng, là nền tảng cho việc biểu diễn các nút cây cú pháp.

```
ASTNode (lớp trừu tượng)
├── Thuộc tính:
│   ├── location: SourceLocation       // Vị trí trong mã nguồn
│   └── documentation: String          // Tài liệu mô tả
└── Phương thức:
    └── accept(visitor: CodeGenVisitor): void  // Pattern Visitor
```

**Ý nghĩa:** Mỗi phần tử trong mô hình DDD cần được ghi chú vị trí của nó (cho báo lỗi) và tài liệu hóa (cho sinh mã).

#### 2.1.2 Lớp SourceLocation

Ghi lại vị trí chính xác của mỗi phần tử trong tập tin nguồn:

```
SourceLocation
├── getLine(): int           // Dòng (1-based)
├── getColumn(): int         // Cột (1-based)
└── getFilePath(): String    // Đường dẫn tập tin
```

**Ứng dụng:** Giúp in báo lỗi rõ ràng và cho phép theo dõi trở lại từ mã sinh đến mã nguồn.

#### 2.1.3 Lớp JavaType

Biểu diễn thông tin kiểu dữ liệu Java với hỗ trợ cho generics và collections:

```
JavaType
├── getName(): String                      // Tên đơn giản
├── getPackageName(): String               // Gói Java
├── isCollection(): boolean                // Có phải collection?
└── getGenericTypes(): List<JavaType>      // Các kiểu generic
```

**Ví dụ:**
- `String` → getName() = "String"
- `List<Order>` → isCollection() = true, getGenericTypes() = [Order]
- `java.util.UUID` → getPackageName() = "java.util"

#### 2.1.4 Lớp ValidationError

Biểu diễn các lỗi xác thực phát hiện trong quá trình phân tích:

```
ValidationError
├── message: String              // Nội dung lỗi
├── location: SourceLocation     // Nơi lỗi xảy ra
└── severity: ErrorSeverity      // Mức độ (ERROR, WARNING, INFO)
```

---

### 2.2 Mô Hình Miền (Domain Model)

#### 2.2.1 Lớp DomainModel

Là gốc của toàn bộ cấu trúc mô hình DDD, đại diện cho một miền hoàn chỉnh:

```
DomainModel (extends ASTNode)
├── Thuộc tính:
│   ├── modelName: String                    // Tên mô hình
│   ├── basePackage: String                  // Gói cơ sở Java
│   └── boundedContexts: List<BoundedContext>// Các Bounded Context
└── Phương thức:
    ├── addBoundedContext(context: BoundedContext): void
    └── generateCode(): CodeArtifacts
```

**Ý nghĩa:** DomainModel là container cấp cao nhất, chứa tất cả các Bounded Context của hệ thống.

#### 2.2.2 Lớp BoundedContext

Đại diện cho một bối cảnh giới hạn - một miền con với các khái niệm riêng:

```
BoundedContext (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                           // Tên BC
│   ├── packageName: String                    // Gói Java
│   ├── aggregates: List<Aggregate>            // Danh sách Aggregate
│   ├── valueObjects: List<ValueObject>        // Danh sách Value Object
│   ├── domainServices: List<DomainService>    // Danh sách Domain Service
│   ├── domainEvents: List<DomainEvent>        // Danh sách Domain Event
│   ├── repositories: List<RepositoryInterface>// Danh sách Repository
│   ├── factories: List<Factory>               // Danh sách Factory
│   ├── specifications: List<Specification>    // Danh sách Specification
│   └── applicationServices: List<ApplicationService> // App Services
└── Phương thức:
    ├── addAggregate(aggregate: Aggregate): void
    ├── addValueObject(vo: ValueObject): void
    ├── addDomainService(service: DomainService): void
    ├── addDomainEvent(event: DomainEvent): void
    ├── addRepository(repo: RepositoryInterface): void
    ├── addFactory(factory: Factory): void
    ├── addSpecification(spec: Specification): void
    └── addApplicationService(service: ApplicationService): void
```

**Ý nghĩa:** BoundedContext giải pháp cho vấn đề về ngôn ngữ chung (Ubiquitous Language) - mỗi BC có thể có các khái niệm khác nhau cho cùng một thuật ngữ.

**Ví dụ thực tiễn:** Trong một ứng dụng thương mại điện tử:
- **BoundedContext "Ordering"**: Tập trung vào đơn đặt hàng, giỏ hàng
- **BoundedContext "Inventory"**: Quản lý kho hàng, tồn kho
- **BoundedContext "Shipping"**: Xử lý vận chuyển, theo dõi

---

### 2.3 Gói Aggregate

#### 2.3.1 Lớp Aggregate

Aggregate là một cụm các đối tượng miền được giữ lại cùng nhau để bảo vệ tính bất biến:

```
Aggregate (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên Aggregate
│   ├── root: Entity                      // Entity gốc (bắt buộc)
│   ├── entities: List<Entity>            // Các Entity bên trong
│   ├── valueObjects: List<ValueObject>   // Các Value Object
│   ├── invariants: List<Invariant>       // Các bất biến
│   └── factories: List<Factory>          // Các Factory
└── Phương thức:
    ├── addEntity(entity: Entity): void
    ├── addValueObject(vo: ValueObject): void
    ├── addInvariant(invariant: Invariant): void
    └── addFactory(factory: Factory): void
```

**Ý nghĩa:** Aggregate là ranh giới giao dịch. Tất cả các thay đổi trong Aggregate phải được xử lý như một đơn vị nguyên tử để duy trì tính bất biến.

#### 2.3.2 Lớp Invariant

Biểu diễn các quy tắc kinh doanh bắt buộc phải được duy trì:

```
Invariant (extends ASTNode)
├── Thuộc tính:
│   ├── name: String              // Tên bất biến
│   ├── expression: String        // Biểu thức logic (VD: amount >= 0)
│   └── errorMessage: String      // Tin nhắn lỗi khi vi phạm
└── Phương thức:
    └── validate(): boolean        // Kiểm tra bất biến
```

**Ví dụ:** 
```
Invariant "MinimumOrderQuantity"
  expression: "items.size() >= 1"
  errorMessage: "Đơn đặt hàng phải có ít nhất một mục"
```

---

### 2.4 Gói Entity

#### 2.4.1 Lớp Entity

Entity đại diện cho các đối tượng có danh tính duy nhất:

```
Entity (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên Entity
│   ├── isAggregateRoot: boolean          // Có phải gốc Aggregate?
│   ├── identityField: IdentityField      // Trường định danh
│   ├── fields: List<Field>               // Danh sách các trường
│   ├── methods: List<Method>             // Danh sách các phương thức
│   └── domainEvents: List<EventReference>// Các sự kiện miền
└── Phương thức:
    ├── addField(field: Field): void
    ├── addMethod(method: Method): void
    └── addDomainEvent(event: EventReference): void
```

**Ý nghĩa:** Entity khác Value Object ở chỗ chúng có danh tính duy nhất và có vòng đời của chúng.

#### 2.4.2 Lớp IdentityField

Xác định trường định danh duy nhất của Entity:

```
IdentityField (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên trường
│   ├── type: IdentityType                // Loại định danh
│   └── generationStrategy: IdGenerationStrategy // Chiến lược sinh
└── Phương thức:
    ├── copy(): IdentityField             // Sao chép trường
    └── validate(location: SourceLocation): List<ValidationError>
```

**Các IdentityType:**
- `UUID`: Định danh dạng UUID (v4, v5)
- `LONG`: Định danh số nguyên 64-bit
- `STRING`: Định danh chuỗi tùy chỉnh
- `CUSTOM`: Định danh tùy chỉnh khác

**Các IdGenerationStrategy:**
- `AUTO`: Tự động sinh bằng database (auto-increment)
- `MANUAL`: Không sinh, do ứng dụng cung cấp
- `SEQUENCE`: Dùng database sequence
- `UUID_GENERATOR`: Sinh UUID từ ứng dụng

#### 2.4.3 Lớp EventReference

Tham chiếu đến các sự kiện miền mà Entity này có thể phát sinh:

```
EventReference (extends ASTNode)
├── Thuộc tính:
│   ├── eventName: String              // Tên sự kiện
│   └── triggerCondition: String       // Điều kiện phát sinh
└── Phương thức:
    └── getEventType(): JavaType       // Lấy kiểu sự kiện
```

**Ví dụ:**
```
EventReference "OrderCreated"
  triggerCondition: "quantity > 0"
```

---

### 2.5 Gói Value Object

#### 2.5.1 Lớp ValueObject

Value Object biểu diễn các đối tượng không có danh tính, định nghĩa bằng các thuộc tính của nó:

```
ValueObject (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                         // Tên VO
│   ├── fields: List<Field>                  // Danh sách trường
│   ├── methods: List<Method>                // Danh sách phương thức
│   └── validations: List<ValidationMethod>  // Các xác thực
└── Phương thức:
    ├── addField(field: Field): void
    ├── addMethod(method: Method): void
    └── addValidation(validation: ValidationMethod): void
```

#### 2.5.2 Lớp ValidationMethod

Định nghĩa một phương thức xác thực cho Value Object:

```
ValidationMethod (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                    // Tên phương thức xác thực
│   └── rules: List<ValidationRule>     // Danh sách quy tắc
└── Phương thức:
    └── addRule(rule: ValidationRule): void
```

#### 2.5.3 Lớp ValidationRule

Biểu diễn một quy tắc xác thực duy nhất:

```
ValidationRule (extends ASTNode)
├── Thuộc tính:
│   ├── expression: String        // Biểu thức xác thực
│   └── errorMessage: String      // Tin nhắn lỗi
└── Phương thức:
    └── validate(): boolean
```

**Ví dụ ValueObject với xác thực:**
```
ValueObject "Money"
  Field "amount: BigDecimal"
  Field "currency: String"
  
  ValidationMethod "validateAmount"
    Rule "amount > 0" → "Số tiền phải dương"
    Rule "currency != null" → "Đơn vị tiền không được trống"
```

---

### 2.6 Gói Domain Services

#### 2.6.1 Lớp DomainService

Dịch vụ miền biểu diễn logic không thuộc về bất kỳ Entity hoặc Value Object nào:

```
DomainService (extends ASTNode)
├── Thuộc tính:
│   ├── name: String              // Tên dịch vụ
│   ├── isInterface: boolean       // Có phải giao diện?
│   └── methods: List<Method>      // Danh sách phương thức
└── Phương thức:
    └── addMethod(method: Method): void
```

#### 2.6.2 Lớp DomainEvent

Sự kiện miền biểu diễn các sự kiện quan trọng xảy ra trong miền:

```
DomainEvent (extends ASTNode)
├── Thuộc tính:
│   ├── name: String              // Tên sự kiện
│   ├── aggregateId: Field        // ID của Aggregate phát sinh sự kiện
│   ├── occurredOn: Field         // Thời gian xảy ra
│   └── fields: List<Field>       // Dữ liệu của sự kiện
└── Phương thức:
    └── addField(field: Field): void
```

**Ví dụ:**
```
DomainEvent "OrderConfirmedEvent"
  Field "orderId: UUID"
  Field "occurredOn: LocalDateTime"
  Field "totalAmount: Money"
  Field "confirmationDate: LocalDate"
```

---

### 2.7 Gói Repository

#### 2.7.1 Lớp RepositoryInterface

Repository xác định giao diện để truy cập và lưu các Aggregate:

```
RepositoryInterface (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                           // Tên Repository
│   ├── aggregateType: JavaType                // Loại Aggregate quản lý
│   ├── idType: JavaType                       // Loại định danh
│   └── methods: List<RepositoryMethod>        // Danh sách phương thức
└── Phương thức:
    └── addMethod(method: RepositoryMethod): void
```

#### 2.7.2 Lớp RepositoryMethod

Phương thức trong Repository:

```
RepositoryMethod (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                          // Tên phương thức
│   ├── type: RepositoryMethodType            // Loại phương thức
│   ├── parameters: List<Parameter>           // Tham số
│   └── returnType: JavaType                  // Kiểu trả về
```

**Các RepositoryMethodType:**
- `FIND_BY_ID`: Tìm Aggregate theo định danh
- `FIND_ALL`: Lấy tất cả Aggregate
- `FIND_BY_CRITERIA`: Tìm theo tiêu chí
- `SAVE`: Lưu hoặc cập nhật Aggregate
- `DELETE`: Xóa Aggregate
- `COUNT`: Đếm Aggregate
- `EXISTS`: Kiểm tra sự tồn tại
- `CUSTOM`: Phương thức tùy chỉnh

---

### 2.8 Gói Factory

#### 2.8.1 Lớp Factory

Factory định nghĩa cách tạo các Aggregate phức tạp:

```
Factory (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên Factory
│   ├── createdType: JavaType             // Loại được tạo
│   └── methods: List<FactoryMethod>      // Danh sách phương thức
└── Phương thức:
    └── addMethod(method: FactoryMethod): void
```

#### 2.8.2 Lớp FactoryMethod

Phương thức tạo trong Factory:

```
FactoryMethod (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên phương thức
│   ├── parameters: List<Parameter>       // Tham số đầu vào
│   ├── returnType: JavaType              // Kiểu được tạo
│   └── body: CodeBlock                   // Thân phương thức
```

---

### 2.9 Gói Specification

#### 2.9.1 Lớp Specification

Specification biểu diễn các tiêu chí phức tạp để tìm kiếm hoặc xác thực:

```
Specification (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên Specification
│   ├── targetType: JavaType              // Loại đối tượng đích
│   └── criteriaField: Field              // Trường tiêu chí
└── Phương thức:
    └── isSatisfiedBy(candidate: Object): boolean
```

#### 2.9.2 Lớp AggregateReference

Tham chiếu đến một Aggregate khác:

```
AggregateReference (extends ASTNode)
├── Thuộc tính:
│   ├── referencedAggregate: String       // Tên Aggregate được tham chiếu
│   └── referenceType: ReferenceType      // Loại tham chiếu
└── Phương thức:
    └── resolve(): Aggregate
```

---

### 2.10 Gói Application Services

#### 2.10.1 Lớp ApplicationService

Dịch vụ ứng dụng điều phối logic xử lý use case:

```
ApplicationService (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên dịch vụ
│   ├── useCases: List<UseCase>           // Danh sách use case
│   └── dependencies: List<Dependency>    // Các phụ thuộc
└── Phương thức:
    ├── addUseCase(useCase: UseCase): void
    └── addDependency(dependency: Dependency): void
```

#### 2.10.2 Lớp UseCase

Use case trong Application Service:

```
UseCase (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên use case
│   ├── parameters: List<Parameter>       // Tham số đầu vào
│   ├── returnType: JavaType              // Kiểu trả về
│   └── implementation: CodeBlock         // Thân thực hiện
```

#### 2.10.3 Lớp Dependency

Biểu diễn một phụ thuộc (injection):

```
Dependency (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên phụ thuộc
│   ├── type: JavaType                    // Loại phụ thuộc
│   └── injectionType: InjectionType      // Loại tiêm (constructor, setter, field)
```

---

### 2.11 Gói Building Blocks

#### 2.11.1 Lớp Field

Trường dữ liệu trong Entity hoặc Value Object:

```
Field (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên trường
│   ├── type: JavaType                    // Kiểu dữ liệu
│   ├── visibility: Visibility            // Tầm nhìn (public/private/...)
│   ├── isFinal: boolean                  // Có phải final?
│   ├── isNullable: boolean               // Có thể null?
│   ├── defaultValue: String              // Giá trị mặc định
│   └── constraints: List<Constraint>     // Các ràng buộc
└── Phương thức:
    └── validate(): List<ValidationError>
```

#### 2.11.2 Lớp Method

Phương thức trong Entity, Value Object, hoặc Domain Service:

```
Method (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên phương thức
│   ├── returnType: JavaType              // Kiểu trả về
│   ├── parameters: List<Parameter>       // Danh sách tham số
│   ├── visibility: Visibility            // Tầm nhìn
│   ├── isStatic: boolean                 // Có phải static?
│   ├── isFinal: boolean                  // Có phải final?
│   ├── body: CodeBlock                   // Thân phương thức
│   ├── throwsExceptions: List<JavaType>  // Ngoại lệ
│   └── entityContext: Entity             // Entity chứa phương thức
```

#### 2.11.3 Lớp Parameter

Tham số của phương thức:

```
Parameter (extends ASTNode)
├── Thuộc tính:
│   ├── name: String                      // Tên tham số
│   ├── type: JavaType                    // Kiểu tham số
│   └── isNullable: boolean               // Có thể null?
```

#### 2.11.4 Lớp CodeBlock

Thân mã máy:

```
CodeBlock (extends ASTNode)
├── Thuộc tính:
│   └── statements: List<String>          // Danh sách câu lệnh
└── Phương thức:
    ├── addStatement(statement: String): void
    └── generate(): String                // Sinh mã Java
```

#### 2.11.5 Lớp Constraint

Ràng buộc trên trường:

```
Constraint (extends ASTNode)
├── Thuộc tính:
│   ├── type: ConstraintType              // Loại ràng buộc
│   ├── value: String                     // Giá trị ràng buộc
│   └── errorMessage: String              // Tin nhắn lỗi
```

**Các ConstraintType:**
- `NOT_NULL`: Không được null
- `MIN_LENGTH`: Độ dài tối thiểu
- `MAX_LENGTH`: Độ dài tối đa
- `REGEX`: Biểu thức chính quy
- `RANGE`: Phạm vi giá trị
- `UNIQUE`: Giá trị duy nhất
- `CUSTOM`: Ràng buộc tùy chỉnh

#### 2.11.6 Enum Visibility

Tầm nhìn của trường hoặc phương thức:

```
Visibility {
  PUBLIC,              // Công khai
  PROTECTED,           // Được bảo vệ
  PACKAGE_PRIVATE,     // Gói riêng
  PRIVATE              // Riêng tư
}
```

---

## 3. Các Quan Hệ Trong AST

### 3.1 Quan Hệ Kế Thừa

Tất cả các phần tử trong mô hình DDSL đều kế thừa từ `ASTNode`:

```
ASTNode
├── DomainModel
├── BoundedContext
├── Aggregate
├── Entity
├── ValueObject
├── DomainService
├── DomainEvent
├── RepositoryInterface
├── Factory
├── Specification
└── ApplicationService
```

### 3.2 Quan Hệ Chứa Đựng

```
DomainModel
  → BoundedContext (1..*)
    → Aggregate (1..*)
      → Entity (1..*)
      → ValueObject (0..*)
      → Invariant (0..*)
      → Factory (0..*)
    → ValueObject (0..*)
    → DomainService (0..*)
    → DomainEvent (0..*)
    → RepositoryInterface (0..*)
    → Factory (0..*)
    → Specification (0..*)
    → ApplicationService (0..*)
```

### 3.3 Quan Hệ Thành Phần

```
Entity
  → IdentityField (1)
  → Field (0..*)
  → Method (0..*)
  → EventReference (0..*)

Method
  → Parameter (0..*)
  → CodeBlock (1)

Field
  → Constraint (0..*)
```

---

## 4. Ví Dụ Thực Tiễn: Hệ Thống Quản Lý Blog

Để minh họa cú pháp trừu tượng, chúng ta sẽ xây dựng một mô hình DDD đơn giản cho hệ thống quản lý blog.

### 4.1 Cấu Trúc Mô Hình

```
DomainModel "BlogSystem"
  basePackage: "com.example.blog"
  
  BoundedContext "BlogManagement"
    packageName: "com.example.blog.management"
    
    Aggregate "Post"
      Entity "Post" (AggregateRoot)
        identityField:
          name: "postId"
          type: UUID
          generationStrategy: UUID_GENERATOR
        
        fields:
          - Field "title: String"
              visibility: PRIVATE
              isFinal: true
              isNullable: false
              constraints:
                - Constraint MIN_LENGTH "5"
                - Constraint MAX_LENGTH "200"
          
          - Field "content: String"
              visibility: PRIVATE
              isFinal: true
              isNullable: false
          
          - Field "author: String"
              visibility: PRIVATE
              isNullable: false
          
          - Field "createdAt: LocalDateTime"
              visibility: PRIVATE
              isFinal: true
          
          - Field "status: PostStatus"
              visibility: PRIVATE
              defaultValue: "DRAFT"
        
        methods:
          - Method "publish(): void"
              visibility: PUBLIC
              body:
                - "if (status != DRAFT) throw new IllegalStateException();"
                - "this.status = PUBLISHED;"
                - "fireEvent(new PostPublishedEvent(postId, now()));"
          
          - Method "unpublish(): void"
              visibility: PUBLIC
        
        domainEvents:
          - EventReference "PostPublishedEvent"
              triggerCondition: "status == PUBLISHED"
          - EventReference "PostContentUpdatedEvent"
              triggerCondition: "content changed"
      
      ValueObject "PostStatus"
        fields:
          - Field "value: String"
        
        validations:
          - ValidationMethod "validateStatus"
              rules:
                - Rule "value in [DRAFT, PUBLISHED, ARCHIVED]"
                  errorMessage: "Trạng thái không hợp lệ"
      
      Factory "PostFactory"
        methods:
          - FactoryMethod "createNewPost(title, content, author)"
              parameters:
                - Parameter "title: String"
                - Parameter "content: String"
                - Parameter "author: String"
              returnType: "Post"
              body:
                - "UUID postId = UUID.randomUUID();"
                - "return new Post(postId, title, content, author, now(), DRAFT);"
    
    RepositoryInterface "PostRepository"
      aggregateType: "Post"
      idType: "UUID"
      methods:
        - RepositoryMethod "findById(postId: UUID): Optional<Post>"
            type: FIND_BY_ID
        - RepositoryMethod "findAll(): List<Post>"
            type: FIND_ALL
        - RepositoryMethod "findByAuthor(author: String): List<Post>"
            type: FIND_BY_CRITERIA
        - RepositoryMethod "save(post: Post): void"
            type: SAVE
        - RepositoryMethod "delete(postId: UUID): void"
            type: DELETE
    
    DomainEvent "PostPublishedEvent"
      aggregateId: "postId: UUID"
      occurredOn: "publishedAt: LocalDateTime"
      fields:
        - Field "title: String"
        - Field "authorId: String"
    
    DomainService "PostSearchService"
      isInterface: true
      methods:
        - Method "searchByKeyword(keyword: String): List<Post>"
            returnType: "List<Post>"
        - Method "findRecentPosts(limit: int): List<Post>"
            returnType: "List<Post>"
    
    ApplicationService "PublishPostService"
      dependencies:
        - Dependency "postRepository: PostRepository"
            injectionType: "CONSTRUCTOR"
        - Dependency "eventPublisher: EventPublisher"
            injectionType: "CONSTRUCTOR"
      
      useCases:
        - UseCase "publishPost(publishPostCommand)"
            parameters:
              - Parameter "command: PublishPostCommand"
            returnType: "void"
            implementation:
              - "Post post = postRepository.findById(command.postId);"
              - "post.publish();"
              - "postRepository.save(post);"
```

### 4.2 Giải Thích Chi Tiết

#### 4.2.1 Aggregate "Post"

Aggregate này bao gồm:
- **Aggregate Root:** Entity "Post" với định danh duy nhất `postId` (UUID)
- **Value Object:** "PostStatus" để biểu diễn trạng thái
- **Factory:** "PostFactory" để tạo mới Post một cách an toàn
- **Repository:** "PostRepository" để truy cập Post
- **Domain Event:** "PostPublishedEvent" được phát sinh khi Post được xuất bản

#### 4.2.2 Bất Biến (Invariants)

Mặc dù không được hiển thị rõ ràng trong ví dụ, các bất biến được thực thi bởi:
- Phương thức `publish()` kiểm tra status hiện tại
- Constraints trên `title` (độ dài 5-200 ký tự)
- PostStatus chỉ chấp nhận ba giá trị hợp lệ

#### 4.2.3 Domain Event

`PostPublishedEvent` được phát sinh khi trạng thái chuyển sang PUBLISHED, cho phép các Bounded Context khác biết về sự kiện này.

#### 4.2.4 Application Service

`PublishPostService` điều phối logic use case "publish post":
1. Lấy Post từ Repository
2. Gọi phương thức `publish()` để phát sinh event
3. Lưu Post đã thay đổi

---

## 5. Các Đặc Tính Chính của DDSL

### 5.1 Chính Thức Hóa (Formality)

DDSL cung cấp một cấu trúc hình thức để biểu diễn các khái niệm DDD:
- Mỗi phần tử có kiểu và cấu trúc rõ ràng
- Các quan hệ được định nghĩa một cách tường minh
- Các quy tắc xác thực được chỉ định rõ ràng

### 5.2 Tự Động Hóa Sinh Mã (Code Generation)

DDSL cho phép sinh tự động:
- Các lớp Entity và Value Object
- Repository implementations
- Domain Event classes
- Application Service stubs
- Unit test templates

### 5.3 Xác Thực Tính Nhất Quán (Consistency Validation)

DDSL thực hiện xác thực:
- Định danh duy nhất trong BoundedContext
- Các tham chiếu Aggregate hợp lệ
- Tính đầy đủ của invariants
- Tính hợp lệ của các quy tắc xác thực

### 5.4 Hỗ Trợ Ngôn Ngữ Chung (Ubiquitous Language)

DDSL tạo điều kiện cho:
- Tài liệu hóa các khái niệm miền
- Giao tiếp giữa nhà phát triển và chuyên gia miền
- Tracing từ mã sinh về định nghĩa mô hình

---

## 6. Quy Trình Biên Dịch

### 6.1 Các Giai Đoạn

```
Tập tin DDSL
    ↓
[Phân tích cú pháp (Parsing)]
    ↓
AST (Abstract Syntax Tree)
    ↓
[Xác thực tính nhất quán (Validation)]
    ↓
Mô hình DDD xác thực
    ↓
[Sinh mã (Code Generation)]
    ↓
Mã Java + Tài liệu
```

### 6.2 Báo Cáo Lỗi

Nếu xác thực thất bại, DDSL sinh báo cáo chi tiết:

```
Error [BlogManagement.java:25]
  Message: Trường "title" không có ràng buộc độ dài
  Severity: WARNING
  Location: BlogManagement.Post.title
  Suggestion: Thêm @Size hoặc @Length annotation
```

---

## 7. Kết Luận

DDSL cung cấp một khuôn khổ toàn diện để chính thức hóa các mô hình DDD. Bằng cách cấu trúc hóa các khái niệm DDD thành một Abstract Syntax Tree rõ ràng, DDSL cho phép:

1. **Giao tiếp rõ ràng** giữa các bên liên quan
2. **Tự động hóa** việc sinh mã
3. **Xác thực** tính nhất quán của mô hình
4. **Tăng cường** chất lượng thiết kế phần mềm

Cú pháp trừu tượng được trình bày trong tài liệu này tạo thành nền tảng cho việc xây dựng các công cụ phát triển tiên tiến hỗ trợ Domain-Driven Design một cách chuyên nghiệp.

---

## Tài Liệu Tham Khảo

- Evans, E. (2003). Domain-Driven Design: Tackling Complexity in the Heart of Software. Addison-Wesley.
- Mernik, M., Heering, J., & Sloane, A. M. (2005). When and how to develop domain-specific languages. ACM Computing Surveys, 37(4), 316-344.
- Fowler, M. (2010). Domain Specific Languages. Addison-Wesley Professional.
- Parr, T. (2009). Language Implementation Patterns. Pragmatic Bookshelf.

---

**Phiên bản:** 1.0  
**Ngày tạo:** Tháng 12, 2024  
**Trạng thái:** Dự thảo học thuật

