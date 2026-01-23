# Trình Bày Cú Pháp Trừu Tượng của Ngôn Ngữ DDSL cho Công Bố Học Thuật

## 4. Cú Pháp Trừu Tượng và Cấu Trúc Mô Hình của DDSL

### 4.1 Giới Thiệu về Cơ Sở Hạ Tầng Lõi

Trong thiết kế của DDSL, tất cả các phần tử cấu thành mô hình miền đều được biểu diễn dưới dạng một cây cú pháp trừu tượng (Abstract Syntax Tree - AST). Điều này cho phép ngôn ngữ cung cấp một cấu trúc hình thức, có thể xác thực và có thể xử lý bằng máy tính. Nền tảng của cây cú pháp này được xây dựng trên một lớp trừu tượng cốt lõi là ASTNode, từ đó tất cả các phần tử DDD khác đều kế thừa. Cách tiếp cận này cho phép áp dụng một cơ chế xử lý thống nhất thông qua mô hình Visitor, có thể được sử dụng cho các mục đích sinh mã, xác thực, và phân tích.

Mỗi nút trong cây cú pháp được liên kết với một đối tượng SourceLocation, giữ lại thông tin chính xác về vị trí của phần tử đó trong tệp nguồn. Thông tin này bao gồm số dòng, số cột, và đường dẫn tệp, cho phép hệ thống cung cấp báo lỗi chính xác và hỗ trợ theo dõi các tham chiếu từ mã sinh trở lại định nghĩa gốc. Tài liệu của mỗi phần tử cũng được lưu giữ tại mức AST, từ đó các công cụ sinh mã có thể khai thác thông tin này để tạo ra các tệp JavaDoc toàn diện.

Để xử lý đa dạng các kiểu dữ liệu Java, bao gồm các kiểu generic và collection, DDSL định nghĩa một lớp JavaType chuyên dụng. Lớp này không chỉ lưu trữ tên và gói của kiểu dữ liệu, mà còn cung cấp các phương thức để kiểm tra xem kiểu dó có phải là một collection, và nếu vậy, để truy cập các kiểu generic của nó. Điều này là quan trọng vì trong DDD, các Aggregate thường chứa các collection của Entity hoặc Value Object, và cần phải xử lý một cách cẩn thận trong quá trình sinh mã và xác thực.

### 4.2 Cấp Độ Mô Hình Miền

Cấu trúc cấp cao nhất của DDSL được định nghĩa bởi lớp DomainModel, đại diện cho một miền hoàn chỉnh được mô tả bởi ngôn ngữ. DomainModel đóng vai trò như một container cho tất cả các Bounded Context của hệ thống, mỗi cái đại diện cho một miền con riêng biệt với ngôn ngữ và logic kinh doanh của riêng nó. Tên mô hình và gói cơ sở (base package) được chỉ định ở cấp này, cung cấp nền tảng cho cấu trúc gói Java của tất cả các lớp được sinh ra.

Mỗi BoundedContext chứa một bộ sưu tập hoàn chỉnh các khái niệm DDD: Aggregate, Value Object, Domain Service, Domain Event, Repository, Factory, Specification, và Application Service. Điều này phản ánh triết lý DDD rằng mỗi bối cảnh giới hạn là một đơn vị độc lập, với ranh giới rõ ràng và ngôn ngữ chung riêng. Tên của BoundedContext và packageName của nó được sử dụng để tổ chức mã được sinh ra, đảm bảo rằng các thành phần của các BoundedContext khác nhau được tách biệt một cách vật lý trên hệ thống tệp.

### 4.3 Cấu Trúc Aggregate

