package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.*;

/**
 * Definition provider for DDSL Language Server.
 * 
 * Provides go-to-definition and find-references functionality for:
 * - Type references (navigate to type definition)
 * - Field references
 * - Method references
 */
public class DdslDefinitionProvider {
    
    /**
     * Get definition location for the symbol at the given position.
     */
    public List<Location> getDefinition(String uri, String content, List<Token> tokens, Position position) {
        List<Location> locations = new ArrayList<>();
        
        // Find the token at the position
        Token token = findTokenAtPosition(tokens, position);
        
        if (token == null || token.getType() != TokenType.IDENTIFIER) {
            return locations;
        }
        
        String symbolName = token.getLexeme();
        
        // Find the definition of this symbol
        Token definition = findDefinition(tokens, symbolName);
        
        if (definition != null) {
            locations.add(new Location(uri, tokenToRange(definition)));
        }
        
        return locations;
    }
    
    /**
     * Get all references to the symbol at the given position.
     */
    public List<Location> getReferences(String uri, String content, List<Token> tokens, 
                                        Position position, boolean includeDeclaration) {
        List<Location> locations = new ArrayList<>();
        
        // Find the token at the position
        Token token = findTokenAtPosition(tokens, position);
        
        if (token == null || token.getType() != TokenType.IDENTIFIER) {
            return locations;
        }
        
        String symbolName = token.getLexeme();
        
        // Find all references to this symbol
        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER && t.getLexeme().equals(symbolName)) {
                // Check if this is the definition
                boolean isDefinition = isDefinitionToken(tokens, t);
                
                if (includeDeclaration || !isDefinition) {
                    locations.add(new Location(uri, tokenToRange(t)));
                }
            }
        }
        
        return locations;
    }
    
    /**
     * Find the definition token for a symbol.
     */
    private Token findDefinition(List<Token> tokens, String symbolName) {
        // Look for type definitions (Aggregate, Entity, ValueObject, etc.)
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            
            if (isTypeDeclarationKeyword(token.getType())) {
                // The next identifier is the type name
                if (i + 1 < tokens.size()) {
                    Token nextToken = tokens.get(i + 1);
                    if (nextToken.getType() == TokenType.IDENTIFIER &&
                        nextToken.getLexeme().equals(symbolName)) {
                        return nextToken;
                    }
                }
            }
        }
        
        // Look for field definitions (identifier followed by colon)
        for (int i = 0; i < tokens.size() - 1; i++) {
            Token token = tokens.get(i);
            Token nextToken = tokens.get(i + 1);
            
            if (token.getType() == TokenType.IDENTIFIER &&
                token.getLexeme().equals(symbolName) &&
                nextToken.getType() == TokenType.COLON) {
                return token;
            }
        }
        
        // Look for parameter definitions (in when clauses)
        boolean inWhenClause = false;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            
            if (token.getType() == TokenType.WHEN) {
                inWhenClause = true;
            } else if (token.getType() == TokenType.LEFT_BRACE) {
                inWhenClause = false;
            }
            
            if (inWhenClause && token.getType() == TokenType.IDENTIFIER &&
                token.getLexeme().equals(symbolName)) {
                return token;
            }
        }
        
        return null;
    }
    
    /**
     * Check if a token is a definition token (vs a reference).
     */
    private boolean isDefinitionToken(List<Token> tokens, Token token) {
        int index = tokens.indexOf(token);
        
        if (index <= 0) {
            return false;
        }
        
        // Check if preceded by a type declaration keyword
        Token prevToken = tokens.get(index - 1);
        if (isTypeDeclarationKeyword(prevToken.getType())) {
            return true;
        }
        
        // Check if followed by a colon (field definition)
        if (index + 1 < tokens.size()) {
            Token nextToken = tokens.get(index + 1);
            if (nextToken.getType() == TokenType.COLON) {
                return true;
            }
        }
        
        // Check if in a when clause parameter list
        // This is more complex and would need context tracking
        
        return false;
    }
    
    /**
     * Check if a token type is a type declaration keyword.
     */
    private boolean isTypeDeclarationKeyword(TokenType type) {
        return type == TokenType.AGGREGATE ||
               type == TokenType.ENTITY ||
               type == TokenType.VALUE_OBJECT ||
               type == TokenType.DOMAIN_SERVICE ||
               type == TokenType.DOMAIN_EVENT ||
               type == TokenType.FACTORY ||
               type == TokenType.REPOSITORY ||
               type == TokenType.SPECIFICATION ||
               type == TokenType.BOUNDED_CONTEXT;
    }
    
    /**
     * Find the token at the given position.
     */
    private Token findTokenAtPosition(List<Token> tokens, Position position) {
        int line = position.getLine() + 1; // Convert to 1-based
        int column = position.getCharacter() + 1;
        
        for (Token token : tokens) {
            if (token.getLine() == line) {
                int tokenStart = token.getColumn();
                int tokenEnd = tokenStart + token.getLexeme().length();
                
                if (column >= tokenStart && column <= tokenEnd) {
                    return token;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Convert a token to an LSP Range.
     */
    private Range tokenToRange(Token token) {
        int line = token.getLine() - 1; // Convert to 0-based
        int startCol = token.getColumn() - 1;
        int endCol = startCol + token.getLexeme().length();
        
        return new Range(
            new Position(line, startCol),
            new Position(line, endCol)
        );
    }
}
