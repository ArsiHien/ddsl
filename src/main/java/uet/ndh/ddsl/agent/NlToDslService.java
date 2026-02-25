package uet.ndh.ddsl.agent;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service that compiles and executes the NL → DDSL self-healing pipeline.
 * Entry point for REST controllers.
 */
@Service
@Slf4j
public class NlToDslService {

    private final CompiledGraph<DslState> compiledGraph;

    public NlToDslService(StateGraph<DslState> stateGraph) throws Exception {
        this.compiledGraph = stateGraph.compile();
        log.info("NlToDslService: compiled LangGraph4j graph (synthesizer → judge)");
    }

    /**
     * Execute the NL → DDSL pipeline with self-healing retry loop.
     *
     * @param naturalLanguageInput the raw user input in any language
     * @param maxRetries           max validation-retry loops (default 3)
     * @return the result containing the final DSL and metadata
     */
    public NlToDslResult translate(String naturalLanguageInput, int maxRetries) {
        log.info("Starting NL→DSL translation ({} chars, maxRetries={})",
                naturalLanguageInput.length(), maxRetries);

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(DslState.KEY_USER_INPUT, naturalLanguageInput);
        initialState.put(DslState.KEY_MAX_RETRIES, maxRetries);

        try {
            DslState finalState = null;

            for (var nodeOutput : compiledGraph.stream(initialState)) {
                log.debug("Graph node output: {}", nodeOutput.node());
                finalState = nodeOutput.state();
            }

            if (finalState == null) {
                return NlToDslResult.failure("Graph produced no output");
            }

            return NlToDslResult.from(finalState);

        } catch (Exception e) {
            log.error("NL→DSL translation failed", e);
            return NlToDslResult.failure(e.getMessage());
        }
    }

    public NlToDslResult translate(String naturalLanguageInput) {
        return translate(naturalLanguageInput, 3);
    }

    // ── Result record ───────────────────────────────────────────────────

    public record NlToDslResult(
            boolean success,
            String dsl,
            List<String> errors,
            int retries
    ) {
        public static NlToDslResult from(DslState state) {
            return new NlToDslResult(
                    state.isSuccessful(),
                    state.currentDsl(),
                    state.errorLogs(),
                    state.retryCount()
            );
        }

        public static NlToDslResult failure(String errorMessage) {
            return new NlToDslResult(false, "", List.of(errorMessage), 0);
        }
    }
}
