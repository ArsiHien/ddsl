# Phần Bổ Sung: Chi Tiết Kỹ Thuật và Hình Thức Hóa DDSL

## 6. Định Nghĩa Hình Thức của AST và Cú Pháp

### 6.1 Ký Pháp BNF cho Cú Pháp Trừu Tượng

Để cung cấp một định nghĩa hình thức hơn về cú pháp trừu tượng của DDSL, chúng tôi trình bày một biểu diễn Backus-Naur Form (BNF) đơn giản hóa của cấu trúc AST. Mặc dù biểu diễn này không phải là một cú pháp ngôn ngữ hoàn chỉnh, nhưng nó cung cấp một khung làm việc để hiểu các cấu trúc cơ bản:

```
<DomainModel> ::= "DomainModel" <Identifier> "basePackage" <PackageName>
                  { <BoundedContext> }

<BoundedContext> ::= "BoundedContext" <Identifier> "packageName" <PackageName>
                     { <Aggregate> | <ValueObject> | <DomainService> 
                       | <DomainEvent> | <RepositoryInterface> 
                       | <Factory> | <Specification> | <ApplicationService> }

<Aggregate> ::= "Aggregate" <Identifier>
                <AggregateRoot>
                { <Entity> | <ValueObject> | <Invariant> | <Factory> }

<AggregateRoot> ::= "Entity" <Identifier> "(" "AggregateRoot" ")"
                    <IdentityField>
                    { <Field> }
                    { <Method> }
                    { <EventReference> }

<Entity> ::= "Entity" <Identifier>
             <IdentityField>
             { <Field> }
             { <Method> }
             { <EventReference> }

<IdentityField> ::= "identityField" ":" <Identifier> ":" <IdentityType>
                    "generationStrategy" ":" <IdGenerationStrategy>

<IdentityType> ::= "UUID" | "LONG" | "STRING" | "CUSTOM"

<IdGenerationStrategy> ::= "AUTO" | "MANUAL" | "SEQUENCE" | "UUID_GENERATOR"

<Field> ::= "Field" <Identifier> ":" <JavaType>
            [ "visibility" ":" <Visibility> ]
            [ "isFinal" ":" <Boolean> ]
            [ "isNullable" ":" <Boolean> ]
            [ "defaultValue" ":" <Value> ]
            { <Constraint> }

<JavaType> ::= <TypeName> [ "<" <JavaType> { "," <JavaType> } ">" ]

<Visibility> ::= "PUBLIC" | "PROTECTED" | "PACKAGE_PRIVATE" | "PRIVATE"

<Constraint> ::= "Constraint" <ConstraintType> <Value> [ <ErrorMessage> ]

<ConstraintType> ::= "NOT_NULL" | "MIN_LENGTH" | "MAX_LENGTH" 
                     | "REGEX" | "RANGE" | "UNIQUE" | "CUSTOM"

<Method> ::= "Method" <Identifier> "(" { <Parameter> } ")" ":" <JavaType>
             "visibility" ":" <Visibility>
             { <CodeStatement> }

<Parameter> ::= <Identifier> ":" <JavaType> [ "isNullable" ":" <Boolean> ]

<ValueObject> ::= "ValueObject" <Identifier>
                  { <Field> }
                  { <Method> }
                  { <ValidationMethod> }

<ValidationMethod> ::= "ValidationMethod" <Identifier>
                       { <ValidationRule> }

<ValidationRule> ::= "Rule" <Expression> "→" <ErrorMessage>

<DomainService> ::= "DomainService" <Identifier> [ "isInterface" ":" <Boolean> ]
                    { <Method> }

<DomainEvent> ::= "DomainEvent" <Identifier>
                  "aggregateId" ":" <Field>
                  "occurredOn" ":" <Field>
                  { <Field> }

<RepositoryInterface> ::= "RepositoryInterface" <Identifier>
                          "aggregateType" ":" <Identifier>
                          "idType" ":" <JavaType>
                          { <RepositoryMethod> }

<RepositoryMethod> ::= "Method" <Identifier> "(" { <Parameter> } ")" ":" <JavaType>
                       "type" ":" <RepositoryMethodType>

<RepositoryMethodType> ::= "FIND_BY_ID" | "FIND_ALL" | "FIND_BY_CRITERIA"
                           | "SAVE" | "DELETE" | "COUNT" | "EXISTS" | "CUSTOM"

<Factory> ::= "Factory" <Identifier>
              "createdType" ":" <Identifier>
              { <FactoryMethod> }

<FactoryMethod> ::= "Method" <Identifier> "(" { <Parameter> } ")" ":" <JavaType>
                    { <CodeStatement> }

<Specification> ::= "Specification" <Identifier>
                    "targetType" ":" <JavaType>
                    "criteriaField" ":" <Field>

<ApplicationService> ::= "ApplicationService" <Identifier>
                         { <Dependency> }
                         { <UseCase> }

<UseCase> ::= "UseCase" <Identifier> "(" { <Parameter> } ")" ":" <JavaType>
              { <CodeStatement> }

<Dependency> ::= "Dependency" <Identifier> ":" <JavaType>
                 "injectionType" ":" <InjectionType>

<InjectionType> ::= "CONSTRUCTOR" | "SETTER" | "FIELD"

<Invariant> ::= "Invariant" <Identifier>
                "expression" ":" <Expression>
                "errorMessage" ":" <ErrorMessage>

<EventReference> ::= "EventReference" <Identifier>
                     "triggerCondition" ":" <Expression>

<Identifier> ::= <Letter> { <Letter> | <Digit> | "_" }
<PackageName> ::= <Identifier> { "." <Identifier> }
<TypeName> ::= <Identifier> | "java.util." <Identifier>
<Value> ::= <Number> | <String> | <Boolean>
<Expression> ::= <String>
<ErrorMessage> ::= <String>
<Boolean> ::= "true" | "false"
<Letter> ::= "a".."z" | "A".."Z"
<Digit> ::= "0".."9"
```

