package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Document Symbol provider for DDSL Language Server.
 * 
 * Provides document outline/symbols for:
 * - Bounded contexts
 * - Aggregates, Entities, Value Objects
 * - Domain Services, Events
 * - Factories, Repositories, Specifications
 * - Operations and behaviors
 */
public class DdslDocumentSymbolProvider {
    
    /**
     * Get document symbols for the outline view.
     */
    public static List<Either<SymbolInformation, DocumentSymbol>> getSymbols(
            String content, List<Token> tokens) {
        
        List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
        
        if (tokens == null || tokens.isEmpty()) {
            return symbols;
        }
        
        // Track the current container for nesting
        DocumentSymbol currentContainer = null;
        int braceDepth = 0;
        
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            TokenType type = token.getType();
            
            // Track brace depth
            if (type == TokenType.LEFT_BRACE) {
                braceDepth++;
            } else if (type == TokenType.RIGHT_BRACE) {
                braceDepth--;
                if (braceDepth == 0) {
                    currentContainer = null;
                }
            }
            
            // Create symbols for type declarations
            if (isDeclarationKeyword(type)) {
                // Get the name (next identifier token)
                String name = "Unknown";
                Token nameToken = null;
                if (i + 1 < tokens.size()) {
                    Token nextToken = tokens.get(i + 1);
                    if (nextToken.getType() == TokenType.IDENTIFIER) {
                        name = nextToken.getLexeme();
                        nameToken = nextToken;
                    }
                }
                
                // Create symbol
                DocumentSymbol symbol = new DocumentSymbol();
                symbol.setName(name);
                symbol.setKind(mapTokenTypeToSymbolKind(type));
                symbol.setDetail(getSymbolDetail(type));
                
                // Set range
                Range range = calculateSymbolRange(tokens, i);
                symbol.setRange(range);
                symbol.setSelectionRange(nameToken != null ? 
                    tokenToRange(nameToken) : tokenToRange(token));
                
                // Initialize children list
                symbol.setChildren(new ArrayList<>());
                
                // Add to appropriate parent
                if (type == TokenType.BOUNDED_CONTEXT) {
                    symbols.add(Either.forRight(symbol));
                    currentContainer = symbol;
                } else if (currentContainer != null) {
                    currentContainer.getChildren().add(symbol);
                } else {
                    symbols.add(Either.forRight(symbol));
                }
            }
            
            // Create symbols for operations
            if (type == TokenType.WHEN && currentContainer != null) {
                // Get operation description
                StringBuilder description = new StringBuilder("when ");
                for (int j = i + 1; j < tokens.size() && j < i + 10; j++) {
                    Token t = tokens.get(j);
                    if (t.getType() == TokenType.LEFT_BRACE) break;
                    if (t.getType() != TokenType.NEWLINE) {
                        description.append(t.getLexeme()).append(" ");
                    }
                }
                
                DocumentSymbol operationSymbol = new DocumentSymbol();
                operationSymbol.setName(description.toString().trim());
                operationSymbol.setKind(SymbolKind.Method);
                operationSymbol.setDetail("operation");
                operationSymbol.setRange(calculateOperationRange(tokens, i));
                operationSymbol.setSelectionRange(tokenToRange(token));
                operationSymbol.setChildren(new ArrayList<>());
                
                currentContainer.getChildren().add(operationSymbol);
            }
            
            // Create symbols for fields
            if (type == TokenType.IDENTIFIER && i + 1 < tokens.size()) {
                Token nextToken = tokens.get(i + 1);
                if (nextToken.getType() == TokenType.COLON && currentContainer != null) {
                    // This is a field declaration
                    String fieldName = token.getLexeme();
                    String fieldType = getFieldType(tokens, i + 2);
                    
                    DocumentSymbol fieldSymbol = new DocumentSymbol();
                    fieldSymbol.setName(fieldName);
                    fieldSymbol.setKind(SymbolKind.Field);
                    fieldSymbol.setDetail(fieldType);
                    fieldSymbol.setRange(tokenToRange(token));
                    fieldSymbol.setSelectionRange(tokenToRange(token));
                    
                    currentContainer.getChildren().add(fieldSymbol);
                }
            }
        }
        