Trong DDSL, Aggregate được mô hình hóa như một cụm được gói gọn các đối tượng miền, được xác định bởi một Entity gốc (aggregate root) bắt buộc. Aggregate Root có một IdentityField duy nhất, xác định danh tính của nó. Lớp IdentityField hỗ trợ nhiều loại định danh khác nhau, bao gồm UUID, LONG, STRING, và CUSTOM, cùng với nhiều chiến lược sinh định danh như AUTO (do cơ sở dữ liệu quản lý), MANUAL (do ứng dụng cung cấp), SEQUENCE (sử dụng database sequence), và UUID_GENERATOR (sinh UUID từ ứng dụng).

Ngoài Entity gốc, một Aggregate có thể chứa các Entity con bổ sung, Value Object, các bất biến (Invariant) mô tả các quy tắc kinh doanh phải được duy trì, và các Factory để chỉ định cách tạo các instance mới một cách an toàn. Các bất biến (Invariant) được biểu diễn dưới dạng biểu thức logic cùng với thông báo lỗi được hiển thị khi bất biến bị vi phạm. Điều này cho phép xác định tại một nơi tập trung những hạn chế logic phải được bảo vệ bất cứ khi nào Aggregate được sửa đổi.

Bất kỳ Entity nào bên trong Aggregate có thể tham chiếu đến các Domain Event có thể được phát sinh bởi nó. Các tham chiếu này được chỉ định thông qua EventReference, mà bao gồm tên của sự kiện và một điều kiện kích hoạt (trigger condition) mô tả khi nào sự kiện sẽ được phát sinh. Điều này tạo cơ sở cho quá trình sinh mã để tạo ra các lệnh gọi phát sinh sự kiện trong các phương thức thích hợp.

### 4.4 Entity và Value Object

Entity trong DDSL được định nghĩa bởi sự có mặt của một IdentityField và một tập hợp các Field và Method. Tầm nhìn (visibility) của các trường được chỉ định (PUBLIC, PROTECTED, PACKAGE_PRIVATE, hoặc PRIVATE), cho phép kiểm soát chặt chẽ quy tắc truy cập theo nguyên tắc DDD. Mỗi Field có thể có một tập hợp các Constraint, chẳng hạn như NOT_NULL, MIN_LENGTH, MAX_LENGTH, REGEX, RANGE, UNIQUE, hoặc CUSTOM, xác định các ràng buộc dữ liệu cần được thực thi.

Value Object đại diện cho các đối tượng không có danh tính riêng, mà thay vào đó được định nghĩa bởi các giá trị của các trường của chúng. Khác với Entity, Value Object có thể chứa một tập hợp ValidationMethod, mỗi cái bao gồm một tập hợp ValidationRule. Mỗi quy tắc xác thực gồm một biểu thức logic và một thông báo lỗi, cho phép chỉ định các ràng buộc logic phức tạp hơn những gì có thể được thể hiện với các Constraint đơn giản.

### 4.5 Domain Services, Events và Repositories

Domain Service biểu diễn các hoạt động thành phố không liên quan tự nhiên với bất kỳ Entity hoặc Value Object cụ thể nào. Chúng có thể được định nghĩa là giao diện (interface) hoặc lớp cụ thể, và chứa một tập hợp các Method định nghĩa các hoạt động được cung cấp bởi dịch vụ. Domain Event là một khái niệm quan trọng trong DDD để ghi lại các sự kiện có ý nghĩa xảy ra trong miền. Trong DDSL, DomainEvent có chứa một trường aggregateId (xác định Aggregate phát sinh sự kiện), một trường occurredOn (ghi lại thời gian xảy ra), và một tập hợp các Field bổ sung chứa dữ liệu của sự kiện.

Repository định nghĩa giao diện để lưu trữ và truy cập Aggregate. Mỗi RepositoryInterface được liên kết với một loại Aggregate cụ thể và một loại định danh (idType). Các phương thức của repository được phân loại theo RepositoryMethodType, bao gồm FIND_BY_ID, FIND_ALL, FIND_BY_CRITERIA, SAVE, DELETE, COUNT, EXISTS, và CUSTOM. Phân loại này cho phép hệ thống sinh mã có thể tạo ra các triển khai phù hợp cho mỗi loại phương thức.

