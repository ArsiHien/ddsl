package uet.ndh.ddsl.parser.lexer;

import java.util.*;

/**
 * Lexical analyzer (Scanner) for the DDSL language.
 * 
 * The scanner reads source code character by character and produces
 * a stream of tokens. It handles:
 * - Keywords and identifiers
 * - String, number, and boolean literals
 * - Operators and delimiters
 * - Comments (single-line // and multi-line /* *&#47;)
 * - Natural language connectors and predicates
 * - Annotations (@required, @min, etc.)
 * 
 * Following "Crafting Interpreters" Chapter 4 design patterns.
 */
public class Scanner {
    
    /** The source code being scanned */
    private final String source;
    
    /** The list of tokens produced */
    private final List<Token> tokens = new ArrayList<>();
    
    /** Start position of the current lexeme */
    private int start = 0;
    
    /** Current position in the source */
    private int current = 0;
    
    /** Current line number (1-based) */
    private int line = 1;
    
    /** Current column number (1-based) */
    private int column = 1;
    
    /** Column at the start of the current lexeme */
    private int startColumn = 1;
    
    /** List of lexical errors encountered */
    private final List<LexicalError> errors = new ArrayList<>();
    
    /** Map of reserved keywords to token types */
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    
    static {
        // Structural keywords
        KEYWORDS.put("BoundedContext", TokenType.BOUNDED_CONTEXT);
        KEYWORDS.put("bounded-context", TokenType.BOUNDED_CONTEXT);
        KEYWORDS.put("ubiquitous-language", TokenType.UBIQUITOUS_LANGUAGE);
        KEYWORDS.put("domain", TokenType.DOMAIN);
        KEYWORDS.put("events", TokenType.EVENTS);
        KEYWORDS.put("factories", TokenType.FACTORIES);
        KEYWORDS.put("repositories", TokenType.REPOSITORIES);
        KEYWORDS.put("specifications", TokenType.SPECIFICATIONS);
        KEYWORDS.put("use-cases", TokenType.USE_CASES);
        
        // Domain model keywords
        KEYWORDS.put("Aggregate", TokenType.AGGREGATE);
        KEYWORDS.put("Entity", TokenType.ENTITY);
        KEYWORDS.put("ValueObject", TokenType.VALUE_OBJECT);
        KEYWORDS.put("value-object", TokenType.VALUE_OBJECT);
        KEYWORDS.put("DomainService", TokenType.DOMAIN_SERVICE);
        KEYWORDS.put("DomainEvent", TokenType.DOMAIN_EVENT);
        KEYWORDS.put("Factory", TokenType.FACTORY);
        KEYWORDS.put("Repository", TokenType.REPOSITORY);
        KEYWORDS.put("Specification", TokenType.SPECIFICATION);
        KEYWORDS.put("UseCase", TokenType.USE_CASE);
        
        // Structural blocks
        KEYWORDS.put("invariants", TokenType.INVARIANTS);
        KEYWORDS.put("operations", TokenType.OPERATIONS);
        KEYWORDS.put("metadata", TokenType.METADATA);
        KEYWORDS.put("input", TokenType.INPUT);
        KEYWORDS.put("output", TokenType.OUTPUT);
        KEYWORDS.put("flow", TokenType.FLOW);
        
        // Behavioral keywords
        KEYWORDS.put("when", TokenType.WHEN);
        KEYWORDS.put("require", TokenType.REQUIRE);
        KEYWORDS.put("given", TokenType.GIVEN);
        KEYWORDS.put("then", TokenType.THEN);
        KEYWORDS.put("emit", TokenType.EMIT);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("that", TokenType.THAT);
        KEYWORDS.put("event", TokenType.EVENT);
        
        // Factory keywords
        KEYWORDS.put("creating", TokenType.CREATING);
        
        // Statement keywords
        KEYWORDS.put("set", TokenType.SET);
        KEYWORDS.put("change", TokenType.CHANGE);
        KEYWORDS.put("record", TokenType.RECORD);
        KEYWORDS.put("calculate", TokenType.CALCULATE);
        KEYWORDS.put("create", TokenType.CREATE);
        KEYWORDS.put("add", TokenType.ADD);
        KEYWORDS.put("remove", TokenType.REMOVE);
        KEYWORDS.put("save", TokenType.SAVE);
        KEYWORDS.put("enable", TokenType.ENABLE);
        KEYWORDS.put("disable", TokenType.DISABLE);
        
        // Control flow
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("otherwise", TokenType.OTHERWISE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("each", TokenType.EACH);
        
        // Natural language connectors
        KEYWORDS.put("with", TokenType.WITH);
        KEYWORDS.put("from", TokenType.FROM);
        KEYWORDS.put("to", TokenType.TO);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("on", TokenType.ON);
        KEYWORDS.put("at", TokenType.AT);
        KEYWORDS.put("of", TokenType.OF);
        KEYWORDS.put("by", TokenType.BY);
        KEYWORDS.put("the", TokenType.THE);
        KEYWORDS.put("a", TokenType.A);
        KEYWORDS.put("an", TokenType.A);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("as", TokenType.AS);
        
        // Comparison predicates
        KEYWORDS.put("is", TokenType.IS);
        KEYWORDS.put("are", TokenType.ARE);
        KEYWORDS.put("was", TokenType.WAS);
        KEYWORDS.put("were", TokenType.WERE);
        KEYWORDS.put("equals", TokenType.EQUALS);
        KEYWORDS.put("equal", TokenType.EQUAL);
        KEYWORDS.put("greater", TokenType.GREATER);
        KEYWORDS.put("less", TokenType.LESS);
        KEYWORDS.put("than", TokenType.THAN);
        KEYWORDS.put("least", TokenType.LEAST);
        KEYWORDS.put("most", TokenType.MOST);
        KEYWORDS.put("exceeds", TokenType.EXCEEDS);
        KEYWORDS.put("within", TokenType.WITHIN);
        KEYWORDS.put("one", TokenType.ONE);
        
        // Collection predicates
        KEYWORDS.put("all", TokenType.ALL);
        KEYWORDS.put("any", TokenType.ANY);
        KEYWORDS.put("no", TokenType.NO);
        KEYWORDS.put("items", TokenType.ITEMS);
        KEYWORDS.put("have", TokenType.HAVE);
        KEYWORDS.put("has", TokenType.HAS);
        KEYWORDS.put("been", TokenType.BEEN);
        KEYWORDS.put("empty", TokenType.EMPTY);
        KEYWORDS.put("exists", TokenType.EXISTS);
        KEYWORDS.put("system", TokenType.SYSTEM);
        KEYWORDS.put("valid", TokenType.VALID);
        KEYWORDS.put("created", TokenType.CREATED);
        KEYWORDS.put("calculated", TokenType.CALCULATED);
        KEYWORDS.put("determined", TokenType.DETERMINED);
        KEYWORDS.put("fetched", TokenType.FETCHED);
        KEYWORDS.put("mapped", TokenType.MAPPED);
        KEYWORDS.put("does", TokenType.DOES);
        
        // Aggregation keywords
        KEYWORDS.put("sum", TokenType.SUM);
        KEYWORDS.put("count", TokenType.COUNT);
        KEYWORDS.put("maximum", TokenType.MAXIMUM);
        KEYWORDS.put("minimum", TokenType.MINIMUM);
        KEYWORDS.put("average", TokenType.AVERAGE);
        KEYWORDS.put("across", TokenType.ACROSS);
        KEYWORDS.put("grouped", TokenType.GROUPED);
        KEYWORDS.put("where", TokenType.WHERE);
        KEYWORDS.put("matches", TokenType.MATCHES);
        KEYWORDS.put("combines", TokenType.COMBINES);
        KEYWORDS.put("satisfies", TokenType.SATISFIES);
        KEYWORDS.put("eligible", TokenType.ELIGIBLE);
        
        // Arithmetic operators (natural language)
        KEYWORDS.put("plus", TokenType.PLUS);
        KEYWORDS.put("minus", TokenType.MINUS);
        KEYWORDS.put("times", TokenType.TIMES);
        KEYWORDS.put("divided", TokenType.DIVIDED);
        
        // Temporal keywords
        KEYWORDS.put("now", TokenType.NOW);
        KEYWORDS.put("today", TokenType.TODAY);
        KEYWORDS.put("yesterday", TokenType.YESTERDAY);
        KEYWORDS.put("tomorrow", TokenType.TOMORROW);
        KEYWORDS.put("last", TokenType.LAST);
        KEYWORDS.put("next", TokenType.NEXT);
        KEYWORDS.put("ago", TokenType.AGO);
        KEYWORDS.put("before", TokenType.BEFORE);
        KEYWORDS.put("after", TokenType.AFTER);
        KEYWORDS.put("between", TokenType.BETWEEN);
        KEYWORDS.put("occurred", TokenType.OCCURRED);
        KEYWORDS.put("second", TokenType.SECONDS);
        KEYWORDS.put("seconds", TokenType.SECONDS);
        KEYWORDS.put("minute", TokenType.MINUTES);
        KEYWORDS.put("minutes", TokenType.MINUTES);
        KEYWORDS.put("hour", TokenType.HOURS);
        KEYWORDS.put("hours", TokenType.HOURS);
        KEYWORDS.put("day", TokenType.DAYS);
        KEYWORDS.put("days", TokenType.DAYS);
        KEYWORDS.put("week", TokenType.WEEKS);
        KEYWORDS.put("weeks", TokenType.WEEKS);
        KEYWORDS.put("month", TokenType.MONTHS);
        KEYWORDS.put("months", TokenType.MONTHS);
        KEYWORDS.put("year", TokenType.YEARS);
        KEYWORDS.put("years", TokenType.YEARS);
        KEYWORDS.put("new", TokenType.NEW);
        KEYWORDS.put("generated", TokenType.GENERATED);
        
        // State machine keywords
        KEYWORDS.put("state", TokenType.STATE);
        KEYWORDS.put("machine", TokenType.MACHINE);
        KEYWORDS.put("states", TokenType.STATES);
        KEYWORDS.put("transitions", TokenType.TRANSITIONS);
        KEYWORDS.put("guards", TokenType.GUARDS);
        KEYWORDS.put("entry", TokenType.ENTRY);
        KEYWORDS.put("exit", TokenType.EXIT);
        KEYWORDS.put("entering", TokenType.ENTERING);
        KEYWORDS.put("leaving", TokenType.LEAVING);
        KEYWORDS.put("initial", TokenType.INITIAL);
        KEYWORDS.put("final", TokenType.FINAL);
        KEYWORDS.put("cannot", TokenType.CANNOT);
        KEYWORDS.put("must", TokenType.MUST);
        KEYWORDS.put("transition", TokenType.TRANSITION);
        KEYWORDS.put("only", TokenType.ONLY);
        KEYWORDS.put("always", TokenType.ALWAYS);
        KEYWORDS.put("never", TokenType.NEVER);
        
        // Error accumulation keywords
        KEYWORDS.put("collect", TokenType.COLLECT);
        KEYWORDS.put("errors", TokenType.ERRORS);
        KEYWORDS.put("warning", TokenType.WARNING);
        KEYWORDS.put("fail", TokenType.FAIL);
        KEYWORDS.put("critical", TokenType.CRITICAL);
        KEYWORDS.put("up", TokenType.UP);
        KEYWORDS.put("group", TokenType.GROUP);
        
        // Match expression keywords
        KEYWORDS.put("match", TokenType.MATCH);
        
        // String operation keywords
        KEYWORDS.put("contains", TokenType.CONTAINS);
        KEYWORDS.put("starts", TokenType.STARTS);
        KEYWORDS.put("ends", TokenType.ENDS);
        KEYWORDS.put("blank", TokenType.BLANK);
        KEYWORDS.put("format", TokenType.FORMAT);
        KEYWORDS.put("url", TokenType.URL);
        KEYWORDS.put("alphanumeric", TokenType.ALPHANUMERIC);
        KEYWORDS.put("numeric", TokenType.NUMERIC);
        KEYWORDS.put("truncated", TokenType.TRUNCATED);
        KEYWORDS.put("concatenated", TokenType.CONCATENATED);
        KEYWORDS.put("replaced", TokenType.REPLACED);
        KEYWORDS.put("length", TokenType.LENGTH);
        KEYWORDS.put("characters", TokenType.CHARACTERS);
        KEYWORDS.put("more", TokenType.MORE);
        KEYWORDS.put("first", TokenType.FIRST);
        KEYWORDS.put("exactly", TokenType.EXACTLY);
        
        // Result keywords
        KEYWORDS.put("success", TokenType.SUCCESS);
        KEYWORDS.put("failure", TokenType.FAILURE);
        KEYWORDS.put("Result", TokenType.RESULT);
        
        // Type keywords
        KEYWORDS.put("String", TokenType.STRING_TYPE);
        KEYWORDS.put("Int", TokenType.INT_TYPE);
        KEYWORDS.put("Integer", TokenType.INT_TYPE);
        KEYWORDS.put("Decimal", TokenType.DECIMAL_TYPE);
        KEYWORDS.put("Double", TokenType.DECIMAL_TYPE);
        KEYWORDS.put("Boolean", TokenType.BOOLEAN_TYPE);
        KEYWORDS.put("DateTime", TokenType.DATETIME_TYPE);
        KEYWORDS.put("UUID", TokenType.UUID_TYPE);
        KEYWORDS.put("List", TokenType.LIST_TYPE);
        KEYWORDS.put("Set", TokenType.SET_TYPE);
        KEYWORDS.put("Map", TokenType.MAP_TYPE);
        KEYWORDS.put("void", TokenType.VOID_TYPE);
        
        // Boolean literals
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("null", TokenType.NULL);
        
        // Annotation keywords (without @)
        KEYWORDS.put("identity", TokenType.IDENTITY);
        KEYWORDS.put("required", TokenType.REQUIRED);
        KEYWORDS.put("unique", TokenType.UNIQUE);
        KEYWORDS.put("min", TokenType.MIN);
        KEYWORDS.put("max", TokenType.MAX);
        KEYWORDS.put("minLength", TokenType.MIN_LENGTH);
        KEYWORDS.put("maxLength", TokenType.MAX_LENGTH);
        KEYWORDS.put("precision", TokenType.PRECISION);
        KEYWORDS.put("default", TokenType.DEFAULT);
        KEYWORDS.put("computed", TokenType.COMPUTED);
        KEYWORDS.put("pattern", TokenType.PATTERN);
    }
    
    /**
     * Creates a new scanner for the given source code.
     * 
     * @param source the source code to scan
     */
    public Scanner(String source) {
        this.source = source;
    }
    
    /**
     * Scans the source and returns the list of tokens.
     * 
     * @return the list of tokens
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme
            start = current;
            startColumn = column;
            scanToken();
        }
        
        tokens.add(Token.eof(line, column, current));
        return tokens;
    }
    
    /**
     * Scans a single token.
     */
    private void scanToken() {
        char c = advance();
        
        switch (c) {
            // Single-character tokens
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '[': addToken(TokenType.LEFT_BRACKET); break;
            case ']': addToken(TokenType.RIGHT_BRACKET); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '?': addToken(TokenType.QUESTION); break;
            case '+': addToken(TokenType.PLUS_SYMBOL); break;
            case '*': addToken(TokenType.STAR); break;
            
            // Two-character tokens
            case '-':
                addToken(TokenType.DASH);
                break;
                
            case ':':
                addToken(TokenType.COLON);
                break;
                
            case '<':
                if (match('=')) {
                    addToken(TokenType.LTE);
                } else {
                    addToken(TokenType.LEFT_ANGLE);
                }
                break;
                
            case '>':
                if (match('=')) {
                    addToken(TokenType.GTE);
                } else {
                    addToken(TokenType.RIGHT_ANGLE);
                }
                break;
                
            case '!':
                if (match('=')) {
                    addToken(TokenType.NEQ);
                } else {
                    addToken(TokenType.NOT);
                }
                break;
                
            case '=':
                if (match('=')) {
                    addToken(TokenType.EQ);
                } else {
                    addToken(TokenType.ASSIGN);
                }
                break;
                
            case '/':
                if (match('/')) {
                    // Single-line comment
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    // Multi-line comment
                    blockComment();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
                
            case '@':
                annotation();
                break;
                
            // Whitespace
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;
                
            case '\n':
                line++;
                column = 1;
                break;
                
            // String literals
            case '"':
                string();
                break;
                
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    error("Unexpected character: '" + c + "'");
                }
                break;
        }
    }
    
    /**
     * Scans an identifier or keyword.
     */
    private void identifier() {
        while (isAlphaNumeric(peek()) || peek() == '-') {
            advance();
        }
        
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.get(text);
        
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        
        addToken(type);
    }
    
    /**
     * Scans a number literal.
     */
    private void number() {
        // Scan integer part
        while (isDigit(peek()) || peek() == ',' || peek() == '_') {
            char c = peek();
            if (c == ',' || c == '_') {
                // Allow thousand separators, but skip them
                advance();
            } else {
                advance();
            }
        }
        
        // Look for a decimal point
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the '.'
            advance();
            
            // Scan fractional part
            while (isDigit(peek())) {
                advance();
            }
            
            String text = source.substring(start, current)
                    .replace(",", "")
                    .replace("_", "");
            double value = Double.parseDouble(text);
            addToken(TokenType.DECIMAL_LITERAL, value);
        } else {
            String text = source.substring(start, current)
                    .replace(",", "")
                    .replace("_", "");
            try {
                long value = Long.parseLong(text);
                addToken(TokenType.INTEGER_LITERAL, value);
            } catch (NumberFormatException e) {
                error("Invalid number format: " + text);
            }
        }
    }
    
