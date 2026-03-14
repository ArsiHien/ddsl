package uet.ndh.ddsl.agent.node;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.DslState;
import uet.ndh.ddsl.agent.prompt.PromptManager;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Synthesizer Node</b> — the NL-to-DDSL translation core of the pipeline.
 * <p>
 * The user provides an informal, natural-language-like domain description within a
 * rough DDSL structure. Fields may be described as sentences ("order id is UUID for
 * identity"), behaviors as compressed phrases ("title and content must not empty,
 * change status to PUBLISHED, publish event PostPublished"), and types as everyday
 * words ("text", "whole number", "yes/no flag"). This node:
 * <ol>
 *   <li><b>RAG enrichment</b> — queries Qdrant for similar translation patterns,
 *       few-shot examples, and grammar rules via {@link QuestionAnswerAdvisor}.</li>
 *   <li><b>Translation</b> — prompts Gemini with a system prompt loaded from
 *       {@code synthesizer-system.st} to interpret the informal input and produce
 *       valid DDSL.</li>
 *   <li><b>Self-correction</b> — on re-entry (retry), incorporates the previous
 *       draft and parser error logs so Gemini can make targeted fixes.</li>
 * </ol>
 */
@Component
@Slf4j
public class SynthesizerNode implements NodeAction<DslState> {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final PromptManager promptManager;

    public SynthesizerNode(ChatClient ddslChatClient,
                           VectorStore vectorStore,
                           PromptManager promptManager) {
        this.chatClient = ddslChatClient;
        this.vectorStore = vectorStore;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(DslState state) throws Exception {
        int attempt = state.retryCount();
        log.info("SynthesizerNode: translating informal DDSL (attempt={})", attempt);

        String userMessage = buildUserMessage(state);

        // RAG: semantic search over Qdrant for syntax correction patterns
        String dslDraft = chatClient.prompt()
                .system(promptManager.synthesizerSystemPrompt())
                .user(userMessage)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .query(state.userInput())
                                .topK(5)
                                .similarityThreshold(0.7)
                                .build())
                        .build())
                .call()
                .content();

        String cleaned = stripCodeFences(dslDraft);

        log.debug("SynthesizerNode draft ({} chars): {}",
                cleaned.length(),
                cleaned.substring(0, Math.min(200, cleaned.length())));

        Map<String, Object> updates = new HashMap<>();
        updates.put(DslState.KEY_CURRENT_DSL, cleaned);
        updates.put(DslState.KEY_RETRY_COUNT, attempt + 1);
        return updates;
    }

    // ── User message construction ───────────────────────────────────────

    private String buildUserMessage(DslState state) {
        var sb = new StringBuilder();

        if (state.retryCount() > 0 && !state.errorLogs().isEmpty()) {
            // Self-correction mode: previous draft + parser errors
            sb.append("## Previous DSL Draft (has parser errors — FIX these)\n");
            sb.append("```ddsl\n").append(state.currentDsl()).append("\n```\n\n");

            sb.append("## Parser Error Logs\n");
            for (String err : state.errorLogs()) {
                sb.append("- ").append(err).append("\n");
            }
            sb.append("\n");
        } else {
            // First attempt: user's informal/NL domain description
            sb.append("## Informal Domain Description to Translate\n");
            sb.append("The following is a domain description written in very informal, ");
            sb.append("natural-language-like pseudocode within a rough DDSL structure.\n");
            sb.append("Interpret the user's intent and translate it into valid, parseable DDSL.\n");
            sb.append("Preserve ALL domain content (fields, behaviors, events, etc.) described by the user.\n\n");
            sb.append("```ddsl\n").append(state.userInput()).append("\n```\n");
        }

        return sb.toString();
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