### 6.2 Mô Hình Dữ Liệu của AST

Để chính thức hóa hơn nữa cấu trúc dữ liệu của AST, chúng tôi mô tả các lớp cơ bản sử dụng ký pháp pseudo-code tương tự Java:

```
abstract class ASTNode {
    SourceLocation location;
    String documentation;
    abstract void accept(CodeGenVisitor visitor);
}

class SourceLocation {
    int line;           // 1-based
    int column;         // 1-based
    String filePath;
    
    int getLine();
    int getColumn();
    String getFilePath();
}

class JavaType {
    String name;                    // e.g., "String", "Order"
    String packageName;             // e.g., "com.example.domain"
    List<JavaType> genericTypes;    // e.g., [Order] for List<Order>
    
    String getName();
    String getPackageName();
    String getFullyQualifiedName();
    boolean isCollection();
    List<JavaType> getGenericTypes();
}

class DomainModel extends ASTNode {
    String modelName;
    String basePackage;
    List<BoundedContext> boundedContexts;
    
    void addBoundedContext(BoundedContext context);
    List<BoundedContext> getBoundedContexts();
}

class BoundedContext extends ASTNode {
    String name;
    String packageName;
    List<Aggregate> aggregates;
    List<ValueObject> valueObjects;
    List<DomainService> domainServices;
    List<DomainEvent> domainEvents;
    List<RepositoryInterface> repositories;
    List<Factory> factories;
    List<Specification> specifications;
    List<ApplicationService> applicationServices;
    
    void addAggregate(Aggregate aggregate);
    void addValueObject(ValueObject valueObject);
    // ... other add methods
}

class Aggregate extends ASTNode {
    String name;
    Entity root;                    // Aggregate Root (1..1)
    List<Entity> entities;          // Child entities (0..*)
    List<ValueObject> valueObjects; // (0..*)
    List<Invariant> invariants;     // (0..*)
    List<Factory> factories;        // (0..*)
    
    void setRoot(Entity rootEntity);
    Entity getRoot();
    void addEntity(Entity entity);
    void addValueObject(ValueObject valueObject);
    void addInvariant(Invariant invariant);
    void addFactory(Factory factory);
}

class Entity extends ASTNode {
    String name;
    boolean isAggregateRoot;
    IdentityField identityField;    // (1..1)
    List<Field> fields;             // (0..*)
    List<Method> methods;           // (0..*)
    List<EventReference> events;    // (0..*)
    
    void setIdentityField(IdentityField field);
    IdentityField getIdentityField();
    void addField(Field field);
    void addMethod(Method method);
    void addEventReference(EventReference eventRef);
}

class IdentityField extends ASTNode {
    String name;
    IdentityType type;              // UUID, LONG, STRING, CUSTOM
    IdGenerationStrategy strategy;  // AUTO, MANUAL, SEQUENCE, UUID_GENERATOR
    
    enum IdentityType { UUID, LONG, STRING, CUSTOM }
    enum IdGenerationStrategy { AUTO, MANUAL, SEQUENCE, UUID_GENERATOR }
    
    IdentityField copy();
    List<ValidationError> validate(SourceLocation location);
}

class Field extends ASTNode {
    String name;
    JavaType type;
    Visibility visibility;          // PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE
    boolean isFinal;
    boolean isNullable;
    String defaultValue;
    List<Constraint> constraints;   // (0..*)
    
    enum Visibility { PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE }
    
    void addConstraint(Constraint constraint);
    List<ValidationError> validate();
}

class Method extends ASTNode {
    String name;
    JavaType returnType;
    List<Parameter> parameters;     // (0..*)
    Visibility visibility;
    boolean isStatic;
    boolean isFinal;
    CodeBlock body;                 // (1..1)
    List<JavaType> throwsExceptions;// (0..*)
    Entity entityContext;           // Containing entity (optional)
    
    void addParameter(Parameter param);
    void setBody(CodeBlock block);
    void addThrowsException(JavaType exceptionType);
}

class Parameter extends ASTNode {
    String name;
    JavaType type;
    boolean isNullable;
}

class CodeBlock extends ASTNode {
    List<String> statements;        // Java code statements
    
    void addStatement(String statement);
    String generate();              // Generate complete code block
}

class ValueObject extends ASTNode {
    String name;
    List<Field> fields;             // (0..*)
    List<Method> methods;           // (0..*)
    List<ValidationMethod> validations; // (0..*)
    
    void addField(Field field);
    void addMethod(Method method);
    void addValidation(ValidationMethod validation);
}

class ValidationMethod extends ASTNode {
    String name;
    List<ValidationRule> rules;     // (0..*)
    
    void addRule(ValidationRule rule);
}

class ValidationRule extends ASTNode {
    String expression;              // Logic expression
    String errorMessage;            // Error message
}

class DomainEvent extends ASTNode {
    String name;
    Field aggregateId;              // (1..1) Aggregate ID field
    Field occurredOn;               // (1..1) Timestamp field
    List<Field> fields;             // Event data (0..*)
    
    void addField(Field field);
}

class RepositoryInterface extends ASTNode {
    String name;
    JavaType aggregateType;         // Type of aggregate managed
    JavaType idType;                // Type of aggregate ID
    List<RepositoryMethod> methods; // (0..*)
    
    enum RepositoryMethodType {
        FIND_BY_ID, FIND_ALL, FIND_BY_CRITERIA,
        SAVE, DELETE, COUNT, EXISTS, CUSTOM
    }
    
    void addMethod(RepositoryMethod method);
}

class RepositoryMethod extends ASTNode {
    String name;
    RepositoryMethodType type;
    List<Parameter> parameters;
    JavaType returnType;
}

class Factory extends ASTNode {
    String name;
    JavaType createdType;           // Type created by this factory
    List<FactoryMethod> methods;    // (0..*)
    
    void addMethod(FactoryMethod method);
}

class FactoryMethod extends ASTNode {
    String name;
    List<Parameter> parameters;
    JavaType returnType;
    CodeBlock body;                 // (1..1)
}

class Specification extends ASTNode {
    String name;
    JavaType targetType;            // Type to be specified
    Field criteriaField;            // Criteria field
    
    boolean isSatisfiedBy(Object candidate);
}

class ApplicationService extends ASTNode {
    String name;
    List<UseCase> useCases;         // (0..*)
    List<Dependency> dependencies;  // (0..*)
    
    void addUseCase(UseCase useCase);
    void addDependency(Dependency dependency);
}

class UseCase extends ASTNode {
    String name;
    List<Parameter> parameters;
    JavaType returnType;
    CodeBlock implementation;       // (1..1)
}

class Dependency extends ASTNode {
    String name;
    JavaType type;
    InjectionType injectionType;    // CONSTRUCTOR, SETTER, FIELD
    
    enum InjectionType { CONSTRUCTOR, SETTER, FIELD }
}

class Constraint extends ASTNode {
    ConstraintType type;
    String value;
    String errorMessage;
    
    enum ConstraintType {
        NOT_NULL, MIN_LENGTH, MAX_LENGTH, REGEX,
        RANGE, UNIQUE, CUSTOM
    }
}

class Invariant extends ASTNode {
    String name;
    String expression;              // Logic expression
    String errorMessage;            // Error message when violated
    
    boolean validate();
}

class EventReference extends ASTNode {
    String eventName;               // Name of domain event
    String triggerCondition;        // Condition triggering the event
    
    JavaType getEventType();
}

class DomainService extends ASTNode {
    String name;
    boolean isInterface;
    List<Method> methods;           // (0..*)
    
    void addMethod(Method method);
}
```

