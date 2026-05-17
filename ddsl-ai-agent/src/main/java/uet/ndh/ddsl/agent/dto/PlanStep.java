package uet.ndh.ddsl.agent.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed plan graph node used by the Orchestrator.
 *
 * LangGraph state still stores this as a Map for channel compatibility, but
 * agent logic should use this DTO instead of ad-hoc Map access.
 */
public record PlanStep(
        String id,
        String task,
        List<String> dependsOn,
        String status
) {
    public PlanStep withStatus(String newStatus) {
        return new PlanStep(id, task, dependsOn, newStatus);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("task", task);
        map.put("dependsOn", dependsOn);
        map.put("status", status);
        return map;
    }

    public static PlanStep fromMap(Map<String, Object> map) {
        @SuppressWarnings("unchecked")
        List<String> dependsOn = (List<String>) map.getOrDefault("dependsOn", List.of());
        return new PlanStep(
                String.valueOf(map.getOrDefault("id", "")),
                String.valueOf(map.getOrDefault("task", "")),
                dependsOn,
                String.valueOf(map.getOrDefault("status", "PENDING"))
        );
    }

    public static List<PlanStep> fromMaps(List<Map<String, Object>> maps) {
        return maps.stream().map(PlanStep::fromMap).toList();
    }

    public static List<Map<String, Object>> toMaps(List<PlanStep> steps) {
        return steps.stream().map(PlanStep::toMap).toList();
    }
}
