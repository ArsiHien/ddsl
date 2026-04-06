package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.Hover;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uet.ndh.ddsl.parser.lexer.Scanner;
import uet.ndh.ddsl.parser.lexer.Token;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API endpoints for LSP functionality.
 * 
 * These endpoints are useful for:
 * - Testing LSP features without WebSocket
 * - Debugging token/semantic token encoding
 * - Integration with tools that don't support WebSocket
 */
@RestController
@RequestMapping("/api/lsp")
@CrossOrigin(origins = "*")
public class LspRestController {
    
    /**
     * Tokenize source code and return tokens.
     * Useful for debugging the lexer.
     */
    @PostMapping("/tokenize")
    public ResponseEntity<List<Map<String, Object>>> tokenize(@RequestBody String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        List<Map<String, Object>> result = tokens.stream()
            .map(token -> Map.<String, Object>of(
                "type", token.getType().name(),
                "lexeme", token.getLexeme(),
                "line", token.getLine(),
                "column", token.getColumn(),
                "offset", token.getOffset(),
                "literal", token.getLiteral() != null ? token.getLiteral().toString() : null
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get semantic tokens for source code.
     * Returns encoded semantic tokens in LSP format.
     */
    @PostMapping("/semantic-tokens")
    public ResponseEntity<Map<String, Object>> semanticTokens(@RequestBody String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        var semanticTokens = DdslSemanticTokens.encode(tokens);
        
        return ResponseEntity.ok(Map.of(
            "data", semanticTokens.getData(),
            "legend", Map.of(
                "tokenTypes", DdslSemanticTokens.getLegend().getTokenTypes(),
                "tokenModifiers", DdslSemanticTokens.getLegend().getTokenModifiers()
            )
        ));
    }
    
    /**
     * Validate source code and return diagnostics.
     */
    @PostMapping("/validate")
    public ResponseEntity<List<Map<String, Object>>> validate(@RequestBody String source) {
        DdslDiagnosticProvider provider = new DdslDiagnosticProvider();
        var diagnostics = provider.getDiagnostics(source);
        
        List<Map<String, Object>> result = diagnostics.stream()
            .map(d -> Map.<String, Object>of(
                "severity", d.getSeverity().name(),
                "message", d.getMessage(),
                "code", d.getCode() != null ? d.getCode().getLeft() : "",
                "range", Map.of(
                    "start", Map.of(
                        "line", d.getRange().getStart().getLine(),
                        "character", d.getRange().getStart().getCharacter()
                    ),
                    "end", Map.of(
                        "line", d.getRange().getEnd().getLine(),
                        "character", d.getRange().getEnd().getCharacter()
                    )
                )
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get completions for a position.
     */
    @PostMapping("/completions")
    public ResponseEntity<List<Map<String, Object>>> completions(
            @RequestBody Map<String, Object> request) {
        
        String source = (String) request.get("source");
        int line = ((Number) request.get("line")).intValue();
        int character = ((Number) request.get("character")).intValue();
        
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        DdslCompletionProvider provider = new DdslCompletionProvider();
        var position = new org.eclipse.lsp4j.Position(line, character);
        var completions = provider.getCompletions(source, tokens, position, null);
        
        List<Map<String, Object>> result = completions.stream()
            .map(c -> Map.<String, Object>of(
                "label", c.getLabel(),
                "kind", c.getKind() != null ? c.getKind().name() : "Text",
                "detail", c.getDetail() != null ? c.getDetail() : "",
                "insertText", c.getInsertText() != null ? c.getInsertText() : c.getLabel()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get hover information for a position.
     */
    @PostMapping("/hover")
    public ResponseEntity<Map<String, Object>> hover(
            @RequestBody Map<String, Object> request) {
        
        String source = (String) request.get("source");
        int line = ((Number) request.get("line")).intValue();
        int character = ((Number) request.get("character")).intValue();
        
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        DdslHoverProvider provider = new DdslHoverProvider();
        var position = new org.eclipse.lsp4j.Position(line, character);
        Hover hover = provider.getHover(source, tokens, position);
        
        if (hover == null) {
            return ResponseEntity.ok(Map.of());
        }
        
        return ResponseEntity.ok(Map.of(
            "contents", hover.getContents().getRight().getValue(),
            "range", hover.getRange() != null ? Map.of(
                "start", Map.of(
                    "line", hover.getRange().getStart().getLine(),
                    "character", hover.getRange().getStart().getCharacter()
                ),
                "end", Map.of(
                    "line", hover.getRange().getEnd().getLine(),
                    "character", hover.getRange().getEnd().getCharacter()
                )
            ) : null
        ));
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "server", "DDSL Language Server",
            "version", "1.0.0"
        ));
    }
}