### 6.3 Bất Biến Cấu Trúc (Structural Invariants)

Để đảm bảo tính nhất quán của AST, hệ thống thực thi một tập hợp các bất biến cấu trúc:

1. **Tính Duy Nhất của Định Danh:**
   - Trong mỗi DomainModel, các tên BoundedContext phải duy nhất
   - Trong mỗi BoundedContext, các tên Aggregate, ValueObject, DomainService, DomainEvent, RepositoryInterface, Factory, Specification, và ApplicationService phải duy nhất
   - Trong mỗi Aggregate, các tên Entity (bao gồm cả Aggregate Root) và ValueObject phải duy nhất
   - Trong mỗi Entity, các tên Field và Method phải duy nhất

2. **Tính Hợp Lệ của Tham Chiếu:**
   - Mỗi RepositoryInterface phải tham chiếu đến một Aggregate tồn tại trong cùng BoundedContext
   - Mỗi EventReference phải tham chiếu đến một DomainEvent tồn tại trong cùng BoundedContext
   - Mỗi tham chiếu đến JavaType tương ứng với một lớp Java hợp lệ

3. **Yêu Cầu về Aggregate:**
   - Mỗi Aggregate phải có chính xác một Aggregate Root (Entity được đánh dấu isAggregateRoot = true)
   - Aggregate Root phải có chính xác một IdentityField

