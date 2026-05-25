package uet.ndh.ddsl.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import uet.ndh.ddsl.agent.dto.PlanStep;
import uet.ndh.ddsl.agent.node.JudgeNode;
import uet.ndh.ddsl.agent.node.OrchestratorNode;
import uet.ndh.ddsl.mcp.DdslValidationTool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentPipelineTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ─── DdslState tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("DdslState")
    class DdslStateTests {

        @Test
        @DisplayName("Initial state has sane default values")
        void defaultValues() {
            var state = new DdslState(Map.ofEntries(
                    Map.entry("userInput", ""),
                    Map.entry("retrievedContext", ""),
                    Map.entry("currentDsl", ""),
                    Map.entry("finalDsl", ""),
                    Map.entry("errorLogs", List.of()),
                    Map.entry("retryCount", 0),
                    Map.entry("retrieverRetries", 0),
                    Map.entry("synthesizerRetries", 0),
                    Map.entry("maxRetries", 3),
                    Map.entry("retrievalQuality", 0.0),
                    Map.entry("isSuccessful", false),
                    Map.entry("compilerFeedback", "")
            ));
            assertEquals("", state.userInput());
            assertEquals("", state.retrievedContext());
            assertEquals("", state.currentDsl());
            assertEquals("", state.finalDsl());
            assertEquals(0, state.retrieverRetries());
            assertEquals(0, state.synthesizerRetries());
            assertEquals(3, state.maxRetries());
            assertEquals(0.0, state.retrievalQuality());
            assertFalse(state.isSuccessful());
            assertEquals(List.of(), state.errorLogs());
            assertEquals("", state.compilerFeedback());
        }

        @Test
        @DisplayName("State reflects provided values")
        void customValues() {
var state = new DdslState(Map.ofEntries(
                    Map.entry("userInput", "create an order"),
                    Map.entry("retrievedContext", "retrieved context"),
                    Map.entry("currentDsl", "BoundedContext {}"),
                    Map.entry("finalDsl", "final dsl"),
                    Map.entry("retrieverRetries", 1),
                    Map.entry("synthesizerRetries", 2),
                    Map.entry("maxRetries", 3),
                    Map.entry("retrievalQuality", 0.85),
                    Map.entry("isSuccessful", true),
                    Map.entry("errorLogs", List.of("error1")),
                    Map.entry("compilerFeedback", "compiler feedback")
            ));

            assertEquals("create an order", state.userInput());
            assertEquals("retrieved context", state.retrievedContext());
            assertEquals("BoundedContext {}", state.currentDsl());
            assertEquals("final dsl", state.finalDsl());
            assertEquals(1, state.retrieverRetries());
            assertEquals(2, state.synthesizerRetries());
            assertEquals(3, state.maxRetries());
            assertEquals(0.85, state.retrievalQuality());
            assertTrue(state.isSuccessful());
            assertEquals(List.of("error1"), state.errorLogs());
            assertEquals("compiler feedback", state.compilerFeedback());
        }

        @Test
        @DisplayName("from(Map) creates state from map")
        void fromMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("userInput", "test input");
            map.put("currentDsl", "DSL content");
            map.put("retrieverRetries", 1);
            map.put("synthesizerRetries", 2);
            map.put("isSuccessful", true);

            var state = DdslState.from(map);

            assertEquals("test input", state.userInput());
            assertEquals("DSL content", state.currentDsl());
            assertEquals(1, state.retrieverRetries());
            assertEquals(2, state.synthesizerRetries());
            assertTrue(state.isSuccessful());
        }
    }

    // ─── DdslValidationTool tests ───────────────────────────────────────

    @Nested
    @DisplayName("DdslValidationTool")
    class ValidationToolTests {

        private final DdslValidationTool tool = new DdslValidationTool();

        @Test
        @DisplayName("Valid DDSL returns valid=true")
        void validDsl() throws Exception {
            String ddsl = """
                    BoundedContext Test {
                        domain {
                            Aggregate Item {
                                @identity itemId: UUID
                                name: String
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Should report valid");
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Temporal now keyword in behavior assignment is valid")
        void temporalNowAssignmentIsValid() throws Exception {
            String ddsl = """
                    BoundedContext Logistics {
                        domain {
                            Aggregate Shipment {
                                @identity shipmentId: UUID
                                status: String
                                shippedAt: DateTime?

                                operations {
                                    when marking shipped:
                                        require that:
                                            - status is "CREATED"
                                        then:
                                            - set status to "SHIPPED"
                                            - set shippedAt to now
                                }
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Built-in temporal keyword now should be valid");
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Temporal now() call in behavior assignment is accepted for compatibility")
        void temporalNowCallAssignmentIsValid() throws Exception {
            String ddsl = """
                    BoundedContext Logistics {
                        domain {
                            Aggregate Shipment {
                                @identity shipmentId: UUID
                                status: String
                                shippedAt: DateTime?

                                operations {
                                    when marking shipped:
                                        require that:
                                            - status is "CREATED"
                                        then:
                                            - set status to "SHIPPED"
                                            - set shippedAt to now()
                                }
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Built-in temporal now() should be accepted");
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Specification supports non-temporal less-than field comparison")
        void specificationLessThanFieldComparisonIsValid() throws Exception {
            String ddsl = """
                    BoundedContext Learning {
                        domain {
                            Aggregate CourseOffering {
                                @identity offeringId: UUID
                                status: String
                                enrolledStudentsCount: Int
                                capacity: Int
                            }
                        }

                        specifications {
                            Specification OpenForEnrollment {
                                matches CourseOffering where:
                                    - status is "PUBLISHED"
                                    - enrolledStudentsCount is less than capacity
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Specification comparison should parse as a normal comparison");
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Course enrollment generated shape is valid")
        void courseEnrollmentGeneratedShapeIsValid() throws Exception {
            String ddsl = """
                    BoundedContext Learning {
                        domain {
                            Aggregate CourseOffering {
                                @identity offeringId: UUID
                                courseCode: String @required
                                instructorId: String @required
                                capacity: Int @min(1)
                                enrolledStudents: Set<StudentEnrollment>
                                waitlistedStudents: List<StudentEnrollment>
                                status: String

                                operations {
                                    when enrolling student with studentId and enrolledAt:
                                        require that:
                                            - status is "PUBLISHED"
                                            - studentId is required
                                            - enrolledStudents size < capacity
                                        then:
                                            - create StudentEnrollment
                                            - add StudentEnrollment to enrolledStudents
                                        emit StudentEnrolled with offeringId, studentId, enrolledAt
                                }
                            }

                            Entity StudentEnrollment {
                                @identity enrollmentId: UUID
                                studentId: String @required
                                enrolledAt: DateTime @required
                            }
                        }

                        events {
                            DomainEvent StudentEnrolled {
                                offeringId: UUID
                                studentId: String
                                enrolledAt: DateTime
                            }
                        }

                        specifications {
                            Specification OpenForEnrollment {
                                matches CourseOffering where:
                                    - status is "PUBLISHED"
                                    - enrolledStudents count < capacity
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Generated course enrollment DSL should be accepted: " + json);
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Order placement generated shape is valid")
        void orderPlacementGeneratedShapeIsValid() throws Exception {
            String ddsl = """
                    BoundedContext Ordering {
                        domain {
                            Aggregate Order {
                                orderId: UUID @identity
                                customerId: String @required
                                items: List<OrderItem>
                                totalAmount: Decimal @min(0)
                                status: String

                                operations {
                                    when placing order with customerId and items:
                                        require that:
                                            - customerId is required
                                            - items is required
                                        then:
                                            - calculate totalAmount from sum of item prices
                                            - set status to "PLACED"
                                        emit OrderPlaced with orderId and totalAmount
                                }
                            }

                            Entity OrderItem {
                                itemId: UUID @identity
                                productId: String @required
                                quantity: Int @min(1)
                                unitPrice: Decimal @min(0)
                            }
                        }

                        events {
                            DomainEvent OrderPlaced {
                                orderId: UUID
                                totalAmount: Decimal
                                occurredAt: DateTime
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Generated order placement DSL should be accepted: " + json);
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Natural increase and decrease then-statements are valid")
        void increaseDecreaseThenStatementsAreValid() throws Exception {
            String ddsl = """
                    BoundedContext Inventory {
                        domain {
                            Aggregate StockItem {
                                @identity itemId: UUID
                                quantity: Int
                                reservedQuantity: Int

                                operations {
                                    when reserving stock with requestedQuantity:
                                        require that:
                                            - requestedQuantity is required
                                        then:
                                            - decrease quantity by requestedQuantity
                                            - increase reservedQuantity by requestedQuantity
                                }
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Generated inventory adjustment DSL should be accepted: " + json);
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Invariant method call accepts lambda predicate argument")
        void invariantLambdaPredicateIsValid() throws Exception {
            String ddsl = """
                    BoundedContext Returns {
                        domain {
                            Aggregate ReturnAuthorization {
                                @identity returnId: UUID
                                items: List<ReturnItem>

                                invariants {
                                    "All items must have quantity greater than zero": items.every(item -> item.quantity > 0)
                                }
                            }

                            Entity ReturnItem {
                                @identity itemId: UUID
                                quantity: Int
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Generated return authorization invariant should be accepted: " + json);
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Factory for target type and from-parameter rule is valid")
        void factoryForTargetTypeWithParameterRuleIsValid() throws Exception {
            String ddsl = """
                    BoundedContext Procurement {
                        domain {
                            Aggregate PurchaseOrder {
                                @identity purchaseOrderId: UUID
                                supplierId: String
                                requesterId: String
                                status: String
                            }
                        }

                        factories {
                            Factory PurchaseOrderFactory for PurchaseOrder {
                                when creating PurchaseOrder from supplierId, requesterId, lines:
                                    require that:
                                        - lines is not empty
                                    then:
                                        - create PurchaseOrder with new purchaseOrderId, supplierId, requesterId, lines, status set to "DRAFT"
                                    return PurchaseOrder
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Generated purchase order factory DSL should be accepted: " + json);
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Factory creation can infer produced type before with-parameters")
        void factoryCreationInfersProducedTypeBeforeWithParameters() throws Exception {
            String ddsl = """
                    BoundedContext Procurement {
                        domain {
                            Aggregate PurchaseOrder {
                                @identity purchaseOrderId: UUID
                                supplierId: String
                                requesterId: String
                            }

                            Entity PurchaseOrderLine {
                                @identity lineId: UUID
                                itemCode: String
                            }
                        }

                        factories {
                            Factory PurchaseOrderFactory for PurchaseOrder {
                                when creating with supplierId: String, requesterId: String, lines: List<PurchaseOrderLine>:
                                    require that:
                                        - lines is not empty
                                    return PurchaseOrder
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Factory should infer PurchaseOrder from declaration: " + json);
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("DomainService then-clause accepts bare comparison expression")
        void domainServiceThenClauseAcceptsBareComparisonExpression() throws Exception {
            String ddsl = """
                    BoundedContext Procurement {
                        domain {
                            ValueObject Money {
                                amount: Decimal @min(0)
                                currency: String
                            }

                            DomainService ApprovalPolicy {
                                when checking approval requirement with totalAmount: Money, requesterId: String:
                                    then:
                                        - totalAmount > 1000
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "DomainService comparison then-statement should parse: " + json);
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Saved failing result artifacts now validate")
        void savedFailingResultArtifactsNowValidate() throws Exception {
            Path moduleRoot = Path.of("").toAbsolutePath().endsWith("ddsl-ai-agent")
                    ? Path.of("")
                    : Path.of("ddsl-ai-agent");
            List<Path> finalArtifacts = List.of(
                    moduleRoot.resolve("src/main/resources/result/M004-Account-Lifecycle-With-Invariants/final.ddsl"),
                    moduleRoot.resolve("src/main/resources/result/M007-Inventory-Adjustment-Service/final.ddsl"),
                    moduleRoot.resolve("src/main/resources/result/H002-Return-Merchandise-Authorization/final.ddsl"),
                    moduleRoot.resolve("src/main/resources/result/H005-Procurement-Purchase-Order/final.ddsl")
            );

            for (Path artifact : finalArtifacts) {
                String json = tool.validateDSL(Files.readString(artifact));
                Map<String, Object> result = mapper.readValue(json, Map.class);
                assertTrue((Boolean) result.get("valid"), artifact + " should validate after parser compatibility fixes: " + json);
                assertTrue(((List<?>) result.get("errors")).isEmpty(), artifact + " should have no errors");
            }
        }

        @Test
        @DisplayName("Loan state-machine prose conditions do not become unresolved identifiers")
        void loanStateMachineProseConditionsAreValid() throws Exception {
            String ddsl = """
                    BoundedContext Lending {
                        domain {
                            Aggregate LoanApplication {
                                @identity applicationId: UUID
                                applicantId: String @required
                                requestedAmount: Decimal @min(1000)
                                creditScore: Int @min(300) @max(850)
                                status: String
                                submittedAt: DateTime?
                                approvedAt: DateTime?

                                operations {
                                    when submit with applicantData:
                                        require that:
                                            - status is "DRAFT"
                                            - applicantData is complete
                                        then:
                                            - set status to "SUBMITTED"
                                            - set submittedAt to now
                                        emit LoanApplicationSubmitted

                                    when approve:
                                        require that:
                                            - status is "SUBMITTED"
                                            - creditScore >= 650
                                            - requestedAmount <= policyLimit
                                        then:
                                            - set status to "APPROVED"
                                            - set approvedAt to now
                                        emit LoanApplicationApproved

                                    when reject:
                                        require that:
                                            - status is "SUBMITTED"
                                            - risk is too high
                                        then:
                                            - set status to "REJECTED"
                                        emit LoanApplicationRejected
                                }
                            }
                        }
                    }
                    """;

            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertTrue((Boolean) result.get("valid"), "Generated H001 state-machine prose should be accepted: " + json);
            assertTrue(((List<?>) result.get("errors")).isEmpty(), "No errors expected");
        }

        @Test
        @DisplayName("Empty input returns error")
        void emptyInput() throws Exception {
            String json = tool.validateDSL("");
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertFalse((Boolean) result.get("valid"));
            assertFalse(((List<?>) result.get("errors")).isEmpty(),
                    "Should have at least one error for empty input");
        }

        @Test
        @DisplayName("Null input returns error")
        void nullInput() throws Exception {
            String json = tool.validateDSL(null);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertFalse((Boolean) result.get("valid"));
        }

        @Test
        @DisplayName("Syntactically invalid DDSL returns errors with location info")
        void invalidSyntax() throws Exception {
            String ddsl = "BoundedContext { missing name }}}";
            String json = tool.validateDSL(ddsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertFalse((Boolean) result.get("valid"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> errors = (List<Map<String, Object>>) result.get("errors");
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e ->
                            String.valueOf(e.get("message")).toLowerCase().contains("error")
                                    || String.valueOf(e.get("location")).contains(":")),
                    "Structured errors should contain message or location info");
        }
    }

    // ─── JudgeNode tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("OrchestratorNode")
    class OrchestratorNodeTests {

        private final OrchestratorNode orchestratorNode = new OrchestratorNode();

        @Test
        @DisplayName("Empty repair output does not overwrite an existing chunk")
        void emptyRepairDoesNotOverwriteExistingChunk() throws Exception {
            String existingDomainModel = """
                    Aggregate CourseOffering {
                        @identity offeringId: UUID
                        capacity: Int
                    }
                    """;
            List<PlanStep> plan = List.of(new PlanStep(
                    "DomainModel",
                    "Define domain model",
                    List.of(),
                    "RUNNING"
            ));
            var state = new DdslState(Map.ofEntries(
                    Map.entry(DdslState.KEY_USER_INPUT, "course enrollment"),
                    Map.entry(DdslState.KEY_CURRENT_DSL, ""),
                    Map.entry(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(plan)),
                    Map.entry(DdslState.KEY_CURRENT_CHUNK_ID, "DomainModel"),
                    Map.entry(DdslState.KEY_CURRENT_CHUNK_TASK, "Define domain model"),
                    Map.entry(DdslState.KEY_CHUNK_OUTPUTS, Map.of("DomainModel", existingDomainModel)),
                    Map.entry(DdslState.KEY_SYNTHESIS_MODE, "REPAIR"),
                    Map.entry(DdslState.KEY_ORCHESTRATOR_PHASE, "SYNTHESIZING"),
                    Map.entry(DdslState.KEY_REPAIR_HISTORY, List.of()),
                    Map.entry(DdslState.KEY_STRUCTURED_ERRORS, List.of()),
                    Map.entry(DdslState.KEY_ERROR_LOGS, List.of()),
                    Map.entry(DdslState.KEY_IS_SUCCESSFUL, false)
            ));

            Map<String, Object> updates = orchestratorNode.apply(state);

            @SuppressWarnings("unchecked")
            Map<String, String> outputs = (Map<String, String>) updates.get(DdslState.KEY_CHUNK_OUTPUTS);
            assertEquals(existingDomainModel, outputs.get("DomainModel"));
            assertTrue(String.valueOf(updates.get(DdslState.KEY_CURRENT_DSL)).contains("Aggregate CourseOffering"));
        }

        @Test
        @DisplayName("Legacy StateMachine chunk is normalized before merge")
        void legacyStateMachineChunkIsNormalizedBeforeMerge() throws Exception {
            String domainModel = """
                    Aggregate LoanApplication {
                        @identity applicationId: UUID
                        status: String
                        creditScore: Int
                        requestedAmount: Decimal
                    }
                    """;
            String legacyStateMachine = """
                    StateMachine LoanApplicationStatus {
                        initial: DRAFT
                        final: REJECTED, WITHDRAWN
                        transitions:
                            - from DRAFT to SUBMITTED when: applicantDataComplete
                            - from SUBMITTED to APPROVED when: creditScore >= 650 and requestedAmount <= policyLimit
                            - from SUBMITTED to REJECTED when: riskTooHigh
                            - from DRAFT to WITHDRAWN
                    }
                    """;
            List<PlanStep> plan = List.of(new PlanStep(
                    "StateMachines",
                    "Define state machine",
                    List.of("DomainModel"),
                    "RUNNING"
            ));
            var state = new DdslState(Map.ofEntries(
                    Map.entry(DdslState.KEY_USER_INPUT, "BoundedContext Lending state machine"),
                    Map.entry(DdslState.KEY_CURRENT_DSL, legacyStateMachine),
                    Map.entry(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(plan)),
                    Map.entry(DdslState.KEY_CURRENT_CHUNK_ID, "StateMachines"),
                    Map.entry(DdslState.KEY_CURRENT_CHUNK_TASK, "Define state machine"),
                    Map.entry(DdslState.KEY_CHUNK_OUTPUTS, Map.of("DomainModel", domainModel)),
                    Map.entry(DdslState.KEY_SYNTHESIS_MODE, "GENERATE"),
                    Map.entry(DdslState.KEY_ORCHESTRATOR_PHASE, "SYNTHESIZING"),
                    Map.entry(DdslState.KEY_REPAIR_HISTORY, List.of()),
                    Map.entry(DdslState.KEY_STRUCTURED_ERRORS, List.of()),
                    Map.entry(DdslState.KEY_ERROR_LOGS, List.of()),
                    Map.entry(DdslState.KEY_IS_SUCCESSFUL, false)
            ));

            Map<String, Object> updates = orchestratorNode.apply(state);
            String mergedDsl = String.valueOf(updates.get(DdslState.KEY_CURRENT_DSL));

            assertTrue(mergedDsl.contains("state machine for status"));
            assertFalse(mergedDsl.contains("StateMachine LoanApplicationStatus"));

            String json = new DdslValidationTool().validateDSL(mergedDsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);
            assertTrue((Boolean) result.get("valid"), "Normalized state machine should validate: " + json);
        }

        @Test
        @DisplayName("Bare state machine transitions receive default condition before merge")
        void bareStateMachineTransitionsReceiveDefaultConditionBeforeMerge() throws Exception {
            String domainModel = """
                    Aggregate LoanApplication {
                        @identity applicationId: UUID
                        status: String
                    }
                    """;
            String stateMachine = """
                    state machine for status {
                        states:
                            - DRAFT (initial)
                            - SUBMITTED
                            - APPROVED
                            - REJECTED (final)
                            - WITHDRAWN (final)
                        transitions:
                            - DRAFT -> SUBMITTED
                            - SUBMITTED -> APPROVED
                            - SUBMITTED -> REJECTED
                            - DRAFT -> WITHDRAWN
                            - SUBMITTED -> WITHDRAWN
                    }
                    """;
            List<PlanStep> plan = List.of(new PlanStep(
                    "StateMachines",
                    "Define state machine",
                    List.of("DomainModel"),
                    "RUNNING"
            ));
            var state = new DdslState(Map.ofEntries(
                    Map.entry(DdslState.KEY_USER_INPUT, "BoundedContext Lending state machine"),
                    Map.entry(DdslState.KEY_CURRENT_DSL, stateMachine),
                    Map.entry(DdslState.KEY_PLAN_GRAPH, PlanStep.toMaps(plan)),
                    Map.entry(DdslState.KEY_CURRENT_CHUNK_ID, "StateMachines"),
                    Map.entry(DdslState.KEY_CURRENT_CHUNK_TASK, "Define state machine"),
                    Map.entry(DdslState.KEY_CHUNK_OUTPUTS, Map.of("DomainModel", domainModel)),
                    Map.entry(DdslState.KEY_SYNTHESIS_MODE, "REPAIR"),
                    Map.entry(DdslState.KEY_ORCHESTRATOR_PHASE, "SYNTHESIZING"),
                    Map.entry(DdslState.KEY_REPAIR_HISTORY, List.of()),
                    Map.entry(DdslState.KEY_STRUCTURED_ERRORS, List.of()),
                    Map.entry(DdslState.KEY_ERROR_LOGS, List.of()),
                    Map.entry(DdslState.KEY_IS_SUCCESSFUL, false)
            ));

            Map<String, Object> updates = orchestratorNode.apply(state);
            String mergedDsl = String.valueOf(updates.get(DdslState.KEY_CURRENT_DSL));

            assertTrue(mergedDsl.contains("- DRAFT -> SUBMITTED: always"));
            assertTrue(mergedDsl.contains("- SUBMITTED -> WITHDRAWN: always"));

            String json = new DdslValidationTool().validateDSL(mergedDsl);
            Map<String, Object> result = mapper.readValue(json, Map.class);
            assertTrue((Boolean) result.get("valid"), "Bare transition normalization should validate: " + json);
        }
    }

    @Nested
    @DisplayName("JudgeNode")
    class JudgeNodeTests {

        private final JudgeNode judgeNode = new JudgeNode(new DdslValidationTool());

        @Test
        @DisplayName("Valid DSL → isSuccessful=true, empty error logs")
        void validDslPassesJudge() throws Exception {
            var state = new DdslState(Map.ofEntries(
                    Map.entry("userInput", "user input"),
                    Map.entry("retrievedContext", "retrieved context"),
                    Map.entry("currentDsl", """
                            BoundedContext MyCtx {
                                domain {
                                    Aggregate Order {
                                        @identity orderId: UUID
                                        total: Decimal
                                    }
                                }
                            }
                            """),
                    Map.entry("finalDsl", ""),
                    Map.entry("retrieverRetries", 0),
                    Map.entry("synthesizerRetries", 1),
                    Map.entry("maxRetries", 2),
                    Map.entry("retrievalQuality", 0.8),
                    Map.entry("isSuccessful", false),
                    Map.entry("errorLogs", List.of()),
                    Map.entry("compilerFeedback", "")
            ));

            Map<String, Object> updates = judgeNode.apply(state);

            assertTrue((Boolean) updates.get("isSuccessful"));
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) updates.get("errorLogs");
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Invalid DSL → isSuccessful=false, error logs populated")
        void invalidDslFailsJudge() throws Exception {
            var state = new DdslState(Map.ofEntries(
                    Map.entry("userInput", "user input"),
                    Map.entry("retrievedContext", "retrieved context"),
                    Map.entry("currentDsl", "This is not DDSL at all {{{"),
                    Map.entry("finalDsl", ""),
                    Map.entry("retrieverRetries", 0),
                    Map.entry("synthesizerRetries", 1),
                    Map.entry("maxRetries", 2),
                    Map.entry("retrievalQuality", 0.8),
                    Map.entry("isSuccessful", false),
                    Map.entry("errorLogs", List.of()),
                    Map.entry("compilerFeedback", "")
            ));

            Map<String, Object> updates = judgeNode.apply(state);

            assertFalse((Boolean) updates.get("isSuccessful"));
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) updates.get("errorLogs");
            assertFalse(errors.isEmpty(), "Should have at least one error");
        }

        @Test
        @DisplayName("Empty DSL draft → isSuccessful=false")
        void emptyDslFailsJudge() throws Exception {
            var state = new DdslState(Map.ofEntries(
                    Map.entry("userInput", "user input"),
                    Map.entry("retrievedContext", "retrieved context"),
                    Map.entry("currentDsl", ""),
                    Map.entry("finalDsl", ""),
                    Map.entry("retrieverRetries", 0),
                    Map.entry("synthesizerRetries", 0),
                    Map.entry("maxRetries", 2),
                    Map.entry("retrievalQuality", 0.8),
                    Map.entry("isSuccessful", false),
                    Map.entry("errorLogs", List.of()),
                    Map.entry("compilerFeedback", "")
            ));

            Map<String, Object> updates = judgeNode.apply(state);

            assertFalse((Boolean) updates.get("isSuccessful"));
        }
    }

    // ─── NlToDslResult tests ────────────────────────────────────────────

    @Nested
    @DisplayName("NlToDslResult")
    class NlToDslResultTests {

        @Test
        @DisplayName("failure() creates a failed result")
        void failureResult() {
            var result = NlToDslResult.failure("something broke");
            assertFalse(result.success());
            assertEquals("", result.dsl());
            assertEquals(List.of("something broke"), result.errors());
            assertEquals(0, result.retrieverRetries());
            assertEquals(0, result.synthesizerRetries());
            assertEquals(0.0, result.retrievalQuality());
            assertEquals("", result.compilerFeedback());
        }

        @Test
        @DisplayName("from() extracts fields from DdslState")
        void fromState() {
            var state = new DdslState(Map.ofEntries(
                    Map.entry("userInput", "user input"),
                    Map.entry("retrievedContext", "retrieved context"),
                    Map.entry("currentDsl", "current dsl"),
                    Map.entry("finalDsl", "BoundedContext OK {}"),
                    Map.entry("retrieverRetries", 1),
                    Map.entry("synthesizerRetries", 1),
                    Map.entry("maxRetries", 2),
                    Map.entry("retrievalQuality", 0.85),
                    Map.entry("isSuccessful", true),
                    Map.entry("errorLogs", List.of()),
                    Map.entry("compilerFeedback", "compiler feedback")
            ));

            var result = NlToDslResult.from(state);
            assertTrue(result.success());
            assertEquals("BoundedContext OK {}", result.dsl());
            assertTrue(result.errors().isEmpty());
            assertEquals(1, result.retrieverRetries());
            assertEquals(1, result.synthesizerRetries());
            assertEquals(0.85, result.retrievalQuality());
            assertEquals("compiler feedback", result.compilerFeedback());
        }

        @Test
        @DisplayName("from() returns currentDsl when not successful")
        void fromStateNotSuccessful() {
var state = new DdslState(Map.ofEntries(
                    Map.entry("userInput", "user input"),
                    Map.entry("retrievedContext", "retrieved context"),
                    Map.entry("currentDsl", "current dsl"),
                    Map.entry("finalDsl", "final dsl"),
                    Map.entry("retrieverRetries", 0),
                    Map.entry("synthesizerRetries", 0),
                    Map.entry("maxRetries", 2),
                    Map.entry("retrievalQuality", 0.5),
                    Map.entry("isSuccessful", false),
                    Map.entry("errorLogs", List.of("error")),
                    Map.entry("compilerFeedback", "compiler feedback")
            ));

            var result = NlToDslResult.from(state);
            assertFalse(result.success());
            assertEquals("current dsl", result.dsl());
            assertEquals(List.of("error"), result.errors());
        }
    }

    // ─── Self-healing loop simulation ───────────────────────────────────

    @Test
    @DisplayName("Simulate self-healing: bad DSL → JudgeNode marks failure → state updates")
    void selfHealingLoopSimulation() throws Exception {
        var judgeNode = new JudgeNode(new DdslValidationTool());

        var state1 = new DdslState(Map.ofEntries(
                Map.entry("userInput", "user input"),
                Map.entry("retrievedContext", "context"),
                Map.entry("currentDsl", "BoundedContext { }"),
                Map.entry("finalDsl", ""),
                Map.entry("retrieverRetries", 0),
                Map.entry("synthesizerRetries", 1),
                Map.entry("maxRetries", 3),
                Map.entry("retrievalQuality", 0.5),
                Map.entry("isSuccessful", false),
                Map.entry("errorLogs", List.of()),
                Map.entry("compilerFeedback", "")
        ));

        Map<String, Object> out1 = judgeNode.apply(state1);
        assertFalse((Boolean) out1.get("isSuccessful"));

        var fixedState = new DdslState(Map.ofEntries(
                Map.entry("userInput", "user input"),
                Map.entry("retrievedContext", "context"),
                Map.entry("currentDsl", """
                        BoundedContext FixedCtx {
                            domain {
                                Aggregate FixedAgg {
                                    @identity id: UUID
                                }
                            }
                        }
                        """),
                Map.entry("finalDsl", ""),
                Map.entry("retrieverRetries", 0),
                Map.entry("synthesizerRetries", 2),
                Map.entry("maxRetries", 3),
                Map.entry("retrievalQuality", 0.5),
                Map.entry("isSuccessful", false),
                Map.entry("errorLogs", List.of()),
                Map.entry("compilerFeedback", "")
        ));

        Map<String, Object> out2 = judgeNode.apply(fixedState);
        assertTrue((Boolean) out2.get("isSuccessful"),
                "Fixed DSL should pass validation");
    }
}
