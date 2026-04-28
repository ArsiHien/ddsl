package uet.ndh.ddsl.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;
import uet.ndh.ddsl.agent.prompt.PromptManager;

/**
 * Spring AI configuration for OpenRouter (OpenAI-compatible API).
 * <p>
 * Configures ChatClient with GPT-4o-mini for DDSL code generation.
 * Uses text-embedding-3-small (1536 dimensions) for vector search.
 * <p>
 * MCP tools are registered separately via {@link uet.ndh.ddsl.mcp.McpToolConfig}
 * and are <b>not</b> added as default tool callbacks.
 *
 * @see <a href="https://openrouter.ai/docs">OpenRouter API Docs</a>
 */
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.chat.base-url:https://openrouter.ai/api/v1}")
    private String chatBaseUrl;

    @Value("${spring.ai.openai.embedding.base-url:https://openrouter.ai/api/v1}")
    private String embeddingBaseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:openai/gpt-4o-mini}")
    private String chatModel;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}")
    private String embeddingModel;

    /**
     * OpenAI API for chat with OpenRouter.
     */
    @Bean
    @Primary
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl(chatBaseUrl)
                .apiKey(apiKey)
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .build();
    }

    /**
     * OpenAI API for embeddings with OpenRouter.
     * Uses separate base URL to ensure correct endpoint.
     */
    @Bean
    @Qualifier("embeddingOpenAiApi")
    public OpenAiApi embeddingOpenAiApi() {
        return OpenAiApi.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(apiKey)
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .build();
    }

    /**
     * EmbeddingModel configured for OpenRouter.
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(@Qualifier("embeddingOpenAiApi") OpenAiApi embeddingOpenAiApi) {
        return new OpenAiEmbeddingModel(
                embeddingOpenAiApi,
                MetadataMode.EMBED,
                org.springframework.ai.openai.OpenAiEmbeddingOptions.builder()
                        .model(embeddingModel)
                        .dimensions(1536)
                        .build(),
                RetryTemplate.builder().build()
        );
    }

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
