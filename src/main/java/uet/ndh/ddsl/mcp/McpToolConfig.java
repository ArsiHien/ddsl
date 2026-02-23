package uet.ndh.ddsl.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to register DDSL MCP tools with Spring AI's tool infrastructure.
 * <p>
 * These tools are exposed via the MCP Server (WebMVC transport) and also
 * available to the internal ChatClient for function calling.
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider ddslToolCallbackProvider(
            DdslValidationTool validationTool,
            DdslCodegenTool codegenTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(validationTool, codegenTool)
                .build();
    }
}
