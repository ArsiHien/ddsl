package uet.ndh.ddsl.lsp.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket configuration for the DDSL Language Server.
 * 
 * Configures the WebSocket endpoint at /lsp that handles
 * Language Server Protocol communication.
 * 
 * CORS is configured to allow all origins for development.
 * In production, this should be restricted.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(simpleLspWebSocketHandler(), "/lsp")
            .setAllowedOrigins("*");  // Configure appropriately for production
    }
    
    @Bean
    public SimpleLspWebSocketHandler simpleLspWebSocketHandler() {
        return new SimpleLspWebSocketHandler();
    }
    
    /**
     * Configure WebSocket container settings.
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024); // 1MB
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L); // 30 minutes
        return container;
    }
}
