package uet.ndh.ddsl.agent;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import uet.ndh.ddsl.agent.dto.JudgeErrorReport;
import uet.ndh.ddsl.agent.dto.PlanStep;

import java.util.List;
import java.util.Map;

/**
 * Lean state for the NL → DDSL self-healing pipeline.
 * <p>
 * Tracks exactly what the two-node graph needs:
 * <ul>
 *   <li>{@code userInput}    — original natural-language description</li>
 *   <li>{@code currentDsl}   — latest DSL draft (updated each iteration)</li>
 *   <li>{@code errorLogs}    — parser error messages from the last Judge run</li>
 *   <li>{@code retryCount}   — how many synthesize→judge loops have executed</li>
 *   <li>{@code isSuccessful}  — {@code true} when the parser accepts the DSL</li>
 *   <li>{@code maxRetries}   — ceiling for the self-correction loop (default 3)</li>
 * </ul>
 *
 * Uses LangGraph4j {@link AgentState} with last-value-wins channels.
 */
public class DdslState extends AgentState {

    // ── State Keys ──────────────────────────────────────────────────────
    public static final String KEY_USER_INPUT = "userInput";
    public static final String KEY_CURRENT_DSL = "currentDsl";
    public static final String KEY_ERROR_LOGS = "errorLogs";
    public static final String KEY_RETRY_COUNT = "retryCount";
    public static final String KEY_IS_SUCCESSFUL = "isSuccessful";
    public static final String KEY_MAX_RETRIES = "maxRetries";
    public static final String KEY_RETRIEVER_RETRIES = "retrieverRetries";
    public static final String KEY_SYNTHESIZER_RETRIES = "synthesizerRetries";
    public static final String KEY_RETRIEVAL_QUALITY = "retrievalQuality";
    public static final String KEY_COMPILER_FEEDBACK = "compilerFeedback";
    public static final String KEY_FINAL_DSL = "finalDsl";
    public static final String KEY_RETRIEVED_CONTEXT = "retrievedContext";
    public static final String KEY_ORCHESTRATOR_ROUTE = "orchestratorRoute";
    public static final String KEY_ORCHESTRATOR_PHASE = "orchestratorPhase";
    public static final String KEY_PLAN_GRAPH = "planGraph";
    public static final String KEY_CURRENT_CHUNK_ID = "currentChunkId";
    public static final String KEY_CURRENT_CHUNK_TASK = "currentChunkTask";
    public static final String KEY_CURRENT_CHUNK_CODE = "currentChunkCode";
    public static final String KEY_CHUNK_OUTPUTS = "chunkOutputs";
    public static final String KEY_CHUNK_CONTEXTS = "chunkContexts";
    public static final String KEY_CHUNK_LINE_MAP = "chunkLineMap";
    public static final String KEY_RETRIEVAL_QUERY = "retrievalQuery";
    public static final String KEY_SYNTHESIS_MODE = "synthesisMode";
    public static final String KEY_STRUCTURED_ERRORS = "structuredErrors";
    public static final String KEY_REPAIR_HISTORY = "repairHistory";
    public static final String KEY_SYNTHESIS_ARTIFACTS = "synthesisArtifacts";
    public static final String KEY_JUDGE_ARTIFACTS = "judgeArtifacts";

