package uet.ndh.ddsl;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Verifies that the Spring application context loads successfully.
 * <p>
 * External AI services (Gemini, Qdrant) are excluded via auto-config exclusion
 * and replaced with mock beans so the test runs offline.
 */
@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=" +
				"org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration," +
				"org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration," +
				"org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration," +
				"org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration"
})
@Import(DdslApplicationTests.MockAiConfig.class)
class DdslApplicationTests {

	@TestConfiguration
	static class MockAiConfig {

		@Bean
		@Primary
		ChatClient.Builder chatClientBuilder() {
			ChatClient mockClient = Mockito.mock(ChatClient.class);
			ChatClient.Builder mockBuilder = Mockito.mock(ChatClient.Builder.class);
			Mockito.when(mockBuilder.defaultSystem(anyString())).thenReturn(mockBuilder);
			Mockito.when(mockBuilder.build()).thenReturn(mockClient);
			return mockBuilder;
		}

		@Bean
		@Primary
		VectorStore vectorStore() {
			return Mockito.mock(VectorStore.class);
		}
	}

	@Test
	void contextLoads() {
	}

}
