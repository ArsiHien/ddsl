package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.*;

/**
 * Semantic Tokens provider for DDSL.
 * 
 * Provides rich syntax highlighting by mapping DDSL tokens to LSP semantic token types.
 * 
 * Architecture Decision: Why Semantic Tokens over TextMate?
 * 1. Server-side tokenization allows semantic understanding of the code
 * 2. Can differentiate between Entity names, ValueObject names, etc. based on context
 * 3. More accurate than regex-based TextMate grammars
 * 4. Reuses the existing DDSL lexer
 * 
 * LSP Semantic Tokens Encoding:
 * - Tokens are encoded as a flat array of integers
 * - Each token uses 5 integers: deltaLine, deltaStart, length, tokenType, tokenModifiers
 * - Delta encoding reduces payload size for incremental updates
 */
public class DdslSemanticTokens {
    
    // ========== Standard Semantic Token Types (LSP 3.16+) ==========
    // These match the standard types supported by Monaco/VS Code
    
    /** Index constants for token types */
    private static final int TYPE_NAMESPACE = 0;
    private static final int TYPE_TYPE = 1;
    private static final int TYPE_CLASS = 2;
    private static final int TYPE_ENUM = 3;
    private static final int TYPE_INTERFACE = 4;
    private static final int TYPE_STRUCT = 5;
    private static final int TYPE_TYPE_PARAMETER = 6;
    private static final int TYPE_PARAMETER = 7;
    private static final int TYPE_VARIABLE = 8;
    private static final int TYPE_PROPERTY = 9;
    private static final int TYPE_ENUM_MEMBER = 10;
    private static final int TYPE_EVENT = 11;
    private static final int TYPE_FUNCTION = 12;
    private static final int TYPE_METHOD = 13;
    private static final int TYPE_MACRO = 14;
    private static final int TYPE_KEYWORD = 15;
    private static final int TYPE_MODIFIER = 16;
    private static final int TYPE_COMMENT = 17;
    private static final int TYPE_STRING = 18;
    private static final int TYPE_NUMBER = 19;
    private static final int TYPE_REGEXP = 20;
    private static final int TYPE_OPERATOR = 21;
    private static final int TYPE_DECORATOR = 22;
    
    /** Standard semantic token type names */
    private static final List<String> TOKEN_TYPES = List.of(
        "namespace",      // 0:  BoundedContext
        "type",           // 1:  Type references
        "class",          // 2:  Aggregate, Entity
        "enum",           // 3:  Enumerations
        "interface",      // 4:  Repository, Specification
        "struct",         // 5:  ValueObject
        "typeParameter",  // 6:  Generic type parameters
        "parameter",      // 7:  Method parameters
        "variable",       // 8:  Local variables
        "property",       // 9:  Field declarations
        "enumMember",     // 10: Enum values
        "event",          // 11: DomainEvent
        "function",       // 12: Factory methods
        "method",         // 13: Operations, methods
        "macro",          // 14: (unused)
        "keyword",        // 15: Keywords
        "modifier",       // 16: Annotations
        "comment",        // 17: Comments
        "string",         // 18: String literals
        "number",         // 19: Number literals
        "regexp",         // 20: (unused)
        "operator",       // 21: Operators
        "decorator"       // 22: @annotations
    );
    
    /** Index constants for token modifiers */
    private static final int MOD_DECLARATION = 0;
    private static final int MOD_DEFINITION = 1;
    private static final int MOD_READONLY = 2;
    private static final int MOD_STATIC = 3;
    private static final int MOD_DEPRECATED = 4;
    private static final int MOD_ABSTRACT = 5;
    private static final int MOD_ASYNC = 6;
    private static final int MOD_MODIFICATION = 7;
    private static final int MOD_DOCUMENTATION = 8;
    private static final int MOD_DEFAULT_LIBRARY = 9;
    
    /** Standard semantic token modifiers */
    private static final List<String> TOKEN_MODIFIERS = List.of(
        "declaration",
        "definition",
        "readonly",
        "static",
        "deprecated",
        "abstract",
        "async",
        "modification",
        "documentation",
        "defaultLibrary"
    );
    
    /**
     * Get the semantic tokens legend for capability negotiation.
     */
    public static SemanticTokensLegend getLegend() {
        return new SemanticTokensLegend(TOKEN_TYPES, TOKEN_MODIFIERS);
    }
    
    /**
     * Encode tokens to LSP semantic token format.
     * 
     * @param tokens the lexer tokens to encode
     * @return semantic tokens in LSP format
     */
    public static SemanticTokens encode(List<Token> tokens) {
        List<Integer> data = new ArrayList<>();
        
        int prevLine = 0;
        int prevChar = 0;
        
        for (Token token : tokens) {
            // Skip EOF and whitespace-only tokens
            if (token.getType() == TokenType.EOF || 
                token.getType() == TokenType.NEWLINE ||
                token.getType() == TokenType.INDENT ||
                token.getType() == TokenType.DEDENT) {
                continue;
            }
            
            // Get semantic token type for this DDSL token
            int tokenType = getSemanticTokenType(token);
            if (tokenType < 0) {
                continue; // Skip tokens we don't want to highlight
            }
            
            // Get modifiers bitmap
            int modifiers = getSemanticTokenModifiers(token);
            
            // Calculate deltas (0-based line/column from token which is 1-based)
            int line = token.getLine() - 1;
            int character = token.getColumn() - 1;
            int length = token.getLexeme().length();
            
            int deltaLine = line - prevLine;
            int deltaStart = (deltaLine == 0) ? (character - prevChar) : character;
            
            // Add the 5 integers for this token
            data.add(deltaLine);
            data.add(deltaStart);
            data.add(length);
            data.add(tokenType);
            data.add(modifiers);
            
            prevLine = line;
            prevChar = character;
        }
        
        return new SemanticTokens(data);
    }
    
