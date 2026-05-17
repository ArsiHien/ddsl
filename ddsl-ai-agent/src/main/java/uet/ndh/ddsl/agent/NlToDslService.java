package uet.ndh.ddsl.agent;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    private final CompiledGraph<DdslState> compiledGraph;

    public NlToDslService(StateGraph<DdslState> stateGraph) throws Exception {
        this.compiledGraph = stateGraph.compile(CompileConfig.builder()
                .recursionLimit(80)
                .build());
        log.info("NlToDslService: compiled LangGraph4j pipeline (orchestrator → retriever/synthesizer/judge)");
    }

    /**
     * Execute the NL → DDSL pipeline with per-agent retry logic.
     *
     * @param naturalLanguageInput the raw user input
     * @param maxRetriesPerAgent   max retries per agent (default 2)
     * @return the result containing DSL and metadata
     */
    public NlToDslResult translate(String naturalLanguageInput, int maxRetriesPerAgent) {
        return translateWithTrace(naturalLanguageInput, maxRetriesPerAgent).result();
    }

    /**
     * Execute the NL -> DDSL pipeline and return detailed node-level diagnostics.
     */
    public NlToDslDetailedResult translateWithTrace(String naturalLanguageInput, int maxRetriesPerAgent) {
        log.info("Starting NL→DSL translation ({} chars, maxRetriesPerAgent={})",
                naturalLanguageInput.length(), maxRetriesPerAgent);

        Map<String, Object> initialState = new HashMap<>();
        initialState.put("userInput", naturalLanguageInput);
        initialState.put("maxRetries", maxRetriesPerAgent);

        long startedAt = System.nanoTime();
        long previousNodeAt = startedAt;
        List<NlToDslTraceStep> trace = new ArrayList<>();
        Map<String, Long> nodeTimeMs = new LinkedHashMap<>();
        Map<String, Integer> nodeIterations = new LinkedHashMap<>();
        DdslState finalState = null;

        try {
            int index = 0;

            for (var nodeOutput : compiledGraph.stream(initialState)) {
                log.debug("Graph node output: {}", nodeOutput.node());
                finalState = nodeOutput.state();
                long now = System.nanoTime();
                long elapsedMs = (now - startedAt) / 1_000_000;
                long deltaMs = (now - previousNodeAt) / 1_000_000;
                previousNodeAt = now;

                String nodeName = String.valueOf(nodeOutput.node());
                nodeTimeMs.merge(nodeName, deltaMs, Long::sum);
                nodeIterations.merge(nodeName, 1, Integer::sum);
                trace.add(NlToDslTraceStep.from(++index, nodeName, finalState, elapsedMs, deltaMs));
            }

            if (finalState == null) {
                long totalTimeMs = (System.nanoTime() - startedAt) / 1_000_000;
                return new NlToDslDetailedResult(
                        NlToDslResult.failure("Graph produced no output"),
                        trace,
                        totalTimeMs,
                        nodeTimeMs,
                        nodeIterations,
                        List.of(),
                        List.of()
                );
            }

            long totalTimeMs = (System.nanoTime() - startedAt) / 1_000_000;
            return new NlToDslDetailedResult(
                    NlToDslResult.from(finalState),
                    trace,
                    totalTimeMs,
                    nodeTimeMs,
                    nodeIterations,
                    finalState.synthesisArtifacts(),
                    finalState.judgeArtifacts()
            );

        } catch (Exception e) {
            log.error("NL→DSL translation failed", e);
            long totalTimeMs = (System.nanoTime() - startedAt) / 1_000_000;
            NlToDslResult result = finalState != null
                    ? new NlToDslResult(
                            false,
                            finalState.currentDsl(),
                            List.of(e.getMessage()),
                            finalState.retrieverRetries(),
                            finalState.synthesizerRetries(),
                            finalState.retrievalQuality(),
                            finalState.compilerFeedback()
                    )
                    : NlToDslResult.failure(e.getMessage());
            return new NlToDslDetailedResult(
                    result,
                    trace,
                    totalTimeMs,
                    nodeTimeMs,
                    nodeIterations,
                    finalState != null ? finalState.synthesisArtifacts() : List.of(),
                    finalState != null ? finalState.judgeArtifacts() : List.of()
            );
        }
    }

    public NlToDslResult translate(String naturalLanguageInput) {
        return translate(naturalLanguageInput, 2);
    }
}
