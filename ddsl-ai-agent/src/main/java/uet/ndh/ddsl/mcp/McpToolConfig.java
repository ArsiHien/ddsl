package uet.ndh.ddsl.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to register DDSL MCP tools with Spring AI's tool infrastructure.
 * <p>
 * Exposes the parser-only validation tool via the MCP Server (WebMVC transport).
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider ddslToolCallbackProvider(
            DdslValidationTool validationTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(validationTool)
                .build();
    }
}