    /**
     * Map DDSL token type to LSP semantic token type.
     */
    private static int getSemanticTokenType(Token token) {
        TokenType type = token.getType();
        
        return switch (type) {
            // Structural keywords → keyword
            case BOUNDED_CONTEXT -> TYPE_NAMESPACE;
            case AGGREGATE, ENTITY -> TYPE_CLASS;
            case VALUE_OBJECT -> TYPE_STRUCT;
            case DOMAIN_SERVICE -> TYPE_CLASS;
            case DOMAIN_EVENT -> TYPE_EVENT;
            case FACTORY -> TYPE_FUNCTION;
            case REPOSITORY, SPECIFICATION -> TYPE_INTERFACE;
            case USE_CASE -> TYPE_METHOD;
            
            // Block keywords → keyword
            case UBIQUITOUS_LANGUAGE, DOMAIN, EVENTS, FACTORIES, REPOSITORIES,
                 SPECIFICATIONS, USE_CASES, INVARIANTS, OPERATIONS, METADATA,
                 INPUT, OUTPUT, FLOW -> TYPE_KEYWORD;
            
            // Behavioral keywords → keyword
            case WHEN, REQUIRE, GIVEN, THEN, EMIT, RETURN, THAT, EVENT,
                 CREATING, SET, CHANGE, RECORD, CALCULATE, CREATE, ADD,
                 REMOVE, SAVE, IF, OTHERWISE, FOR, EACH -> TYPE_KEYWORD;
            
            // Type keywords → type
            case STRING_TYPE, INT_TYPE, DECIMAL_TYPE, BOOLEAN_TYPE,
                 DATETIME_TYPE, UUID_TYPE, LIST_TYPE, SET_TYPE, MAP_TYPE,
                 VOID_TYPE -> TYPE_TYPE;
            
            // Natural language connectors → keyword (lighter weight)
            case WITH, FROM, TO, IN, ON, AT, OF, BY, FOR_PREP, THE, A,
                 AND, OR, NOT, AS, IS, ARE, WAS, WERE, EQUALS, EQUAL,
                 GREATER, LESS, THAN, LEAST, MOST, EXCEEDS, WITHIN, ONE,
                 ALL, ANY, NO, ITEMS, HAVE, HAS, BEEN, EMPTY, EXISTS,
                 SYSTEM, VALID, CREATED, CALCULATED, DETERMINED,
                 SUM, COUNT, WHERE, MATCHES, COMBINES,
                 PLUS, MINUS, TIMES, DIVIDED,
                 NOW, TODAY, LAST, DAYS, NEW, GENERATED,
                 SUCCESS, FAILURE, RESULT -> TYPE_KEYWORD;
            
            // Annotations → decorator
            case AT_SIGN, IDENTITY, REQUIRED, UNIQUE, MIN, MAX, MIN_LENGTH,
                 MAX_LENGTH, PRECISION, DEFAULT, COMPUTED, PATTERN -> TYPE_DECORATOR;
            
            // Literals
            case STRING_LITERAL -> TYPE_STRING;
            case INTEGER_LITERAL, DECIMAL_LITERAL -> TYPE_NUMBER;
            case TRUE, FALSE -> TYPE_KEYWORD;
            case NULL -> TYPE_KEYWORD;
            
            // Operators
            case GT, LT, GTE, LTE, NEQ, EQ, PLUS_SYMBOL, MINUS_SYMBOL,
                 STAR, SLASH, ASSIGN -> TYPE_OPERATOR;
            
            // Delimiters - don't highlight
            case LEFT_BRACE, RIGHT_BRACE, LEFT_PAREN, RIGHT_PAREN,
                 LEFT_BRACKET, RIGHT_BRACKET, LEFT_ANGLE, RIGHT_ANGLE,
                 COLON, SEMICOLON, COMMA, DOT, DASH, QUESTION -> -1;
            
            // Identifiers
            case IDENTIFIER -> TYPE_VARIABLE;
            
            // Error tokens - could highlight as error
            case ERROR -> -1;
            
            default -> -1;
        };
    }
    
    /**
     * Get semantic token modifiers for a token.
     * Returns a bitmap of active modifiers.
     */
    private static int getSemanticTokenModifiers(Token token) {
        int modifiers = 0;
        TokenType type = token.getType();
        
        // Declaration modifiers
        if (isDeclarationKeyword(type)) {
            modifiers |= (1 << MOD_DECLARATION);
        }
        
        // Definition modifiers for type definitions
        if (isTypeDefinitionKeyword(type)) {
            modifiers |= (1 << MOD_DEFINITION);
        }
        
        // Readonly for constants
        if (type == TokenType.TRUE || type == TokenType.FALSE || type == TokenType.NULL) {
            modifiers |= (1 << MOD_READONLY);
        }
        
        return modifiers;
    }
    
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
    
    private static boolean isTypeDefinitionKeyword(TokenType type) {
        return type == TokenType.AGGREGATE ||
               type == TokenType.ENTITY ||
               type == TokenType.VALUE_OBJECT ||
               type == TokenType.DOMAIN_EVENT;
    }
}