4. **Yêu Cầu về Entity:**
   - Mỗi Entity phải có chính xác một IdentityField
   - Các IdentityField không thể là null

5. **Yêu Cầu về Field:**
   - Nếu một Field được đánh dấu isFinal = true, nó không thể có defaultValue khác null
   - Các ràng buộc (Constraint) trên một Field phải nhất quán (ví dụ: MIN_LENGTH không thể lớn hơn MAX_LENGTH)

---

## 7. Semantic Analysis và Xác Thực

### 7.1 Quy Trình Xác Thực Ngữ nghĩa

Sau khi phân tích cú pháp tạo ra AST, giai đoạn xác thực ngữ nghĩa kiểm tra một tập hợp các điều kiện để đảm bảo mô hình là hợp lệ:

**Giai Đoạn 1: Xác Thực Bất Biến Cấu Trúc (Structural Invariant Validation)**

Kiểm tra các bất biến cấu trúc được liệt kê ở trên. Nếu phát hiện ra vi phạm, báo lỗi ERROR được sinh ra.

**Giai Đoạn 2: Xác Thực Tham Chiếu (Reference Validation)**

Duyệt qua tất cả các tham chiếu trong mô hình (ví dụ: tham chiếu đến Aggregate trong RepositoryInterface, tham chiếu đến DomainEvent trong EventReference) và xác thực rằng chúng được giải quyết chính xác. Nếu tham chiếu không được giải quyết, báo lỗi ERROR được sinh ra.

