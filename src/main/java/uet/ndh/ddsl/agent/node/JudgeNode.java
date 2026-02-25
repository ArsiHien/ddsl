package uet.ndh.ddsl.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.DslState;
import uet.ndh.ddsl.mcp.DdslValidationTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Judge Node</b> — the validation gate of the pipeline.
 * <p>
 * Responsibilities:
 * <ol>
 *   <li>Takes the current DSL draft from state.</li>
 *   <li>Calls the {@link DdslValidationTool} (DDSL parser) to check syntax.</li>
 *   <li>If the parser reports <b>no errors</b> → marks the state as successful.</li>
 *   <li>If errors exist → stores the error logs in state for the Synthesizer
 *       to consume on the next retry iteration.</li>
 * </ol>
 *
 * The graph's conditional edge inspects {@code isSuccessful} and {@code retryCount}
 * to decide whether to finish or loop back to the Synthesizer.
 */
@Component
public class JudgeNode implements NodeAction<DslState> {

    private static final Logger log = LoggerFactory.getLogger(JudgeNode.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DdslValidationTool validationTool;

    public JudgeNode(DdslValidationTool validationTool) {
        this.validationTool = validationTool;
    }

    @Override
    public Map<String, Object> apply(DslState state) throws Exception {
        log.info("JudgeNode: validating DSL (attempt={})", state.retryCount());

        String dsl = state.currentDsl();
        Map<String, Object> updates = new HashMap<>();

        if (dsl == null || dsl.isBlank()) {
            updates.put(DslState.KEY_ERROR_LOGS, List.of("DSL draft is empty"));
            updates.put(DslState.KEY_IS_SUCCESSFUL, false);
            return updates;
        }

        // Call the DDSL parser via the MCP tool
        String jsonResult = validationTool.validateDSL(dsl);
        Map<String, Object> parsed = parseResult(jsonResult);

        boolean valid = Boolean.TRUE.equals(parsed.get("valid"));

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) parsed.getOrDefault("errors", List.of());

        if (valid) {
            log.info("JudgeNode: DSL is valid ✓");
            updates.put(DslState.KEY_IS_SUCCESSFUL, true);
            updates.put(DslState.KEY_ERROR_LOGS, List.of());
        } else {
            log.info("JudgeNode: {} error(s) found — will route back to Synthesizer", errors.size());
            updates.put(DslState.KEY_IS_SUCCESSFUL, false);
            updates.put(DslState.KEY_ERROR_LOGS, errors);
        }

        return updates;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResult(String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("JudgeNode: failed to parse validation JSON", e);
            return Map.of(
                    "valid", false,
                    "errors", List.of("Failed to parse validation output: " + json)
            );
        }
    }
}
