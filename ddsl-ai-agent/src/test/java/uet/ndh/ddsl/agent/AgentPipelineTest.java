package uet.ndh.ddsl.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import uet.ndh.ddsl.agent.node.JudgeNode;
import uet.ndh.ddsl.mcp.DdslValidationTool;

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
            List<String> errors = (List<String>) result.get("errors");
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e ->
                            e.toLowerCase().contains("error") || e.toLowerCase().contains("line")),
                    "Error messages should contain 'error' or 'line'");
        }
    }

    // ─── JudgeNode tests ────────────────────────────────────────────────

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