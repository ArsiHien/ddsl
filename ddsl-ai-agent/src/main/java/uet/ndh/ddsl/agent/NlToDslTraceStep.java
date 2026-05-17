package uet.ndh.ddsl.agent;

import java.util.List;
import java.util.Map;

/**
 * One emitted LangGraph node execution snapshot for NL -> DDSL diagnostics.
 */
public record NlToDslTraceStep(
        int index,
        String node,
        long elapsedMs,
        long deltaMs,
        String phase,
        String route,
        String chunkId,
        String synthesisMode,
        int retrieverRetries,
        int synthesizerRetries,
        double retrievalQuality,
        int currentDslChars,
        int currentChunkChars,
        int errorCount,
        int repairAttempts,
        Map<String, String> planStatuses,
        List<Map<String, Object>> structuredErrors
) {
    public static NlToDslTraceStep from(
            int index,
            String node,
            DdslState state,
            long elapsedMs,
            long deltaMs
    ) {
        return new NlToDslTraceStep(
                index,
                node,
                elapsedMs,
                deltaMs,
                state.orchestratorPhase(),
                state.orchestratorRoute(),
                state.currentChunkId(),
                state.synthesisMode(),
                state.retrieverRetries(),
                state.synthesizerRetries(),
                state.retrievalQuality(),
                state.currentDsl() != null ? state.currentDsl().length() : 0,
                state.currentChunkCode() != null ? state.currentChunkCode().length() : 0,
                state.errorLogs() != null ? state.errorLogs().size() : 0,
                state.repairHistory() != null ? state.repairHistory().size() : 0,
                planStatuses(state),
                state.structuredErrors()
        );
    }

    private static Map<String, String> planStatuses(DdslState state) {
        return state.planGraph().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.get("id")),
                        entry -> String.valueOf(entry.get("status")),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));
    }
}
