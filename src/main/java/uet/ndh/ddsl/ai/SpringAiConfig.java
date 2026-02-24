package uet.ndh.ddsl.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uet.ndh.ddsl.agent.prompt.PromptManager;

/**
 * Spring AI configuration for Gemini ChatClient.
 * <p>
 * The ChatClient is configured with a default system prompt only.
 * MCP tools are registered separately via {@link uet.ndh.ddsl.mcp.McpToolConfig}
 * and are <b>not</b> added as default tool callbacks — the Synthesizer node
 * generates pure text output without function-calling side effects.
 */
@Configuration
public class SpringAiConfig {

    @Bean
    public ChatClient.Builder chatClientBuilder(
            ChatClient.Builder builder,
            PromptManager promptManager
    ) {
        return builder
                .defaultSystem(promptManager.defaultSystemPrompt());
    }
}