### 4.6 Factory, Specification và Application Service

Factory chỉ định cách tạo các instance của Aggregate phức tạp. Mỗi FactoryMethod chứa tham số, loại trả về, và thân mã tạo đối tượng theo một cách đảm bảo các bất biến được duy trì. Specification biểu diễn các tiêu chí phức tạp để tìm kiếm hoặc xác thực các đối tượng miền. Chúng có thể được sử dụng với các Repository để tìm kiếm các Aggregate thỏa mãn các điều kiện phức tạp.

Application Service điều phối logic xử lý các use case cấp ứng dụng. Chúng chứa một tập hợp các UseCase, mỗi cái biểu diễn một tương tác do người dùng hoặc hệ thống bên ngoài bắt đầu. Mỗi UseCase có các Parameter (đầu vào từ người gọi), một loại trả về, và một CodeBlock chứa thân thực hiện. Application Service cũng chứa một tập hợp các Dependency, đại diện cho các dịch vụ khác cần được tiêm (inject) vào để sử dụng.

### 4.7 Building Blocks: Fields, Methods và Parameters

Cấp độ chi tiết nhất của DDSL được cấu thành bởi các khối xây dựng cơ bản: Field, Method, Parameter, CodeBlock, và Constraint. Một Field biểu diễn một thuộc tính dữ liệu có tên, kiểu (JavaType), tầm nhìn, và một tập hợp các Constraint. Nó cũng có thể có một giá trị mặc định và có thể được đánh dấu là final hoặc nullable.

Một Method biểu diễn một hoạt động có thể được gọi trên một Entity, Value Object, hoặc Domain Service. Ngoài tên, loại trả về, và danh sách Parameter, Method cũng chứa thân mã được biểu diễn dưới dạng CodeBlock, một tập hợp các ngoại lệ có thể ném (throwsExceptions), và một tham chiếu đến Entity chứa nó (entityContext). Parameter biểu diễn một đối số của một phương thức, có tên, kiểu, và một cờ chỉ định xem nó có thể nhận giá trị null hay không.

CodeBlock là một tập hợp các câu lệnh được biểu diễn dưới dạng chuỗi. Cách tiếp cận này cho phép các nhà phát triển nhập các đoạn mã Java tùy chỉnh trực tiếp vào mô hình DDSL, với hiểu rằng các đoạn này sẽ được chèn vào các lớp được sinh ra. Điều này cung cấp sự linh hoạt để xử lý logic phức tạp không thể hoặc cồng kềnh để biểu diễn bằng cách sử dụng các khái niệm cấp cao hơn của DDSL.

### 4.8 Quan Hệ và Tính Bao Hàm

Quan hệ giữa các phần tử trong AST của DDSL được xác định rõ ràng thông qua một cây bao hàm (containment hierarchy). DomainModel chứa một hoặc nhiều BoundedContext. Mỗi BoundedContext chứa một hoặc nhiều Aggregate, cùng với các khái niệm DDD khác như ValueObject, DomainService, DomainEvent, RepositoryInterface, Factory, Specification, và ApplicationService. Mỗi Aggregate lại chứa một Entity gốc bắt buộc, có thể là Entity con bổ sung, ValueObject, Invariant, và Factory.

Ở cấp chi tiết hơn, mỗi Entity chứa một IdentityField bắt buộc, một tập hợp các Field, một tập hợp các Method, và các tham chiếu đến Domain Event có thể được phát sinh. Mỗi Method chứa một tập hợp các Parameter và một CodeBlock. Mỗi Field có thể có một tập hợp các Constraint. Cấu trúc bao hàm này phản ánh cách các khái niệm DDD tự nhiên được lồng vào nhau, với các Entity chứa các trường và phương thức, các Aggregate chứa các Entity, và các BoundedContext chứa các Aggregate.

### 4.9 Ví Dụ Minh Họa: Hệ Thống Quản Lý Blog

