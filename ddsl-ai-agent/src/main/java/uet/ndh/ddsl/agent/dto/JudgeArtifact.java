package uet.ndh.ddsl.agent.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Captures one Judge/compiler validation output for diagnostics.
 */
public record JudgeArtifact(
        String chunkId,
        int attempt,
        boolean valid,
        String dsl,
        String compilerOutput,
        String compilerFeedback
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chunkId", chunkId);
        map.put("attempt", attempt);
        map.put("valid", valid);
        map.put("dsl", dsl);
        map.put("compilerOutput", compilerOutput);
        map.put("compilerFeedback", compilerFeedback);
        return map;
    }
}
