package uet.ndh.ddsl.agent.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Centralized prompt management for all agent nodes.
 * Loads prompt templates from external .st resource files.
 */
@Component
public class PromptManager {

    private static final Logger log = LoggerFactory.getLogger(PromptManager.class);

    private final String synthesizerSystemPrompt;
    private final String synthesizerStrictPrompt;
    private final String orchestratorSystemPrompt;
    private final String retrieverSystemPrompt;
    private final String judgeSystemPrompt;
    private final String defaultSystemPrompt;

    public PromptManager(
            @Value("classpath:prompts/synthesizer-system.st") Resource synthesizerSystem,
            @Value("classpath:prompts/synthesizer-strict.st") Resource synthesizerStrict,
            @Value("classpath:prompts/orchestrator-system.st") Resource orchestratorSystem,
            @Value("classpath:prompts/retriever-system.st") Resource retrieverSystem,
            @Value("classpath:prompts/judge-system.st") Resource judgeSystem,
            @Value("classpath:prompts/default-system.st") Resource defaultSystem
    ) throws IOException {
        this.synthesizerSystemPrompt = loadResource(synthesizerSystem, "synthesizer-system.st");
        this.synthesizerStrictPrompt = loadResource(synthesizerStrict, "synthesizer-strict.st");
        this.orchestratorSystemPrompt = loadResource(orchestratorSystem, "orchestrator-system.st");
        this.retrieverSystemPrompt = loadResource(retrieverSystem, "retriever-system.st");
        this.judgeSystemPrompt = loadResource(judgeSystem, "judge-system.st");
        this.defaultSystemPrompt = loadResource(defaultSystem, "default-system.st");

        log.info("PromptManager: loaded 6 prompt templates from classpath:prompts/");
    }

    public String synthesizerSystemPrompt() {
        return synthesizerSystemPrompt;
    }

    public String synthesizerStrictPrompt() {
        return synthesizerStrictPrompt;
    }

    public String orchestratorSystemPrompt() {
        return orchestratorSystemPrompt;
    }

    public String retrieverSystemPrompt() {
        return retrieverSystemPrompt;
    }

    public String judgeSystemPrompt() {
        return judgeSystemPrompt;
    }

    public String defaultSystemPrompt() {
        return defaultSystemPrompt;
    }

    private static String loadResource(Resource resource, String name) throws IOException {
        if (!resource.exists()) {
            throw new IOException("Prompt template not found: classpath:prompts/" + name);
        }
        String content = resource.getContentAsString(StandardCharsets.UTF_8);
        log.debug("Loaded prompt template '{}' ({} chars)", name, content.length());
        return content;
    }
}