**Giai Đoạn 3: Xác Thực Constraint (Constraint Validation)**

Kiểm tra tính nhất quán của các ràng buộc trên Field. Ví dụ:
- MIN_LENGTH phải là số dương
- MAX_LENGTH phải lớn hơn MIN_LENGTH
- REGEX phải là một biểu thức chính quy hợp lệ
- RANGE phải chỉ định các giới hạn hợp lệ

**Giai Đoạn 4: Xác Thực Invariant (Invariant Validation)**

Kiểm tra tính đúng đắn cú pháp của các biểu thức Invariant. Mặc dù việc xác thực ngữ nghĩa đầy đủ của các biểu thức logic là ngoài phạm vi, hệ thống ít nhất có thể kiểm tra cú pháp cơ bản.

**Giai Đoạn 5: Xác Thực ValidationRule (ValidationRule Validation)**

Tương tự như Giai Đoạn 4, kiểm tra tính đúng đắn cú pháp của các biểu thức ValidationRule.

**Giai Đoạn 6: Xác Thực Repository (Repository Validation)**

Kiểm tra xem mỗi RepositoryInterface có tham chiếu đến một Aggregate hợp lệ, và rằng idType của Repository phù hợp với loại định danh của Aggregate Root.

**Giai Đoạn 7: Xác Thực Tính Đầy Đủ (Completeness Validation)**

Kiểm tra xem các thành phần DDD quan trọng có được định nghĩa một cách đầy đủ không. Ví dụ:
- Mỗi Aggregate nên có ít nhất một RepositoryInterface
- Mỗi Aggregate có Method thay đổi trạng thái nên tham chiếu đến ít nhất một DomainEvent
- Mỗi ApplicationService nên có ít nhất một UseCase

### 7.2 Hệ Thống Báo Lỗi

Hệ thống báo lỗi của DDSL cung cấp thông tin chi tiết về các vấn đề được phát hiện:

```
ValidationError {
    String message;              // Error message
    SourceLocation location;     // Where the error occurred
    ErrorSeverity severity;      // ERROR, WARNING, INFO
    String suggestion;           // Suggestion for fixing
}

enum ErrorSeverity {
    ERROR,      // Blocking error, prevents code generation
    WARNING,    // Non-blocking warning, code generation proceeds
    INFO        // Informational message
}
```

Ví dụ về báo lỗi:

```
[ERROR] BlogManagement.java:42
  Message: Aggregate "Post" không có Repository
  Location: BlogManagement.Post
  Suggestion: Thêm RepositoryInterface "PostRepository" cho Aggregate "Post"

[WARNING] BlogManagement.java:67
  Message: Field "title" không có ràng buộc độ dài
  Location: BlogManagement.Post.title
  Suggestion: Thêm Constraint MIN_LENGTH hoặc MAX_LENGTH để bảo vệ độ dài title

[INFO] BlogManagement.java:89
  Message: Method "publish()" không phát sinh bất kỳ DomainEvent nào
  Location: BlogManagement.Post.publish
  Suggestion: Xem xét thêm EventReference nếu phương thức này thay đổi trạng thái
```

---

## 8. Mở Rộng và Liên Kết

### 8.1 Mô Hình Visitor Pattern

