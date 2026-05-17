package uet.ndh.ddsl.agent;

import java.util.List;
import java.util.Map;

/**
 * Translation result plus execution trace and aggregate diagnostics.
 */
public record NlToDslDetailedResult(
        NlToDslResult result,
        List<NlToDslTraceStep> trace,
        long totalTimeMs,
        Map<String, Long> nodeTimeMs,
        Map<String, Integer> nodeIterations,
        List<Map<String, Object>> synthesisArtifacts,
        List<Map<String, Object>> judgeArtifacts
) {
}
