package uet.ndh.ddsl.agent.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Captures one Synthesizer LLM output for diagnostics.
 */
public record SynthesisArtifact(
        String chunkId,
        String mode,
        int attempt,
        String code
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chunkId", chunkId);
        map.put("mode", mode);
        map.put("attempt", attempt);
        map.put("code", code);
        return map;
    }
}