Để minh họa các khái niệm được trình bày ở trên, chúng ta xem xét một ví dụ đơn giản là hệ thống quản lý blog (Blog Management System). Hệ thống này bao gồm một DomainModel có tên "BlogSystem" với gói cơ sở "com.example.blog", chứa một BoundedContext có tên "BlogManagement".

Trong BoundedContext này, Aggregate chính là "Post", có Aggregate Root là Entity "Post" được xác định bởi IdentityField có tên "postId" thuộc loại UUID được sinh bằng UUID_GENERATOR. Entity "Post" có một tập hợp các Field bao gồm "title" (chuỗi với ràng buộc MIN_LENGTH "5" và MAX_LENGTH "200"), "content" (chuỗi), "author" (chuỗi), "createdAt" (LocalDateTime, final), và "status" (PostStatus với giá trị mặc định "DRAFT").

Aggregate "Post" cũng chứa một ValueObject có tên "PostStatus", định nghĩa các trạng thái hợp lệ mà một Post có thể có. ValueObject này chứa một Field có tên "value" thuộc loại String, cùng với một ValidationMethod có tên "validateStatus" chứa các quy tắc xác thực chỉ định rằng giá trị phải là một trong ba giá trị: DRAFT, PUBLISHED, hoặc ARCHIVED.

Entity "Post" có hai Method: "publish()" và "unpublish()". Phương thức "publish()" kiểm tra xem trạng thái hiện tại có phải là DRAFT, nếu không nó ném ra một ngoại lệ IllegalStateException. Nếu trạng thái là DRAFT, phương thức thay đổi trạng thái thành PUBLISHED và phát sinh một sự kiện PostPublishedEvent. Entity "Post" tham chiếu đến hai Domain Event có thể được phát sinh: "PostPublishedEvent" (kích hoạt khi status == PUBLISHED) và "PostContentUpdatedEvent" (kích hoạt khi content thay đổi).

Aggregate chứa một Factory có tên "PostFactory" với một FactoryMethod có tên "createNewPost(title, content, author)". Phương thức này nhận ba tham số (title, content, author) và trả về một Post được tạo ra một cách an toàn với postId được sinh ngẫu nhiên, trạng thái DRAFT, và thời gian tạo là thời điểm hiện tại.

BoundedContext cũng định nghĩa một RepositoryInterface có tên "PostRepository" quản lý Aggregate "Post" với loại định danh UUID. Repository này định nghĩa năm phương thức: "findById(postId: UUID): Optional<Post>" (loại FIND_BY_ID), "findAll(): List<Post>" (loại FIND_ALL), "findByAuthor(author: String): List<Post>" (loại FIND_BY_CRITERIA), "save(post: Post): void" (loại SAVE), và "delete(postId: UUID): void" (loại DELETE).

DomainEvent "PostPublishedEvent" được định nghĩa với aggregateId là "postId: UUID", occurredOn là "publishedAt: LocalDateTime", và hai Field bổ sung: "title: String" và "authorId: String".

Một DomainService có tên "PostSearchService" được định nghĩa là một giao diện với hai Method: "searchByKeyword(keyword: String): List<Post>" và "findRecentPosts(limit: int): List<Post>".

Cuối cùng, một ApplicationService có tên "PublishPostService" được định nghĩa với hai phụ thuộc: "postRepository" (loại PostRepository) và "eventPublisher" (loại EventPublisher), cả hai được tiêm qua constructor. ApplicationService này chứa một UseCase có tên "publishPost(publishPostCommand)" nhận một tham số "command: PublishPostCommand" và thực hiện ba bước: (1) lấy Post từ repository bằng postId từ command, (2) gọi phương thức publish() trên Post để phát sinh sự kiện, (3) lưu Post đã thay đổi trở lại repository.

### 4.10 Xác Thực và Sinh Mã

