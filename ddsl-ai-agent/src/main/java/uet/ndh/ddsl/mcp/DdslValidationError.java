package uet.ndh.ddsl.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured representation of a DDSL validation error.
 * This is serialized to JSON for MCP consumers.
 */
public record DdslValidationError(
        @JsonProperty("category") ErrorCategory errorCategory,
        @JsonProperty("location") String location,
        @JsonProperty("message") String message,
        @JsonProperty("suggestion") String suggestion
) {
}