Để hỗ trợ các hoạt động khác nhau trên AST mà không cần thay đổi định nghĩa của các lớp nút, DDSL sử dụng mô hình Visitor Pattern:

```
abstract interface CodeGenVisitor {
    void visitDomainModel(DomainModel node);
    void visitBoundedContext(BoundedContext node);
    void visitAggregate(Aggregate node);
    void visitEntity(Entity node);
    void visitValueObject(ValueObject node);
    void visitDomainEvent(DomainEvent node);
    void visitRepositoryInterface(RepositoryInterface node);
    void visitFactory(Factory node);
    void visitApplicationService(ApplicationService node);
    void visitField(Field node);
    void visitMethod(Method node);
    // ... other visit methods
}

class JavaCodeGenVisitor implements CodeGenVisitor {
    private StringBuilder currentOutput;
    
    public void visitEntity(Entity node) {
        // Generate Java class for Entity
        currentOutput.append("public class ").append(node.getName()).append(" {\n");
        // Generate fields
        for (Field field : node.getFields()) {
            field.accept(this);
        }
        // Generate identity field
        node.getIdentityField().accept(this);
        // Generate methods
        for (Method method : node.getMethods()) {
            method.accept(this);
        }
        currentOutput.append("}\n");
    }
    
    public void visitValueObject(ValueObject node) {
        // Generate Java class for ValueObject
        currentOutput.append("public class ").append(node.getName()).append(" {\n");
        // Similar to Entity generation
        currentOutput.append("}\n");
    }
    
    // ... other implementations
}
```

### 8.2 Khả Năng Mở Rộng

Mô hình Visitor Pattern cho phép dễ dàng thêm các hoạt động mới mà không cần sửa đổi các lớp nút:

```
// Để thêm một hoạt động mới (ví dụ: sinh schema cơ sở dữ liệu)
class DatabaseSchemaGenVisitor implements CodeGenVisitor {
    private StringBuilder sqlOutput;
    
    public void visitEntity(Entity node) {
        // Generate CREATE TABLE statement
        sqlOutput.append("CREATE TABLE ").append(node.getName()).append(" (\n");
        for (Field field : node.getFields()) {
            // Generate column definition
        }
        sqlOutput.append(");\n");
    }
}

// Để thêm một hoạt động khác (ví dụ: sinh tài liệu)
class DocumentationGenVisitor implements CodeGenVisitor {
    private StringBuilder docOutput;
    
    public void visitAggregate(Aggregate node) {
        // Generate documentation for Aggregate
        docOutput.append("## ").append(node.getName()).append("\n");
        docOutput.append(node.getDocumentation()).append("\n");
        // Generate documentation for components
    }
}
```

---

## 9. Kết Luận

Định nghĩa hình thức của cú pháp trừu tượng DDSL được trình bày trong phần này cung cấp một cơ sở vững chắc cho việc triển khai các công cụ dựa trên DDSL. Bằng cách chỉ định một AST rõ ràng, xác định các bất biến cấu trúc, và mô tả quy trình xác thực ngữ nghĩa, chúng tôi đảm bảo rằng các mô hình DDSL là hợp lệ và có thể được xử lý một cách đáng tin cậy bởi các công cụ tự động.

Mô hình Visitor Pattern cho phép dễ dàng mở rộng hệ thống để hỗ trợ các hoạt động mới, chẳng hạn như sinh schema cơ sở dữ liệu, tạo tài liệu, hoặc tạo các thành phần kiến trúc bổ sung. Cách tiếp cận này tuân theo các nguyên tắc thiết kế tốt, tạo điều kiện cho sự bảo trì và tiến hóa của hệ thống.

---

**Ghi chú sử dụng:**

Phần bổ sung này có thể được thêm vào bài báo nếu bạn muốn thêm chi tiết kỹ thuật và hình thức hóa. Nó phù hợp cho một phụ lục hoặc một phần "Technical Details" trong bài báo của bạn. Bạn có thể điều chỉnh mức độ chi tiết dựa trên yêu cầu của ấn phẩm.