    /**
     * Scans a string literal.
     */
    private void string() {
        StringBuilder value = new StringBuilder();
        
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                column = 1;
            }
            
            // Handle escape sequences
            if (peek() == '\\' && peekNext() != '\0') {
                advance(); // consume backslash
                char escaped = advance();
                switch (escaped) {
                    case 'n': value.append('\n'); break;
                    case 't': value.append('\t'); break;
                    case 'r': value.append('\r'); break;
                    case '"': value.append('"'); break;
                    case '\\': value.append('\\'); break;
                    default:
                        value.append('\\').append(escaped);
                }
            } else {
                value.append(advance());
            }
        }
        
        if (isAtEnd()) {
            error("Unterminated string");
            return;
        }
        
        // Consume the closing "
        advance();
        
        addToken(TokenType.STRING_LITERAL, value.toString());
    }
    
    /**
     * Scans an annotation.
     */
    private void annotation() {
        // @ has already been consumed
        addToken(TokenType.AT_SIGN);
        
        // Skip any whitespace between @ and the annotation name
        while (peek() == ' ' || peek() == '\t') {
            advance();
        }
        
        // Start a new token for the annotation name
        start = current;
        startColumn = column;
        
        // Scan the annotation identifier
        if (isAlpha(peek())) {
            while (isAlphaNumeric(peek())) {
                advance();
            }
            
            String text = source.substring(start, current);
            TokenType type = KEYWORDS.get(text);
            
            if (type != null && isAnnotationType(type)) {
                addToken(type);
            } else {
                addToken(TokenType.IDENTIFIER);
            }
        }
    }
    
    /**
     * Check if a token type is an annotation type.
     */
    private boolean isAnnotationType(TokenType type) {
        return type == TokenType.IDENTITY ||
               type == TokenType.REQUIRED ||
               type == TokenType.UNIQUE ||
               type == TokenType.MIN ||
               type == TokenType.MAX ||
               type == TokenType.MIN_LENGTH ||
               type == TokenType.MAX_LENGTH ||
               type == TokenType.PRECISION ||
               type == TokenType.DEFAULT ||
               type == TokenType.COMPUTED ||
               type == TokenType.PATTERN;
    }
    
    /**
     * Scans a block comment.
     */
    private void blockComment() {
        int nesting = 1;
        
        while (nesting > 0 && !isAtEnd()) {
            if (peek() == '/' && peekNext() == '*') {
                advance();
                advance();
                nesting++;
            } else if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                nesting--;
            } else {
                if (peek() == '\n') {
                    line++;
                    column = 1;
                }
                advance();
            }
        }
        
        if (nesting > 0) {
            error("Unterminated block comment");
        }
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Check if we've reached the end of the source.
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }
    
    /**
     * Advance and return the current character.
     */
    private char advance() {
        char c = source.charAt(current);
        current++;
        column++;
        return c;
    }
    
    /**
     * Look at the current character without consuming it.
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }
    
    /**
     * Look at the next character without consuming it.
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }
    
    /**
     * Conditionally advance if the current character matches.
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        
        current++;
        column++;
        return true;
    }
    
    /**
     * Check if a character is a digit.
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    /**
     * Check if a character is alphabetic.
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }
    
    /**
     * Check if a character is alphanumeric.
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    
    /**
     * Add a token with no literal value.
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }
    
    /**
     * Add a token with a literal value.
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line, startColumn, start));
    }
    
    /**
     * Record a lexical error.
     */
    private void error(String message) {
        errors.add(new LexicalError(message, line, column));
        // Add an error token to allow error recovery
        tokens.add(Token.error(message, line, column, current));
    }
    
    /**
     * Get the list of errors encountered during scanning.
     */
    public List<LexicalError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    /**
     * Check if there were any errors during scanning.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Represents a lexical error.
     */
    public static class LexicalError {
        private final String message;
        private final int line;
        private final int column;
        
        public LexicalError(String message, int line, int column) {
            this.message = message;
            this.line = line;
            this.column = column;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getLine() {
            return line;
        }
        
        public int getColumn() {
            return column;
        }
        
        @Override
        public String toString() {
            return String.format("[%d:%d] Lexical error: %s", line, column, message);
        }
    }
}
