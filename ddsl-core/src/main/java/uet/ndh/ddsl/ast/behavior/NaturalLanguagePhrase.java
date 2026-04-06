package uet.ndh.ddsl.ast.behavior;

import uet.ndh.ddsl.ast.SourceSpan;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a natural language phrase used in behavior definitions.
 * 
 * Example: "placing an order" becomes method name "placingAnOrder"
 * 
 * Pure data record.
 */
public record NaturalLanguagePhrase(
    SourceSpan span,
    String rawText,
    List<String> tokens
) {
    
    public NaturalLanguagePhrase {
        tokens = tokens != null ? List.copyOf(tokens) : List.of();
    }
    
    /**
     * Create from raw text, tokenizing automatically.
     */
    public static NaturalLanguagePhrase from(SourceSpan span, String rawText) {
        List<String> tokens = List.of(rawText.toLowerCase().split("\\s+"));
        return new NaturalLanguagePhrase(span, rawText, tokens);
    }
    
    /**
     * Convert to a valid method name in camelCase.
     */
    public String toMethodName() {
        if (tokens.isEmpty()) return "unnamed";
        
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (first) {
                result.append(token.toLowerCase());
                first = false;
            } else {
                result.append(Character.toUpperCase(token.charAt(0)));
                if (token.length() > 1) {
                    result.append(token.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }
    
    /**
     * Get the display text.
     */
    public String toDisplayText() {
        return rawText;
    }
}
