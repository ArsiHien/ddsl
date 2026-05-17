package uet.ndh.ddsl.agent.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured Judge verdict for the repair loop.
 */
public record JudgeErrorReport(
        String verdict,
        String errorType,
        String errorLocation,
        int lineNumber,
        String errorMessage,
        String fixHint,
        boolean retryAllowed,
        String sourceChunk,
        String bestPartialCode
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("verdict", verdict);
        map.put("error_type", errorType);
        map.put("error_location", errorLocation);
        map.put("line_number", lineNumber);
        map.put("error_message", errorMessage);
        map.put("fix_hint", fixHint);
        map.put("retry_allowed", retryAllowed);
        map.put("source_chunk", sourceChunk);
        if (bestPartialCode != null && !bestPartialCode.isBlank()) {
            map.put("best_partial_code", bestPartialCode);
        }
        return map;
    }

    public static JudgeErrorReport fromMap(Map<String, Object> map) {
        return new JudgeErrorReport(
                String.valueOf(map.getOrDefault("verdict", "")),
                String.valueOf(map.getOrDefault("error_type", "")),
                String.valueOf(map.getOrDefault("error_location", "")),
                intValue(map.get("line_number")),
                String.valueOf(map.getOrDefault("error_message", "")),
                String.valueOf(map.getOrDefault("fix_hint", "")),
                booleanValue(map.get("retry_allowed")),
                String.valueOf(map.getOrDefault("source_chunk", "")),
                String.valueOf(map.getOrDefault("best_partial_code", ""))
        );
    }

    public static List<JudgeErrorReport> fromMaps(List<Map<String, Object>> maps) {
        return maps.stream().map(JudgeErrorReport::fromMap).toList();
    }

    public static List<Map<String, Object>> toMaps(List<JudgeErrorReport> reports) {
        return reports.stream().map(JudgeErrorReport::toMap).toList();
    }

    private static int intValue(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
