package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Code formatter for DDSL Language Server.
 * 
 * Provides document formatting with:
 * - Consistent indentation
 * - Proper spacing around operators
 * - Aligned field declarations
 * - Consistent brace placement
 */
public class DdslFormatter {
    
    /**
     * Format the entire document.
     */
    public static List<TextEdit> format(String content, FormattingOptions options) {
        List<TextEdit> edits = new ArrayList<>();
        
        // Parse options
        int tabSize = options.getTabSize();
        boolean insertSpaces = options.isInsertSpaces();
        String indent = insertSpaces ? " ".repeat(tabSize) : "\t";
        
        // Split into lines
        String[] lines = content.split("\n", -1);
        StringBuilder formatted = new StringBuilder();
        
        int indentLevel = 0;
        boolean inMultiLineString = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            
            // Skip empty lines but preserve them
            if (trimmed.isEmpty()) {
                formatted.append("\n");
                continue;
            }
            
            // Handle multi-line strings
            if (inMultiLineString) {
                formatted.append(line).append("\n");
                if (trimmed.endsWith("\"\"\"")) {
                    inMultiLineString = false;
                }
                continue;
            }
            
            if (trimmed.contains("\"\"\"") && !trimmed.endsWith("\"\"\"")) {
                inMultiLineString = true;
            }
            
            // Decrease indent for closing braces
            if (trimmed.startsWith("}")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            
            // Apply indentation
            String indented = indent.repeat(indentLevel) + formatLine(trimmed);
            formatted.append(indented).append("\n");
            
            // Increase indent after opening braces
            if (trimmed.endsWith("{")) {
                indentLevel++;
            }
        }
        
        // Remove trailing newline if original didn't have one
        String result = formatted.toString();
        if (!content.endsWith("\n") && result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        
        // Create a single edit to replace the entire document
        int lastLine = lines.length - 1;
        int lastLineLength = lines[lastLine].length();
        
        TextEdit edit = new TextEdit(
            new Range(
                new Position(0, 0),
                new Position(lastLine, lastLineLength)
            ),
            result
        );
        
        edits.add(edit);
        return edits;
    }
    
    /**
     * Format a single line.
     */
    private static String formatLine(String line) {
        StringBuilder result = new StringBuilder();
        
        // Tokenize and reformat
        int i = 0;
        boolean inString = false;
        
        while (i < line.length()) {
            char c = line.charAt(i);
            
            // Handle strings
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inString = !inString;
                result.append(c);
                i++;
                continue;
            }
            
            if (inString) {
                result.append(c);
                i++;
                continue;
            }
            
            // Handle operators with spacing
            if (c == ':') {
                // Field type annotation - space after colon
                result.append(": ");
                i++;
                // Skip existing spaces
                while (i < line.length() && line.charAt(i) == ' ') {
                    i++;
                }
                continue;
            }
            
            if (c == ',') {
                // List separator - space after comma
                result.append(", ");
                i++;
                // Skip existing spaces
                while (i < line.length() && line.charAt(i) == ' ') {
                    i++;
                }
                continue;
            }
            
            if (c == '{') {
                // Opening brace - space before
                if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                    result.append(" ");
                }
                result.append("{");
                i++;
                continue;
            }
            
            if (c == '@') {
                // Annotation - space before if not at start
                if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                    result.append(" ");
                }
                result.append("@");
                i++;
                continue;
            }
            
            // Default: append character
            result.append(c);
            i++;
        }
        
        // Trim trailing spaces
        String formatted = result.toString().stripTrailing();
        
        return formatted;
    }
}
