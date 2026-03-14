package uet.ndh.ddsl.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uet.ndh.ddsl.agent.prompt.PromptManager;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration;

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
    public ChatClient ddslChatClient(
            ChatClient.Builder builder,
            PromptManager promptManager
    ) {
        return builder
                .defaultSystem(promptManager.defaultSystemPrompt())
                .build();
    }

}
