package uet.ndh.ddsl.agent.node;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.agent.DdslState;
import uet.ndh.ddsl.agent.prompt.PromptManager;
import uet.ndh.ddsl.agent.dto.SynthesisArtifact;

import java.util.HashMap;
import java.util.List;
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
public class SynthesizerNode implements NodeAction<DdslState> {

    private final ChatClient chatClient;
    private final PromptManager promptManager;

    public SynthesizerNode(ChatClient ddslChatClient, PromptManager promptManager) {
        this.chatClient = ddslChatClient;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(DdslState state) {
        int retryCount = state.synthesizerRetries();
        String userInput = state.userInput();
        String retrievedContext = state.retrievedContext();
        
        log.info("SynthesizerNode: generating DDSL chunk={} mode={} (attempt={})",
                state.currentChunkId(), state.synthesisMode(), retryCount);

        // Validate inputs: userInput and retrievedContext must be present
        if ((userInput == null || userInput.isBlank()) || (retrievedContext == null || retrievedContext.isBlank())) {
            log.warn("SynthesizerNode: missing userInput or retrievedContext");
            return createErrorResult("Missing userInput or retrievedContext", retryCount);
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
            updates.put("currentDsl", cleaned);
            updates.put("currentChunkCode", cleaned);
            updates.put("synthesizerRetries", retryCount + 1);
            updates.put(DdslState.KEY_SYNTHESIS_ARTIFACTS, appendArtifact(state, retryCount, cleaned));
            return updates;

        } catch (Exception e) {
            log.error("SynthesizerNode: generation failed", e);
            return createErrorResult(e.getMessage(), retryCount);
        }
    }

    private String buildUserMessage(DdslState state, int retryCount) {
        String userInput = state.userInput();
        String retrievedContext = state.retrievedContext();
        String compilerFeedback = state.compilerFeedback();
        StringBuilder sb = new StringBuilder();

        if (!state.currentChunkId().isBlank()) {
            sb.append("## Orchestrator Task\n");
            sb.append("Chunk ID: ").append(state.currentChunkId()).append("\n");
            sb.append("Mode: ").append(state.synthesisMode()).append("\n");
            sb.append("Task: ").append(state.currentChunkTask()).append("\n\n");

            sb.append("## Output Contract\n");
            sb.append("Output ONLY raw DDSL declarations for this chunk.\n");
            sb.append("Do not output BoundedContext wrappers or top-level section wrappers such as domain/events/repositories.\n");
            sb.append("If the user did not request anything belonging to this chunk, output an empty response.\n");
            sb.append("Do not invent services, repositories, events, value objects, or factories just to fill a chunk.\n");
            sb.append("Keep declarations syntactically complete because the Orchestrator will merge them into the final file.\n\n");

            if ("DomainTypes".equals(state.currentChunkId())) {
                sb.append("## DomainTypes Rules\n");
                sb.append("- Generate only explicitly requested ValueObject/enum/shared scalar declarations.\n");
                sb.append("- Do NOT create ValueObjects just because a field is named id.\n");
                sb.append("- ValueObject fields must NOT use @identity. @identity is only for Aggregate/Entity fields.\n");
                sb.append("- If no value object/custom type is requested, output empty text.\n\n");
            }
            if ("DomainModel".equals(state.currentChunkId())) {
                sb.append("## DomainModel Rules\n");
                sb.append("- Generate Aggregate/Entity declarations requested by the user.\n");
                sb.append("- Put @identity only on Aggregate/Entity identity fields.\n");
                sb.append("- Prefer built-in UUID identity fields unless a prior ValueObject type exists in context.\n");
                sb.append("- Do not define DomainService, DomainEvent, Repository, Factory, or Specification here.\n\n");
            }

            sb.append("## Parser-Compatible Behavior Syntax\n");
            sb.append("Use the colon/list style accepted by the current DDSL parser:\n");
            sb.append("invariants {\n");
            sb.append("    \"Business rule message\": condition\n");
            sb.append("}\n");
            sb.append("when action name with param:\n");
            sb.append("require that:\n");
            sb.append("    - condition\n");
            sb.append("then:\n");
            sb.append("    - action\n");
            sb.append("emit EventName with field\n");
            sb.append("Specification Name {\n");
            sb.append("    matches EntityName where:\n");
            sb.append("        - condition\n");
            sb.append("}\n");
            sb.append("Do NOT use brace-style behavior blocks like `when action { ... }`.\n\n");
            sb.append("Do NOT write bare invariant lines like `balance must not be negative`; use `\"Balance must not be negative\": balance >= 0`.\n");
            sb.append("Every require/then/specification condition line MUST start with `-`.\n");
            sb.append("Do NOT write `Specification Name for Entity`; put the target after `matches`.\n\n");
            sb.append("Use temporal keyword `now` without parentheses. Do NOT write `now()`.\n\n");
        }

        if ("REPAIR".equalsIgnoreCase(state.synthesisMode())) {
            sb.append("## Repair Mode\n");
            sb.append("You are an expert DSL programmer. Fix ONLY the error described for this chunk and keep unrelated code unchanged.\n\n");
            sb.append("## Current Chunk Code\n");
            sb.append("```ddsl\n").append(state.currentChunkCode()).append("\n```\n\n");
            sb.append("## Structured Compilation Error\n");
            if (state.structuredErrors() != null && !state.structuredErrors().isEmpty()) {
                for (Map<String, Object> error : state.structuredErrors()) {
                    sb.append("- ").append(error).append("\n");
                }
            } else if (state.errorLogs() != null && !state.errorLogs().isEmpty()) {
                for (String err : state.errorLogs()) {
                    sb.append("- ").append(err).append("\n");
                }
            }
            sb.append("\n");
            if (compilerFeedback != null && !compilerFeedback.isBlank()) {
                sb.append("## Compiler Feedback\n").append(compilerFeedback).append("\n\n");
            }
            sb.append("## Required Context and Similar Fixes\n").append(retrievedContext).append("\n\n");
            sb.append("## Instructions\n");
            sb.append("Apply the suggested fix or a better fix that resolves the error.\n");
            sb.append("Output the full corrected code for this chunk only.\n");
            return sb.toString();
        }
        
        if (retryCount > 0) {
            // Retry 2: Inject stricter instructions
            sb.append("## RETRY ATTEMPT - STRICT MODE\n");
            sb.append("Previous attempt had errors. Follow grammar EXACTLY.\n\n");
            
            if (state.errorLogs() != null && !state.errorLogs().isEmpty()) {
                sb.append("## Previous Errors (FIX THESE)\n");
                for (String err : state.errorLogs()) {
                    sb.append("- ").append(err).append("\n");
                }
                sb.append("\n");
            }
            
            sb.append("## Previous Draft (for reference only)\n");
            sb.append("```ddsl\n").append(state.currentDsl()).append("\n```").append("\n\n");
            // Include compiler feedback on retry if available
            if (compilerFeedback != null && !compilerFeedback.isBlank()) {
                sb.append("## Compiler Feedback\n");
                sb.append(compilerFeedback).append("\n\n");
            }
        }
        sb.append("## User Input\n");
        sb.append(userInput).append("\n\n");
        sb.append("## Retrieved Context\n");
        sb.append(retrievedContext).append("\n\n");
        
        if (retryCount == 0) {
            sb.append("## Instructions\n");
            sb.append("Generate valid DDSL code for the requested chunk based on the task and context above.\n");
            sb.append("Return only the declarations that belong to this chunk.\n");
        } else {
            sb.append("## STRICT Instructions\n");
            sb.append("1. Use EXACT DDSL grammar - no deviations\n");
            sb.append("2. @identity annotation MUST come BEFORE field name\n");
            sb.append("3. Every Aggregate/Entity MUST have an @identity field\n");
            sb.append("4. Use 'then' before all action statements\n");
            sb.append("5. Use 'emit EventName', not 'emit event EventName'\n");
            sb.append("6. Output ONLY raw DDSL, no markdown, no comments\n");
            sb.append("7. Invariants require quoted message syntax: \"Message\": condition\n");
            sb.append("8. Require/then/specification condition items must start with '-'\n");
            sb.append("9. Use `now`, not `now()`\n");
        }
        
        return sb.toString();
    }

    private Map<String, Object> createErrorResult(String error, int retryCount) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentDsl", "");
        updates.put("synthesizerRetries", retryCount + 1);
        updates.put("lastError", error);
        updates.put("errorStage", "SYNTHESIZER");
        return updates;
    }

    private List<Map<String, Object>> appendArtifact(DdslState state, int retryCount, String cleaned) {
        List<Map<String, Object>> artifacts = new java.util.ArrayList<>(state.synthesisArtifacts());
        artifacts.add(new SynthesisArtifact(
                state.currentChunkId(),
                state.synthesisMode(),
                retryCount,
                cleaned
        ).toMap());
        return artifacts;
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