Quá trình xử lý một mô hình DDSL bao gồm ba giai đoạn chính: phân tích cú pháp (parsing), xác thực (validation), và sinh mã (code generation). Ở giai đoạn phân tích cú pháp, tệp DDSL được phân tích cú pháp thành một Abstract Syntax Tree. Ở giai đoạn xác thực, hệ thống kiểm tra các tính bất biến như định danh duy nhất bên trong một BoundedContext, các tham chiếu Aggregate hợp lệ, tính đầy đủ của các bất biến (invariant), và tính hợp lệ của các quy tắc xác thực. Nếu phát hiện ra lỗi, hệ thống báo cáo chi tiết vị trí lỗi, thông báo lỗi, mức độ nghiêm trọng, và các gợi ý sửa chữa.

Ở giai đoạn sinh mã, hệ thống sử dụng mô hình Visitor để duyệt qua cây cú pháp trừu tượng, tạo ra mã Java cho các lớp Entity, Value Object, Repository, và các thành phần khác được xác định trong mô hình. Quá trình này đảm bảo rằng mã được sinh ra tuân thủ các nguyên tắc DDD và phản ánh chính xác các khái niệm được mô tả trong mô hình DDSL.

---

## Phần Thảo Luận về Cú Pháp Trừu Tượng

### Lợi Ích của Việc Chính Thức Hóa Cú Pháp Trừu Tượng

Cú pháp trừu tượng được trình bày ở trên cung cấp một cơ sở vững chắc cho việc chính thức hóa các khái niệm DDD. Bằng cách biểu diễn tất cả các khái niệm DDD dưới dạng các nút trong một cây cú pháp trừu tượng, DDSL cho phép áp dụng các kỹ thuật từ lĩnh vực xử lý ngôn ngữ hình thức, chẳng hạn như xác thực cú pháp, phân tích ngữ nghĩa, và sinh mã. Điều này so sánh với các phương pháp trước đây để mô tả các mô hình DDD, thường dựa trên mô tả tự do hoặc các biểu đồ hình ảnh.

Chính thức hóa này cũng cho phép các công cụ không chỉ xác thực cú pháp mà còn xác thực các tính bất biến ngữ nghĩa, chẳng hạn như việc xảy ra của các tham chiếu Aggregate không hợp lệ hoặc các định danh trùng lặp trong một BoundedContext. Hơn nữa, bằng cách xác định một Abstract Syntax Tree rõ ràng, chúng ta tạo điều kiện để sinh mã có sự kiểm soát chi tiết, cho phép tạo ra các triển khai cụ thể của các khái niệm DDD mà sẽ nhất quán với mô hình.

### Giới Hạn và Hướng Phát Triển Tương Lai

Mặc dù cú pháp trừu tượng được trình bày là toàn diện, nhưng vẫn có những khía cạnh của DDD không được hỗ trợ đầy đủ bởi phiên bản hiện tại. Ví dụ, các khái niệm chuyên sâu như Module và Subdomains được đề cập trong công trình của Evans được nhưng không có đại diện rõ ràng trong mô hình AST. Tương tự, các Pattern như Saga cho các giao dịch phân tán hoặc CQRS (Command Query Responsibility Segregation) có thể được hỗ trợ tốt hơn bằng các phần mở rộng trong tương lai.

Hướng phát triển trong tương lai bao gồm mở rộng AST để hỗ trợ các khái niệm DDD bổ sung, cải thiện hệ thống xác thực để phát hiện các tính không nhất quán tinh tế hơn, và tăng cường chất lượng của mã được sinh ra để bao gồm các câu hỏi về hiệu suất và khả năng duy trì.

---

**Ghi chú:** Nội dung trên được trình bày theo phong cách học thuật chính thức, phù hợp cho công bố trong các tạp chí khoa học hoặc hội nghị học thuật về phần mềm. Bạn có thể điều chỉnh độ chi tiết, thêm các tham chiếu cụ thể, hoặc thay đổi tonality theo các yêu cầu của bài báo của bạn.