    /**
     * Channel schema — every key uses last-value-wins semantics.
     */
    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry(KEY_USER_INPUT, Channels.base(() -> "")),
            Map.entry(KEY_CURRENT_DSL, Channels.base(() -> "")),
            Map.entry(KEY_ERROR_LOGS, Channels.base(() -> List.<String>of())),
            Map.entry(KEY_RETRY_COUNT, Channels.base(() -> 0)),
            Map.entry(KEY_IS_SUCCESSFUL, Channels.base(() -> false)),
            Map.entry(KEY_MAX_RETRIES, Channels.base(() -> 3)),
            Map.entry(KEY_RETRIEVER_RETRIES, Channels.base(() -> 0)),
            Map.entry(KEY_SYNTHESIZER_RETRIES, Channels.base(() -> 0)),
            Map.entry(KEY_RETRIEVAL_QUALITY, Channels.base(() -> 0.0)),
            Map.entry(KEY_COMPILER_FEEDBACK, Channels.base(() -> "")),
            Map.entry(KEY_FINAL_DSL, Channels.base(() -> "")),
            Map.entry(KEY_RETRIEVED_CONTEXT, Channels.base(() -> "")),
            Map.entry(KEY_ORCHESTRATOR_ROUTE, Channels.base(() -> "")),
            Map.entry(KEY_ORCHESTRATOR_PHASE, Channels.base(() -> "")),
            Map.entry(KEY_PLAN_GRAPH, Channels.base(() -> List.<Map<String, Object>>of())),
            Map.entry(KEY_CURRENT_CHUNK_ID, Channels.base(() -> "")),
            Map.entry(KEY_CURRENT_CHUNK_TASK, Channels.base(() -> "")),
            Map.entry(KEY_CURRENT_CHUNK_CODE, Channels.base(() -> "")),
            Map.entry(KEY_CHUNK_OUTPUTS, Channels.base(() -> Map.<String, String>of())),
            Map.entry(KEY_CHUNK_CONTEXTS, Channels.base(() -> Map.<String, String>of())),
            Map.entry(KEY_CHUNK_LINE_MAP, Channels.base(() -> Map.<String, Map<String, Integer>>of())),
            Map.entry(KEY_RETRIEVAL_QUERY, Channels.base(() -> "")),
            Map.entry(KEY_SYNTHESIS_MODE, Channels.base(() -> "GENERATE")),
            Map.entry(KEY_STRUCTURED_ERRORS, Channels.base(() -> List.<Map<String, Object>>of())),
            Map.entry(KEY_REPAIR_HISTORY, Channels.base(() -> List.<Map<String, Object>>of())),
            Map.entry(KEY_SYNTHESIS_ARTIFACTS, Channels.base(() -> List.<Map<String, Object>>of())),
            Map.entry(KEY_JUDGE_ARTIFACTS, Channels.base(() -> List.<Map<String, Object>>of()))
    );

    /**
     * Factory method for LangGraph4j StateGraph compatibility.
     */
    public static DdslState from(Map<String, Object> initData) {
        return new DdslState(initData);
    }

    public DdslState(Map<String, Object> initData) {
        super(initData);
    }

    // ── Convenience Accessors ───────────────────────────────────────────

    public String userInput() {
        return this.<String>value(KEY_USER_INPUT).orElse("");
    }

    public String currentDsl() {
        return this.<String>value(KEY_CURRENT_DSL).orElse("");
    }

    public List<String> errorLogs() {
        return this.<List<String>>value(KEY_ERROR_LOGS).orElse(List.of());
    }

    public int retryCount() {
        return this.<Integer>value(KEY_RETRY_COUNT).orElse(0);
    }

    public boolean isSuccessful() {
        return this.<Boolean>value(KEY_IS_SUCCESSFUL).orElse(false);
    }

    public int maxRetries() {
        return this.<Integer>value(KEY_MAX_RETRIES).orElse(3);
    }

    public int retrieverRetries() {
        return this.<Integer>value(KEY_RETRIEVER_RETRIES).orElse(0);
    }

    public int synthesizerRetries() {
        return this.<Integer>value(KEY_SYNTHESIZER_RETRIES).orElse(0);
    }

    public double retrievalQuality() {
        return this.<Double>value(KEY_RETRIEVAL_QUALITY).orElse(0.0);
    }

    public String compilerFeedback() {
        return this.<String>value(KEY_COMPILER_FEEDBACK).orElse("");
    }

    public String finalDsl() {
        return this.<String>value(KEY_FINAL_DSL).orElse("");
    }

    public String retrievedContext() {
        return this.<String>value(KEY_RETRIEVED_CONTEXT).orElse("");
    }

    public String orchestratorRoute() {
        return this.<String>value(KEY_ORCHESTRATOR_ROUTE).orElse("");
    }

    public String orchestratorPhase() {
        return this.<String>value(KEY_ORCHESTRATOR_PHASE).orElse("");
    }

    public List<Map<String, Object>> planGraph() {
        return this.<List<Map<String, Object>>>value(KEY_PLAN_GRAPH).orElse(List.of());
    }

    public List<PlanStep> planSteps() {
        return PlanStep.fromMaps(planGraph());
    }

    public String currentChunkId() {
        return this.<String>value(KEY_CURRENT_CHUNK_ID).orElse("");
    }

    public String currentChunkTask() {
        return this.<String>value(KEY_CURRENT_CHUNK_TASK).orElse("");
    }

    public String currentChunkCode() {
        return this.<String>value(KEY_CURRENT_CHUNK_CODE).orElse("");
    }

    public Map<String, String> chunkOutputs() {
        return this.<Map<String, String>>value(KEY_CHUNK_OUTPUTS).orElse(Map.of());
    }

    public Map<String, String> chunkContexts() {
        return this.<Map<String, String>>value(KEY_CHUNK_CONTEXTS).orElse(Map.of());
    }

    public Map<String, Map<String, Integer>> chunkLineMap() {
        return this.<Map<String, Map<String, Integer>>>value(KEY_CHUNK_LINE_MAP).orElse(Map.of());
    }

    public String retrievalQuery() {
        return this.<String>value(KEY_RETRIEVAL_QUERY).orElse("");
    }

    public String synthesisMode() {
        return this.<String>value(KEY_SYNTHESIS_MODE).orElse("GENERATE");
    }

    public List<Map<String, Object>> structuredErrors() {
        return this.<List<Map<String, Object>>>value(KEY_STRUCTURED_ERRORS).orElse(List.of());
    }

    public List<JudgeErrorReport> judgeErrorReports() {
        return JudgeErrorReport.fromMaps(structuredErrors());
    }

    public List<Map<String, Object>> repairHistory() {
        return this.<List<Map<String, Object>>>value(KEY_REPAIR_HISTORY).orElse(List.of());
    }

    public List<Map<String, Object>> synthesisArtifacts() {
        return this.<List<Map<String, Object>>>value(KEY_SYNTHESIS_ARTIFACTS).orElse(List.of());
    }

    public List<Map<String, Object>> judgeArtifacts() {
        return this.<List<Map<String, Object>>>value(KEY_JUDGE_ARTIFACTS).orElse(List.of());
    }
}
