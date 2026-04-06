package uet.ndh.ddsl.codegen;

import org.junit.jupiter.api.*;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DDSL → Java code generation pipeline.
 *
 * These tests verify end-to-end correctness: DDSL source → parse → codegen → Java source.
 * No Spring context required — exercises parser + PoetModule directly.
 */
class CodeGenTest {

    private static final String BASE_PACKAGE = "com.example.domain";

    // ─── Helpers ────────────────────────────────────────────────────────

    private DomainModel parse(String ddsl) throws ParseException {
        var parser = new DdslParser(ddsl, "<test>");
        return parser.parse();
    }

    private List<CodeArtifact> generate(String ddsl) throws ParseException {
        DomainModel model = parse(ddsl);
        var poet = new PoetModule(BASE_PACKAGE);
        return poet.generateFromModel(model);
    }

    private CodeArtifact findByName(List<CodeArtifact> artifacts, String typeName) {
        return artifacts.stream()
                .filter(a -> a.typeName().equals(typeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No artifact named '" + typeName + "'. Found: " +
                                artifacts.stream().map(CodeArtifact::typeName).toList()));
    }

    // ─── 1. Basic aggregate generation ──────────────────────────────────

    @Test
    @DisplayName("Generate aggregate root class with identity field")
    void aggregateWithIdentity() throws ParseException {
        String ddsl = """
                BoundedContext Shop {
                    domain {
                        Aggregate Product {
                            productId: UUID @identity
                            name: String
                            price: Decimal
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        assertFalse(artifacts.isEmpty(), "Should produce at least one artifact");
        CodeArtifact root = findByName(artifacts, "Product");
        assertEquals(CodeArtifact.ArtifactType.AGGREGATE_ROOT, root.artifactType());
        assertTrue(root.sourceCode().contains("productId"),
                "Generated code should contain the identity field");
        assertTrue(root.sourceCode().contains("name"),
                "Generated code should contain regular fields");
    }

    // ─── 2. Entity generation ───────────────────────────────────────────

    @Test
    @DisplayName("Generate standalone entity with fields and constraints")
    void entityWithConstraints() throws ParseException {
        String ddsl = """
                BoundedContext Shop {
                    domain {
                        Aggregate Cart {
                            cartId: UUID @identity

                            Entity CartItem {
                                itemId: UUID @identity
                                quantity: Int @min(1)
                                unitPrice: Decimal @min(0)
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        CodeArtifact cartItem = findByName(artifacts, "CartItem");
        assertEquals(CodeArtifact.ArtifactType.ENTITY, cartItem.artifactType());
        assertTrue(cartItem.sourceCode().contains("itemId"));
        assertTrue(cartItem.sourceCode().contains("quantity"));
    }

    // ─── 3. Value object generation ─────────────────────────────────────

    @Test
    @DisplayName("Generate value object with multiple fields")
    void valueObjectGeneration() throws ParseException {
        String ddsl = """
                BoundedContext Shipping {
                    domain {
                        ValueObject Address {
                            street: String @required
                            city: String @required
                            zipCode: String @required
                            country: String @required
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        CodeArtifact address = findByName(artifacts, "Address");
        assertEquals(CodeArtifact.ArtifactType.VALUE_OBJECT, address.artifactType());
        String src = address.sourceCode();
        assertTrue(src.contains("street"));
        assertTrue(src.contains("city"));
        assertTrue(src.contains("zipCode"));
        assertTrue(src.contains("country"));
    }

    // ─── 4. Domain event generation ─────────────────────────────────────

    @Test
    @DisplayName("Generate domain events with fields")
    void domainEventGeneration() throws ParseException {
        String ddsl = """
                BoundedContext Orders {
                    events {
                        DomainEvent OrderPlaced {
                            orderId: UUID
                            customerId: UUID
                            orderDate: DateTime
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        CodeArtifact event = findByName(artifacts, "OrderPlaced");
        assertEquals(CodeArtifact.ArtifactType.DOMAIN_EVENT, event.artifactType());
        assertTrue(event.sourceCode().contains("orderId"));
        assertTrue(event.sourceCode().contains("customerId"));
    }

    // ─── 5. Domain service generation ───────────────────────────────────

    @Test
    @DisplayName("Generate domain service")
    void domainServiceGeneration() throws ParseException {
        String ddsl = """
                BoundedContext Pricing {
                    domain {
                        ValueObject Product {
                            name: String
                            basePrice: Decimal
                        }
                        
                        ValueObject Quantity {
                            value: Int @min(0)
                        }
                        
                        ValueObject Price {
                            amount: Decimal
                        }
                        
                        ValueObject DiscountRate {
                            rate: Decimal
                        }
                        
                        DomainService PricingService {
                            when calculating price with product and quantity:
                            then:
                                - set result to product
                            when applying discount with price and discountRate:
                            then:
                                - set result to price
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        CodeArtifact service = findByName(artifacts, "PricingService");
        assertEquals(CodeArtifact.ArtifactType.DOMAIN_SERVICE, service.artifactType());
        assertTrue(service.sourceCode().contains("calculatingPrice") 
                || service.sourceCode().contains("calculating"),
                "Generated service should contain an operation method");
    }

    // ─── 6. Repository generation ───────────────────────────────────────

    @Test
    @DisplayName("Generate repository interface with custom queries")
    void repositoryGeneration() throws ParseException {
        String ddsl = """
                BoundedContext Orders {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            status: String
                        }
                    }
                    repositories {
                        Repository OrderRepository for Order {
                            findById(id: UUID): Order?
                            findByStatus(status: String): List<Order>
                            save(order: Order): Void
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        CodeArtifact repo = findByName(artifacts, "OrderRepository");
        assertEquals(CodeArtifact.ArtifactType.REPOSITORY, repo.artifactType());
        assertTrue(repo.sourceCode().contains("findById"));
        assertTrue(repo.sourceCode().contains("findByStatus"));
    }

    // ─── 7. Behavior / operations generation ────────────────────────────

    @Test
    @DisplayName("Generate aggregate with operations / behavior blocks")
    void aggregateWithOperations() throws ParseException {
        String ddsl = """
                BoundedContext Blog {
                    domain {
                        Aggregate Post {
                            postId: UUID @identity
                            title: String
                            status: String
                            
                            operations {
                                when publishing post:
                                require that:
                                    - title is not empty
                                then:
                                    - set status to "PUBLISHED"
                                emit PostPublished with postId
                            }
                        }
                    }
                    events {
                        DomainEvent PostPublished {
                            postId: UUID
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        CodeArtifact post = findByName(artifacts, "Post");
        // The generated code should contain some form of the operation
        assertNotNull(post.sourceCode());
        assertTrue(post.sourceCode().length() > 50,
                "Generated aggregate should have non-trivial source code");
    }

    // ─── 8. Multi-bounded-context model ─────────────────────────────────

    @Test
    @DisplayName("Generate artifacts from a model with multiple bounded contexts")
    void multipleBoundedContexts() throws ParseException {
        String ddsl = """
                BoundedContext Catalog {
                    domain {
                        Aggregate Product {
                            productId: UUID @identity
                            name: String
                        }
                    }
                }
                BoundedContext Inventory {
                    domain {
                        Aggregate Stock {
                            stockId: UUID @identity
                            quantity: Int
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("Product")),
                "Should generate Product from Catalog context");
        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("Stock")),
                "Should generate Stock from Inventory context");
    }

    // ─── 9. Package naming ──────────────────────────────────────────────

    @Test
    @DisplayName("Generated artifacts use the configured base package")
    void packageNaming() throws ParseException {
        String ddsl = """
                BoundedContext Banking {
                    domain {
                        Aggregate Account {
                            accountId: UUID @identity
                            balance: Decimal
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        for (CodeArtifact a : artifacts) {
            assertTrue(a.packageName().startsWith(BASE_PACKAGE),
                    "Package " + a.packageName() + " should start with " + BASE_PACKAGE);
        }
    }

    // ─── 10. Artifact metadata correctness ──────────────────────────────

    @Test
    @DisplayName("CodeArtifact fields are consistent (fileName, path)")
    void artifactMetadata() throws ParseException {
        String ddsl = """
                BoundedContext HR {
                    domain {
                        ValueObject Email {
                            address: String @required
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact email = findByName(artifacts, "Email");

        assertEquals("Email.java", email.fileName());
        assertTrue(email.relativePath().endsWith("Email.java"));
        assertTrue(email.fullyQualifiedName().contains("Email"));
    }

    // ─── 11. Empty context produces no artifacts ────────────────────────

    @Test
    @DisplayName("Bounded context with only ubiquitous-language produces no domain artifacts")
    void emptyDomainBlock() throws ParseException {
        String ddsl = """
                BoundedContext Empty {
                    ubiquitous-language {
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        assertTrue(artifacts.isEmpty(), "Empty context should produce 0 artifacts");
    }

    // ─── 12. Parse failure propagation ──────────────────────────────────

    @Test
    @DisplayName("Invalid DDSL throws ParseException")
    void invalidDdslThrows() {
        String ddsl = "this is totally not valid DDSL }{}{";
        assertThrows(Exception.class, () -> generate(ddsl));
    }

    // ─── 13. Artifact type distribution ─────────────────────────────────

    @Test
    @DisplayName("E-commerce model produces correct artifact type distribution")
    void artifactTypeDistribution() throws ParseException {
        String ddsl = """
                BoundedContext ECommerce {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            lineItems: List<OrderItem>
                            status: String
                            
                            Entity OrderItem {
                                itemId: UUID @identity
                                product: String
                                quantity: Int @min(1)
                            }
                        }
                        
                        ValueObject Money {
                            amount: Decimal @min(0)
                            currency: String
                        }
                    }
                    events {
                        DomainEvent OrderPlaced {
                            orderId: UUID
                            orderDate: DateTime
                        }
                    }
                    repositories {
                        Repository OrderRepository for Order {
                            findById(id: UUID): Order?
                            save(order: Order): Void
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        Map<CodeArtifact.ArtifactType, List<CodeArtifact>> byType = artifacts.stream()
                .collect(Collectors.groupingBy(CodeArtifact::artifactType));

        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.AGGREGATE_ROOT),
                "Should contain AGGREGATE_ROOT");
        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.ENTITY),
                "Should contain ENTITY");
        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.VALUE_OBJECT),
                "Should contain VALUE_OBJECT");
        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.DOMAIN_EVENT),
                "Should contain DOMAIN_EVENT");
        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.REPOSITORY),
                "Should contain REPOSITORY");
    }

    // ─── 14. Generated code is compilable Java (basic check) ────────────

    @Test
    @DisplayName("Generated code looks like valid Java (has class/record/interface)")
    void generatedCodeIsJavaLike() throws ParseException {
        String ddsl = """
                BoundedContext CRM {
                    domain {
                        Aggregate Customer {
                            customerId: UUID @identity
                            name: String
                            emailAddress: String
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        for (CodeArtifact a : artifacts) {
            String src = a.sourceCode();
            assertTrue(
                    src.contains("class ") || src.contains("record ") || src.contains("interface "),
                    "Artifact " + a.typeName() + " should contain a class/record/interface declaration"
            );
            assertTrue(src.contains("package "),
                    "Artifact " + a.typeName() + " should have a package declaration");
        }
    }

    @Test
    @DisplayName("Generate helper classes for temporal, state machine, and validation features")
    void helperClassesGeneratedFromExtendedFeatures() throws ParseException {
        String ddsl = """
                BoundedContext Orders {
                    domain {
                        state machine for OrderStatus {
                            states:
                                - Pending (initial)
                                - Confirmed
                            transitions:
                                - Pending -> Confirmed: only within 24 hours of created at
                        }

                        Aggregate Order {
                            orderId: UUID @identity
                            createdAt: DateTime

                            operations {
                                when validating order with orderId:
                                require that:
                                    - createdAt is before now
                                collect all errors:
                                    - orderId is present, otherwise "Order id required"
                                fail if any errors
                                then:
                                    - set createdAt to now
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("TemporalPredicates")),
                "Should generate TemporalPredicates helper");
        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("ValidationResult")),
                "Should generate ValidationResult helper");
        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("ValidationException")),
                "Should generate ValidationException helper");
        assertTrue(artifacts.stream().anyMatch(a -> a.typeName().equals("IllegalStateTransitionException")),
                "Should generate IllegalStateTransitionException helper");
    }

    @Test
    @DisplayName("Match on String field generates quoted case labels")
    void matchOnStringFieldUsesQuotedCases() throws ParseException {
        String ddsl = """
                BoundedContext Shop {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            customerTier: String

                            operations {
                                when computing discount with customerTier:
                                given:
                                    - discount as match customerTier with:
                                        GOLD: 0.15
                                        SILVER: 0.10
                                        default: 0.00
                                then:
                                    - set customerTier to "GOLD"
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact order = findByName(artifacts, "Order");
        String src = order.sourceCode();

        assertTrue(src.contains("case \"GOLD\" ->") && src.contains("case \"SILVER\" ->"),
                "String match target should generate quoted switch labels");
    }

    @Test
    @DisplayName("Match on enum-typed field generates enum case labels")
    void matchOnEnumFieldUsesEnumCases() throws ParseException {
        String ddsl = """
                BoundedContext Shop {
                    domain {
                        state machine for OrderStatus {
                            states:
                                - Pending (initial)
                                - Confirmed
                            transitions:
                                - Pending -> Confirmed: always
                        }

                        Aggregate Order {
                            orderId: UUID @identity
                            status: OrderStatus

                            operations {
                                when evaluating status with status:
                                given:
                                    - score as match status with:
                                        Pending: 1
                                        Confirmed: 2
                                        default: 0
                                then:
                                    - set status to status
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact order = findByName(artifacts, "Order");
        String src = order.sourceCode();

        assertTrue(src.contains("case PENDING ->") && src.contains("case CONFIRMED ->"),
                "Enum match target should generate enum-style switch labels");
        assertFalse(src.contains("case \"PENDING\" ->"),
                "Enum match target should not generate quoted labels");
    }

    @Test
    @DisplayName("First-class enum declarations generate Java enum artifacts")
    void firstClassEnumDeclarationGeneratesEnumArtifact() throws ParseException {
        String ddsl = """
                BoundedContext Shop {
                    domain {
                        Enum CustomerTier {
                            GOLD
                            SILVER
                            BRONZE
                        }

                        Aggregate Order {
                            orderId: UUID @identity
                            customerTier: CustomerTier

                            operations {
                                when evaluating tier with customerTier:
                                given:
                                    - discount as match customerTier with:
                                        GOLD: 20
                                        SILVER: 10
                                        default: 0
                                then:
                                    - set customerTier to customerTier
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);

        CodeArtifact tierEnum = findByName(artifacts, "CustomerTier");
        assertEquals(CodeArtifact.ArtifactType.ENUM, tierEnum.artifactType(),
                "Enum declaration should generate ENUM artifact type");
        assertTrue(tierEnum.sourceCode().contains("enum CustomerTier"),
                "Generated source should declare CustomerTier enum");
        assertTrue(tierEnum.sourceCode().contains("GOLD") && tierEnum.sourceCode().contains("SILVER"),
                "Generated enum should contain declared constants");

        CodeArtifact order = findByName(artifacts, "Order");
        String orderSrc = order.sourceCode();
        assertTrue(orderSrc.contains("case GOLD ->") && orderSrc.contains("case SILVER ->"),
                "Match over enum-typed field should use enum case labels");
        assertFalse(orderSrc.contains("case \"GOLD\" ->"),
                "Enum match labels should not be quoted");
    }

    @Test
    @DisplayName("Then-clause method calls are emitted as Java method invocations")
    void thenClauseMethodCallsGenerateJavaInvocations() throws ParseException {
        String ddsl = """
                BoundedContext Calls {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            customerEmail: String

                            operations {
                                when notifying customer:
                                then:
                                    - notify customerEmail

                                when persisting order with order:
                                then:
                                    - save order to orderRepository
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact order = findByName(artifacts, "Order");
        String src = order.sourceCode();

        assertTrue(src.contains("notify(customerEmail);") || src.contains("notify(this.customerEmail);"),
                "Natural method call should be emitted as notify(...) invocation");
        assertTrue(src.contains("orderRepository.save(order);") || src.contains("save(order);"),
                "Save statement should be emitted as repository.save(entity)");
    }

    @Test
    @DisplayName("Explicit call syntax generates readable Java invocations")
    void explicitCallSyntaxGeneratesReadableInvocations() throws ParseException {
        String ddsl = """
                BoundedContext Calls {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            customerEmail: String

                            operations {
                                when processing order with order:
                                then:
                                    - call sendOrderConfirmationEmail(customerEmail)
                                    - call orderRepository.save(order)
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact order = findByName(artifacts, "Order");
        String src = order.sourceCode();

        assertTrue(src.contains("sendOrderConfirmationEmail(customerEmail);")
                        || src.contains("sendOrderConfirmationEmail(this.customerEmail);"),
                "Explicit local call should be emitted directly");
        assertTrue(src.contains("orderRepository.save(order);")
                        || src.contains("orderRepository.save(this.order);")
                        || src.contains("save(order);"),
                "Explicit receiver call should be emitted directly");
    }

    @Test
    @DisplayName("Execute and external call phrase syntax generate expected Java invocations")
    void executeAndExternalCallPhraseSyntaxGenerateInvocations() throws ParseException {
        String ddsl = """
                BoundedContext Calls {
                    domain {
                        Aggregate Reservation {
                            reservationId: UUID @identity
                            amount: Decimal
                            guestId: UUID

                            operations {
                                when placing reservation:
                                then:
                                    - execute apply seasonal discount
                                    - call PaymentService to process payment with amount, guestId
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact reservation = findByName(artifacts, "Reservation");
        String src = reservation.sourceCode();

        assertTrue(src.contains("this.applySeasonalDiscount();") || src.contains("applySeasonalDiscount();"),
                "Execute phrase syntax should generate internal method invocation");
        assertTrue(src.contains("paymentService.processPayment(amount, guestId);")
                        || src.contains("paymentService.processPayment(this.amount, this.guestId);")
                        || src.contains("processPayment(amount, guestId);"),
                "External call phrase syntax should generate service invocation");
    }

    @Test
    @DisplayName("Decimal fields map to BigDecimal and comparison uses compareTo")
    void decimalComparisonUsesBigDecimalCompareTo() throws ParseException {
        String ddsl = """
                BoundedContext Billing {
                    domain {
                        Aggregate Invoice {
                            invoiceId: UUID @identity
                            amount: Decimal

                            operations {
                                when validating amount:
                                require that:
                                    - amount > 0
                                then:
                                    - set amount to amount
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact invoice = findByName(artifacts, "Invoice");
        String src = invoice.sourceCode();

        assertTrue(src.contains("BigDecimal amount"),
                "Decimal field should map to java.math.BigDecimal");
        assertTrue(src.contains("this.amount.compareTo(") && src.contains(") > 0"),
            "Decimal comparisons should use BigDecimal.compareTo");
    }

    @Test
    @DisplayName("Untyped behavior parameter inferred from generic field keeps type arguments")
    void inferredBehaviorParameterPreservesGenericTypeArguments() throws ParseException {
        String ddsl = """
                BoundedContext Orders {
                    domain {
                        Aggregate Order {
                            orderId: UUID @identity
                            orderItems: List<OrderItem>

                            Entity OrderItem {
                                itemId: UUID @identity
                                quantity: Int
                            }

                            operations {
                                when recomputing with orderItems:
                                then:
                                    - set orderItems to orderItems
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact order = findByName(artifacts, "Order");
        String src = order.sourceCode();

        assertTrue(src.contains("recomputing(List<OrderItem> orderItems)")
                        || src.contains("recomputingWithOrderItems(List<OrderItem> orderItems)"),
                "Inferred List parameter type should preserve generic argument");
    }

    @Test
    @DisplayName("Regex format validation emits correctly escaped Java string literal")
    void regexFormatValidationEmitsEscapedPattern() throws ParseException {
        String ddsl = """
                BoundedContext Accounts {
                    domain {
                        Aggregate Customer {
                            customerId: UUID @identity
                            email: String

                            operations {
                                when validating email:
                                require that:
                                    - email is valid EMAIL format
                                then:
                                    - set email to email
                            }
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact customer = findByName(artifacts, "Customer");
        String src = customer.sourceCode();

        assertTrue(
            src.contains("Pattern.matches(")
                || src.contains("email.matches(")
                || src.contains("this.email.matches("),
            "Generated validation should emit a regex-based match check");
    }

    @Test
    @DisplayName("Aggregate root includes getters for non-identity fields")
    void aggregateRootGeneratesGettersForFields() throws ParseException {
        String ddsl = """
                BoundedContext CRM {
                    domain {
                        Aggregate Customer {
                            customerId: UUID @identity
                            email: String
                            tier: String
                        }
                    }
                }
                """;

        List<CodeArtifact> artifacts = generate(ddsl);
        CodeArtifact customer = findByName(artifacts, "Customer");
        String src = customer.sourceCode();

        assertTrue(src.contains("public String getEmail()"),
                "Aggregate root should generate getter for regular field email");
        assertTrue(src.contains("public String getTier()"),
                "Aggregate root should generate getter for regular field tier");
    }
}