        return symbols;
    }
    
    /**
     * Check if a token type is a declaration keyword.
     */
    private static boolean isDeclarationKeyword(TokenType type) {
        return type == TokenType.BOUNDED_CONTEXT ||
               type == TokenType.AGGREGATE ||
               type == TokenType.ENTITY ||
               type == TokenType.VALUE_OBJECT ||
               type == TokenType.DOMAIN_SERVICE ||
               type == TokenType.DOMAIN_EVENT ||
               type == TokenType.FACTORY ||
               type == TokenType.REPOSITORY ||
               type == TokenType.SPECIFICATION;
    }
    
    /**
     * Map DDSL token type to LSP symbol kind.
     */
    private static SymbolKind mapTokenTypeToSymbolKind(TokenType type) {
        return switch (type) {
            case BOUNDED_CONTEXT -> SymbolKind.Namespace;
            case AGGREGATE, ENTITY, DOMAIN_SERVICE -> SymbolKind.Class;
            case VALUE_OBJECT -> SymbolKind.Struct;
            case DOMAIN_EVENT -> SymbolKind.Event;
            case FACTORY -> SymbolKind.Constructor;
            case REPOSITORY, SPECIFICATION -> SymbolKind.Interface;
            default -> SymbolKind.Variable;
        };
    }
    
    /**
     * Get detail text for a symbol.
     */
    private static String getSymbolDetail(TokenType type) {
        return switch (type) {
            case BOUNDED_CONTEXT -> "bounded context";
            case AGGREGATE -> "aggregate";
            case ENTITY -> "entity";
            case VALUE_OBJECT -> "value object";
            case DOMAIN_SERVICE -> "domain service";
            case DOMAIN_EVENT -> "domain event";
            case FACTORY -> "factory";
            case REPOSITORY -> "repository";
            case SPECIFICATION -> "specification";
            default -> "";
        };
    }
    
    /**
     * Calculate the full range of a symbol (from keyword to closing brace).
     */
    private static Range calculateSymbolRange(List<Token> tokens, int startIndex) {
        Token startToken = tokens.get(startIndex);
        int braceCount = 0;
        boolean foundBrace = false;
        Token endToken = startToken;
        
        for (int i = startIndex; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            
            if (token.getType() == TokenType.LEFT_BRACE) {
                braceCount++;
                foundBrace = true;
            } else if (token.getType() == TokenType.RIGHT_BRACE) {
                braceCount--;
                if (foundBrace && braceCount == 0) {
                    endToken = token;
                    break;
                }
            }
        }
        
        return new Range(
            new Position(startToken.getLine() - 1, startToken.getColumn() - 1),
            new Position(endToken.getLine() - 1, endToken.getColumn() + endToken.getLexeme().length() - 1)
        );
    }
    
    /**
     * Calculate the range of an operation (when block).
     */
    private static Range calculateOperationRange(List<Token> tokens, int startIndex) {
        Token startToken = tokens.get(startIndex);
        int braceCount = 0;
        boolean foundBrace = false;
        Token endToken = startToken;
        
        for (int i = startIndex; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            
            if (token.getType() == TokenType.LEFT_BRACE) {
                braceCount++;
                foundBrace = true;
            } else if (token.getType() == TokenType.RIGHT_BRACE) {
                braceCount--;
                if (foundBrace && braceCount == 0) {
                    endToken = token;
                    break;
                }
            }
        }
        
        return new Range(
            new Position(startToken.getLine() - 1, startToken.getColumn() - 1),
            new Position(endToken.getLine() - 1, endToken.getColumn() + endToken.getLexeme().length() - 1)
        );
    }
    
    /**
     * Get field type from tokens.
     */
    private static String getFieldType(List<Token> tokens, int startIndex) {
        StringBuilder type = new StringBuilder();
        
        for (int i = startIndex; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            TokenType tokenType = token.getType();
            
            // Stop at newline or certain keywords
            if (tokenType == TokenType.NEWLINE || 
                tokenType == TokenType.AT_SIGN ||
                tokenType == TokenType.IDENTITY ||
                tokenType == TokenType.REQUIRED) {
                break;
            }
            
            // Add type tokens
            if (isTypeToken(tokenType)) {
                type.append(token.getLexeme());
            } else if (tokenType == TokenType.LEFT_ANGLE) {
                type.append("<");
            } else if (tokenType == TokenType.RIGHT_ANGLE) {
                type.append(">");
            } else if (tokenType == TokenType.COMMA) {
                type.append(", ");
            } else if (tokenType == TokenType.QUESTION) {
                type.append("?");
            }
        }
        
        return type.toString().trim();
    }
    
    /**
     * Check if a token type is a type token.
     */
    private static boolean isTypeToken(TokenType type) {
        return type == TokenType.IDENTIFIER ||
               type == TokenType.STRING_TYPE ||
               type == TokenType.INT_TYPE ||
               type == TokenType.DECIMAL_TYPE ||
               type == TokenType.BOOLEAN_TYPE ||
               type == TokenType.DATETIME_TYPE ||
               type == TokenType.UUID_TYPE ||
               type == TokenType.LIST_TYPE ||
               type == TokenType.SET_TYPE ||
               type == TokenType.MAP_TYPE ||
               type == TokenType.VOID_TYPE;
    }
    
    /**
     * Convert a token to an LSP Range.
     */
    private static Range tokenToRange(Token token) {
        int line = token.getLine() - 1;
        int startCol = token.getColumn() - 1;
        int endCol = startCol + token.getLexeme().length();
        
        return new Range(
            new Position(line, startCol),
            new Position(line, endCol)
        );
    }
}
