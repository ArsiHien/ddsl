package uet.ndh.ddsl.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import uet.ndh.ddsl.agent.node.JudgeNode;
import uet.ndh.ddsl.mcp.DdslValidationTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the LLM agent pipeline components.
 *
 * <p>The SynthesizerNode requires a live ChatClient + VectorStore (Gemini + Qdrant)
 * and is therefore tested only as an integration test. The JudgeNode, however,
 * only depends on the DDSL parser and can be tested fully offline.</p>
 *
 * <p>These tests also cover the DslState and DdslValidationTool independently.</p>
 */
class AgentPipelineTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ─── DslState tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("DslState")
    class DslStateTests {

        @Test
        @DisplayName("Default state has sane initial values")
        void defaultValues() {
            var state = new DslState(Map.of());
            assertEquals("", state.userInput());
            assertEquals("", state.currentDsl());
            assertEquals(List.of(), state.errorLogs());
            assertEquals(0, state.retryCount());
            assertFalse(state.isSuccessful());
            assertEquals(3, state.maxRetries());
        }

        @Test
        @DisplayName("State reflects provided values")
        void customValues() {
            var state = new DslState(Map.of(
                    DslState.KEY_USER_INPUT, "create an order",
                    DslState.KEY_CURRENT_DSL, "BoundedContext {}",
                    DslState.KEY_RETRY_COUNT, 2,
                    DslState.KEY_IS_SUCCESSFUL, true,
                    DslState.KEY_MAX_RETRIES, 5
            ));

            assertEquals("create an order", state.userInput());
            assertEquals("BoundedContext {}",  state.currentDsl());
            assertEquals(2, state.retryCount());
            assertTrue(state.isSuccessful());
            assertEquals(5, state.maxRetries());
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
            // Parser error messages should include location info
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
            var state = new DslState(Map.of(
                    DslState.KEY_CURRENT_DSL, """
                            BoundedContext MyCtx {
                                domain {
                                    Aggregate Order {
                                        @identity orderId: UUID
                                        total: Decimal
                                    }
                                }
                            }
                            """,
                    DslState.KEY_RETRY_COUNT, 1
            ));

            Map<String, Object> updates = judgeNode.apply(state);

            assertTrue((Boolean) updates.get(DslState.KEY_IS_SUCCESSFUL));
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) updates.get(DslState.KEY_ERROR_LOGS);
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Invalid DSL → isSuccessful=false, error logs populated")
        void invalidDslFailsJudge() throws Exception {
            var state = new DslState(Map.of(
                    DslState.KEY_CURRENT_DSL, "This is not DDSL at all {{{",
                    DslState.KEY_RETRY_COUNT, 1
            ));

            Map<String, Object> updates = judgeNode.apply(state);

            assertFalse((Boolean) updates.get(DslState.KEY_IS_SUCCESSFUL));
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) updates.get(DslState.KEY_ERROR_LOGS);
            assertFalse(errors.isEmpty(), "Should have at least one error");
        }

        @Test
        @DisplayName("Empty DSL draft → isSuccessful=false")
        void emptyDslFailsJudge() throws Exception {
            var state = new DslState(Map.of(
                    DslState.KEY_CURRENT_DSL, "",
                    DslState.KEY_RETRY_COUNT, 0
            ));

            Map<String, Object> updates = judgeNode.apply(state);

            assertFalse((Boolean) updates.get(DslState.KEY_IS_SUCCESSFUL));
        }

        @Test
        @DisplayName("Null DSL draft → isSuccessful=false")
        void nullDslFailsJudge() throws Exception {
            // DslState defaults currentDsl to "" but let's be defensive
            var state = new DslState(Map.of());

            Map<String, Object> updates = judgeNode.apply(state);

            assertFalse((Boolean) updates.get(DslState.KEY_IS_SUCCESSFUL));
        }
    }

    // ─── NlToDslResult tests ────────────────────────────────────────────

    @Nested
    @DisplayName("NlToDslService.NlToDslResult")
    class NlToDslResultTests {

        @Test
        @DisplayName("failure() creates a failed result")
        void failureResult() {
            var result = NlToDslService.NlToDslResult.failure("something broke");
            assertFalse(result.success());
            assertEquals("", result.dsl());
            assertEquals(List.of("something broke"), result.errors());
            assertEquals(0, result.retries());
        }

        @Test
        @DisplayName("from() extracts fields from DslState")
        void fromState() {
            var state = new DslState(Map.of(
                    DslState.KEY_IS_SUCCESSFUL, true,
                    DslState.KEY_CURRENT_DSL, "BoundedContext OK {}",
                    DslState.KEY_ERROR_LOGS, List.of(),
                    DslState.KEY_RETRY_COUNT, 2
            ));

            var result = NlToDslService.NlToDslResult.from(state);
            assertTrue(result.success());
            assertEquals("BoundedContext OK {}", result.dsl());
            assertTrue(result.errors().isEmpty());
            assertEquals(2, result.retries());
        }
    }

    // ─── Self-healing loop simulation ───────────────────────────────────

    @Test
    @DisplayName("Simulate self-healing: bad DSL → JudgeNode marks failure → state updates")
    void selfHealingLoopSimulation() throws Exception {
        var judgeNode = new JudgeNode(new DdslValidationTool());

        // --- Iteration 1: bad DSL ---
        var state1 = new DslState(Map.of(
                DslState.KEY_CURRENT_DSL, "BoundedContext { }",  // missing name
                DslState.KEY_RETRY_COUNT, 1,
                DslState.KEY_MAX_RETRIES, 3
        ));
        Map<String, Object> out1 = judgeNode.apply(state1);
        assertFalse((Boolean) out1.get(DslState.KEY_IS_SUCCESSFUL));

        // --- Iteration 2: fixed DSL ---
        var fixedState = new HashMap<String, Object>();
        fixedState.put(DslState.KEY_CURRENT_DSL, """
                BoundedContext FixedCtx {
                    domain {
                        Aggregate FixedAgg {
                            @identity id: UUID
                        }
                    }
                }
                """);
        fixedState.put(DslState.KEY_RETRY_COUNT, 2);
        fixedState.put(DslState.KEY_MAX_RETRIES, 3);

        Map<String, Object> out2 = judgeNode.apply(new DslState(fixedState));
        assertTrue((Boolean) out2.get(DslState.KEY_IS_SUCCESSFUL),
                "Fixed DSL should pass validation");
    }
}
