package uet.ndh.ddsl.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dedicated Spring Boot entrypoint for AI backend APIs and MCP tooling.
 */
@SpringBootApplication(scanBasePackages = "uet.ndh.ddsl")
public class AiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }
}
