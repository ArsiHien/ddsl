package uet.ndh.ddsl.agent;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that compiles and executes the NL → DDSL pipeline.
 * Entry point for REST controllers.
 */
@Service
@Slf4j
public class NlToDslService {

    private final CompiledGraph<EnhancedDslState> compiledGraph;

    public NlToDslService(StateGraph<EnhancedDslState> stateGraph) throws Exception {
        this.compiledGraph = stateGraph.compile();
        log.info("NlToDslService: compiled LangGraph4j pipeline (retriever → normalizer → synthesizer → judge)");
    }

    /**
     * Execute the NL → DDSL pipeline with per-agent retry logic.
     *
     * @param naturalLanguageInput the raw user input
     * @param maxRetriesPerAgent   max retries per agent (default 2)
     * @return the result containing DSL and metadata
     */
    public NlToDslResult translate(String naturalLanguageInput, int maxRetriesPerAgent) {
        log.info("Starting NL→DSL translation ({} chars, maxRetriesPerAgent={})",
                naturalLanguageInput.length(), maxRetriesPerAgent);

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(EnhancedDslState.KEY_USER_INPUT, naturalLanguageInput);
        initialState.put(EnhancedDslState.KEY_MAX_RETRIES_PER_AGENT, maxRetriesPerAgent);

        try {
            EnhancedDslState finalState = null;

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
        return translate(naturalLanguageInput, 2);
    }

    /**
     * Result record for NL → DSL translation.
     */
    public record NlToDslResult(
            boolean success,
            String dsl,
            List<String> errors,
            int retrieverRetries,
            int normalizerRetries,
            int synthesizerRetries,
            double retrievalQuality,
            String errorStage
    ) {
        public static NlToDslResult from(EnhancedDslState state) {
            return new NlToDslResult(
                    state.isSuccessful(),
                    state.isSuccessful() ? state.finalDsl() : state.currentDsl(),
                    state.errorLogs(),
                    state.retrieverRetries(),
                    state.normalizerRetries(),
                    state.synthesizerRetries(),
                    state.retrievalQuality(),
                    state.errorStage()
            );
        }

        public static NlToDslResult failure(String errorMessage) {
            return new NlToDslResult(
                    false, "", List.of(errorMessage), 0, 0, 0, 0.0, "SERVICE"
            );
        }
    }
}
