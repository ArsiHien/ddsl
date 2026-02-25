package uet.ndh.ddsl.controller;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uet.ndh.ddsl.agent.NlToDslService;

/**
 * REST API for translating informal natural-language input into valid DDSL code.
 * Uses the self-healing LangGraph pipeline (Synthesizer → Judge) with retry loop.
 */
@RestController
@RequestMapping("/api/translate")
@Slf4j
public class TranslationController {

    private final NlToDslService nlToDslService;

    public TranslationController(NlToDslService nlToDslService) {
        this.nlToDslService = nlToDslService;
    }

    /**
     * Translate informal/NL domain description into valid DDSL code.
     *
     * @param request the translation request containing informal input
     * @return the translation result with generated DDSL
     */
    @PostMapping
    public ResponseEntity<NlToDslService.NlToDslResult> translate(
            @RequestBody TranslateRequest request
    ) {
        log.info("POST /api/translate — input: {} chars",
                request.input() != null ? request.input().length() : 0);

        if (request.input() == null || request.input().isBlank()) {
            return ResponseEntity.badRequest().body(
                    NlToDslService.NlToDslResult.failure("Input cannot be empty")
            );
        }

        int maxRetries = request.maxRetries() != null ? request.maxRetries() : 3;

        var result = nlToDslService.translate(request.input(), maxRetries);
        return ResponseEntity.ok(result);
    }

    // ── Request DTO ─────────────────────────────────────────────────────

    public record TranslateRequest(
            String input,
            Integer maxRetries
    ) {}
}
