package uet.ndh.ddsl.agent;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

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
public class DslState extends AgentState {

    // ── State Keys ──────────────────────────────────────────────────────
    public static final String KEY_USER_INPUT = "userInput";
    public static final String KEY_CURRENT_DSL = "currentDsl";
    public static final String KEY_ERROR_LOGS = "errorLogs";
    public static final String KEY_RETRY_COUNT = "retryCount";
    public static final String KEY_IS_SUCCESSFUL = "isSuccessful";
    public static final String KEY_MAX_RETRIES = "maxRetries";

    /**
     * Channel schema — every key uses last-value-wins semantics.
     */
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            KEY_USER_INPUT, Channels.base(() -> ""),
            KEY_CURRENT_DSL, Channels.base(() -> ""),
            KEY_ERROR_LOGS, Channels.base(() -> List.<String>of()),
            KEY_RETRY_COUNT, Channels.base(() -> 0),
            KEY_IS_SUCCESSFUL, Channels.base(() -> false),
            KEY_MAX_RETRIES, Channels.base(() -> 3)
    );

    public DslState(Map<String, Object> initData) {
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
}
