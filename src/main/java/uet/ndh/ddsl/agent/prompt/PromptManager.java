package uet.ndh.ddsl.agent.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Centralized prompt management — loads prompt templates from external {@code .st}
 * resource files under {@code classpath:prompts/}.
 *
 * <h3>Loaded prompts:</h3>
 * <ul>
 *   <li>{@code synthesizer-system.st} — SynthesizerNode system prompt (grammar ref + self-correction rules)</li>
 *   <li>{@code default-system.st}     — default ChatClient system prompt</li>
 * </ul>
 */
@Component
public class PromptManager {

    private static final Logger log = LoggerFactory.getLogger(PromptManager.class);

    private final String synthesizerSystemPrompt;
    private final String defaultSystemPrompt;

    public PromptManager(
            @Value("classpath:prompts/synthesizer-system.st") Resource synthesizerSystem,
            @Value("classpath:prompts/default-system.st") Resource defaultSystem
    ) throws IOException {
        this.synthesizerSystemPrompt = loadResource(synthesizerSystem, "synthesizer-system.st");
        this.defaultSystemPrompt = loadResource(defaultSystem, "default-system.st");

        log.info("PromptManager: loaded 2 prompt templates from classpath:prompts/");
    }

    /**
     * System prompt for the SynthesizerNode — DDSL code generation with grammar
     * reference and self-correction rules.
     */
    public String synthesizerSystemPrompt() {
        return synthesizerSystemPrompt;
    }

    /**
     * Default system prompt for the ChatClient builder.
     */
    public String defaultSystemPrompt() {
        return defaultSystemPrompt;
    }

    // ── Internal helpers ─────────────────────────────────────────────────

    private static String loadResource(Resource resource, String name) throws IOException {
        if (!resource.exists()) {
            throw new IOException("Prompt template not found: classpath:prompts/" + name);
        }
        String content = resource.getContentAsString(StandardCharsets.UTF_8);
        log.debug("Loaded prompt template '{}' ({} chars)", name, content.length());
        return content;
    }
}
