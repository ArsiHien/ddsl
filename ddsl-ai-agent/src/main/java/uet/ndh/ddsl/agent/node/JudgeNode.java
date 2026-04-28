package uet.ndh.ddsl.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.EnhancedDslState;
import uet.ndh.ddsl.mcp.DdslValidationTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Judge Agent</b> — stateless validation gate.
 * <p>
 * Validates DDSL syntax using the parser. Stateless - no retries.
 * <p>
 * Returns validation result with error logs for synthesizer retry loop.
 */
@Component
public class JudgeNode implements NodeAction<EnhancedDslState> {

    private static final Logger log = LoggerFactory.getLogger(JudgeNode.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DdslValidationTool validationTool;

    public JudgeNode(DdslValidationTool validationTool) {
        this.validationTool = validationTool;
    }

    @Override
    public Map<String, Object> apply(EnhancedDslState state) {
        log.info("JudgeNode: validating DSL");

        String dsl = state.currentDsl();
        Map<String, Object> updates = new HashMap<>();

        if (dsl == null || dsl.isBlank()) {
            updates.put(EnhancedDslState.KEY_ERROR_LOGS, List.of("DSL draft is empty"));
            updates.put(EnhancedDslState.KEY_IS_SUCCESSFUL, false);
            return updates;
        }

        try {
            String jsonResult = validationTool.validateDSL(dsl);
            Map<String, Object> parsed = parseResult(jsonResult);

            boolean valid = Boolean.TRUE.equals(parsed.get("valid"));
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) parsed.getOrDefault("errors", List.of());

            if (valid) {
                log.info("JudgeNode: DSL is valid ✓");
                updates.put(EnhancedDslState.KEY_IS_SUCCESSFUL, true);
                updates.put(EnhancedDslState.KEY_FINAL_DSL, dsl);
                updates.put(EnhancedDslState.KEY_ERROR_LOGS, List.of());
            } else {
                log.info("JudgeNode: {} error(s) found", errors.size());
                updates.put(EnhancedDslState.KEY_IS_SUCCESSFUL, false);
                updates.put(EnhancedDslState.KEY_ERROR_LOGS, errors);
            }

            return updates;

        } catch (Exception e) {
            log.error("JudgeNode: validation failed", e);
            updates.put(EnhancedDslState.KEY_IS_SUCCESSFUL, false);
            updates.put(EnhancedDslState.KEY_ERROR_LOGS, List.of("Validation error: " + e.getMessage()));
            return updates;
        }
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
