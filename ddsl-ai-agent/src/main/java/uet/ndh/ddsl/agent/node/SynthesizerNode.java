package uet.ndh.ddsl.agent.node;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.EnhancedDslState;
import uet.ndh.ddsl.agent.prompt.PromptManager;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Synthesizer Agent</b> — DDSL code generation with retry strategy.
 * <p>
 * Retry Strategy:
 * <ul>
 *   <li>Retry 1: Normal generation with context</li>
 *   <li>Retry 2: Inject stricter grammar rules + previous errors</li>
 * </ul>
 * <p>
 * On retry, adds explicit grammar constraints and requires deterministic output.
 */
@Component
@Slf4j
public class SynthesizerNode implements NodeAction<EnhancedDslState> {

    private final ChatClient chatClient;
    private final PromptManager promptManager;

    public SynthesizerNode(ChatClient ddslChatClient, PromptManager promptManager) {
        this.chatClient = ddslChatClient;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(EnhancedDslState state) {
        int retryCount = state.synthesizerRetries();
        String normalizedContext = state.normalizedContext();
        String previousDsl = state.currentDsl();
        
        log.info("SynthesizerNode: generating DDSL (attempt={})", retryCount);

        if (normalizedContext == null || normalizedContext.isBlank()) {
            log.warn("SynthesizerNode: empty normalized context");
            return createErrorResult("Empty normalized context", retryCount);
        }

        try {
            String userMessage = buildUserMessage(state, retryCount);
            
            // Retry 2: use stricter system prompt
            String systemPrompt = retryCount > 0 
                ? promptManager.synthesizerStrictPrompt() 
                : promptManager.synthesizerSystemPrompt();

            String dslDraft = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            String cleaned = stripCodeFences(dslDraft);

            log.info("SynthesizerNode: generated draft ({} chars)", cleaned.length());

            Map<String, Object> updates = new HashMap<>();
            updates.put(EnhancedDslState.KEY_CURRENT_DSL, cleaned);
            updates.put(EnhancedDslState.KEY_SYNTHESIZER_RETRIES, retryCount + 1);
            return updates;

        } catch (Exception e) {
            log.error("SynthesizerNode: generation failed", e);
            return createErrorResult(e.getMessage(), retryCount);
        }
    }

    private String buildUserMessage(EnhancedDslState state, int retryCount) {
        StringBuilder sb = new StringBuilder();
        
        if (retryCount > 0) {
            // Retry 2: Inject stricter instructions
            sb.append("## RETRY ATTEMPT - STRICT MODE\n");
            sb.append("Previous attempt had errors. Follow grammar EXACTLY.\n\n");
            
            if (!state.errorLogs().isEmpty()) {
                sb.append("## Previous Errors (FIX THESE)\n");
                for (String err : state.errorLogs()) {
                    sb.append("- ").append(err).append("\n");
                }
                sb.append("\n");
            }
            
            sb.append("## Previous Draft (for reference only)\n");
            sb.append("```ddsl\n").append(state.currentDsl()).append("\n```\n\n");
        }
        
        sb.append("## Structured Domain Context\n");
        sb.append(state.normalizedContext()).append("\n\n");
        
        if (retryCount == 0) {
            sb.append("## Instructions\n");
            sb.append("Generate valid DDSL code based on the structured context above.\n");
            sb.append("Start with 'BoundedContext [Name] {'.\n");
        } else {
            sb.append("## STRICT Instructions\n");
            sb.append("1. Use EXACT DDSL grammar - no deviations\n");
            sb.append("2. @identity annotation MUST come BEFORE field name\n");
            sb.append("3. Every Aggregate/Entity MUST have an @identity field\n");
            sb.append("4. Use 'then' before all action statements\n");
            sb.append("5. Use 'emit event' not just 'emit'\n");
            sb.append("6. Output ONLY raw DDSL, no markdown, no comments\n");
        }
        
        return sb.toString();
    }

    private Map<String, Object> createErrorResult(String error, int retryCount) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(EnhancedDslState.KEY_CURRENT_DSL, "");
        updates.put(EnhancedDslState.KEY_SYNTHESIZER_RETRIES, retryCount + 1);
        updates.put(EnhancedDslState.KEY_LAST_ERROR, error);
        updates.put(EnhancedDslState.KEY_ERROR_STAGE, "SYNTHESIZER");
        return updates;
    }

    private static String stripCodeFences(String raw) {
        if (raw == null) return "";
        String s = raw.strip();
        if (s.startsWith("```")) {
            s = s.replaceAll("^```[a-zA-Z]*\\n?", "")
                 .replaceAll("\\n?```$", "")
                 .strip();
        }
        return s;
    }
}
