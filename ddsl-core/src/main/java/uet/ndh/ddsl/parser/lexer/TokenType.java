package uet.ndh.ddsl.parser.lexer;

/**
 * Enumeration of all token types in the DDSL language.
 * 
 * Based on the concrete syntax defined in the language design document,
 * this enum captures all lexical elements including:
 * - Keywords for DDD constructs (BoundedContext, Aggregate, Entity, etc.)
 * - Behavioral keywords (when, require, given, then, emit, return)
 * - Natural language connectors (is, are, with, from, to, etc.)
 * - Operators and delimiters
 * - Literals and identifiers
 */
public enum TokenType {
    
    // ========== STRUCTURAL KEYWORDS ==========
    
    /** Top-level bounded context declaration */
    BOUNDED_CONTEXT,
    
    /** Ubiquitous language section */
    UBIQUITOUS_LANGUAGE,
    
    /** Domain section */
    DOMAIN,
    
    /** Events section */
    EVENTS,
    
    /** Factories section */
    FACTORIES,
    
    /** Repositories section */
    REPOSITORIES,
    
    /** Specifications section */
    SPECIFICATIONS,
    
    /** Use cases section */
    USE_CASES,
    
    // ========== DOMAIN MODEL KEYWORDS ==========
    
    /** Aggregate root declaration */
    AGGREGATE,

    /** Enum declaration */
    ENUM,
    
    /** Entity declaration */
    ENTITY,
    
    /** Value object declaration */
    VALUE_OBJECT,
    
    /** Domain service declaration */
    DOMAIN_SERVICE,
    
    /** Domain event declaration */
    DOMAIN_EVENT,
    
    /** Factory declaration */
    FACTORY,
    
    /** Repository declaration */
    REPOSITORY,
    
    /** Specification declaration */
    SPECIFICATION,
    
    /** Use case declaration */
    USE_CASE,
    
    // ========== STRUCTURAL BLOCKS ==========
    
    /** Invariants block */
    INVARIANTS,
    
    /** Operations block */
    OPERATIONS,
    
    /** Metadata block */
    METADATA,
    
    /** Input block for use cases */
    INPUT,
    
    /** Output block for use cases */
    OUTPUT,
    
    /** Flow block for use cases */
    FLOW,
    
    // ========== BEHAVIORAL KEYWORDS ==========
    
    /** Behavior trigger keyword */
    WHEN,
    
    /** Require clause keyword */
    REQUIRE,
    
    /** Given clause keyword */
    GIVEN,
    
    /** Then clause keyword */
    THEN,
    
    /** Emit clause keyword */
    EMIT,
    
    /** Return keyword */
    RETURN,
    
    /** That keyword (used with require) */
    THAT,
    
    /** Event keyword (used with emit) */
    EVENT,
    
    // ========== FACTORY KEYWORDS ==========
    
    /** Creating keyword for factory rules */
    CREATING,
    
    // ========== STATEMENT KEYWORDS ==========
    
    /** Set assignment keyword */
    SET,
    
    /** Change assignment keyword */
    CHANGE,
    
    /** Record assignment keyword */
    RECORD,
    
    /** Calculate assignment keyword */
    CALCULATE,
    
    /** Create assignment keyword */
    CREATE,
    
    /** Add to collection keyword */
    ADD,
    
    /** Remove from collection keyword */
    REMOVE,
    
    /** Save to repository keyword */
    SAVE,
    
    /** Enable keyword (boolean true) */
    ENABLE,
    
    /** Disable keyword (boolean false) */
    DISABLE,
    
    /** Fetched keyword (for given clause) */
    FETCHED,
    
    /** Mapped keyword (for given clause) */
    MAPPED,
    
    /** Does keyword (for "does not exist") */
    DOES,
    
    // ========== CONTROL FLOW KEYWORDS ==========
    
    /** If conditional */
    IF,
    
    /** Otherwise (else) */
    OTHERWISE,
    
    /** For loop */
    FOR,
    
    /** Each iterator */
    EACH,
    
    // ========== NATURAL LANGUAGE CONNECTORS ==========
    
    /** Preposition: with */
    WITH,
    
    /** Preposition: from */
    FROM,
    
    /** Preposition: to */
    TO,
    
    /** Preposition: in */
    IN,
    
    /** Preposition: on */
    ON,
    
    /** Preposition: at */
    AT,
    
    /** Preposition: of */
    OF,
    
    /** Preposition: by */
    BY,
    
    /** Preposition: for */
    FOR_PREP,
    
    /** Article: the */
    THE,
    
    /** Article: a/an */
    A,
    
    /** Conjunction: and */
    AND,
    
    /** Conjunction: or */
    OR,
    
    /** Negation: not */
    NOT,
    
    /** Existence: as */
    AS,
    
    // ========== COMPARISON PREDICATES ==========
    
    /** Equality: is */
    IS,
    
    /** Equality: are */
    ARE,
    
    /** Equality: was */
    WAS,
    
    /** Equality: were */
    WERE,
    
    /** Equality: equals */
    EQUALS,
    
    /** Equality: equal */
    EQUAL,
    
    /** Comparison: greater */
    GREATER,
    
    /** Comparison: less */
    LESS,
    
    /** Comparison: than */
    THAN,
    
    /** Comparison: least */
    LEAST,
    
    /** Comparison: most */
    MOST,
    
    /** Comparison: exceeds */
    EXCEEDS,
    
    /** Comparison: within */
    WITHIN,
    
    /** Membership: one */
    ONE,
    
    // ========== COLLECTION PREDICATES ==========
    
    /** Quantifier: all */
    ALL,
    
    /** Quantifier: any */
    ANY,
    
    /** Quantifier: no */
    NO,
    
    /** Collection: items */
    ITEMS,
    
    /** Predicate: have */
    HAVE,
    
    /** Predicate: has */
    HAS,
    
    /** Predicate: been */
    BEEN,
    
    /** Predicate: empty */
    EMPTY,
    
    /** Predicate: exists */
    EXISTS,
    
    /** System reference */
    SYSTEM,
    
    /** Predicate: valid */
    VALID,
    
    /** Transformation: created */
    CREATED,
    
    /** Transformation: calculated */
    CALCULATED,
    
    /** Transformation: determined */
    DETERMINED,
    
    // ========== AGGREGATION KEYWORDS ==========
    
    /** Aggregation: sum */
    SUM,
    
    /** Aggregation: count */
    COUNT,
    
    /** Aggregation: maximum */
    MAXIMUM,
    
    /** Aggregation: minimum */
    MINIMUM,
    
    /** Aggregation: average */
    AVERAGE,
    
    /** Aggregation: across */
    ACROSS,
    
    /** Aggregation: grouped */
    GROUPED,
    
    /** Filter: where */
    WHERE,
    
    /** Specification: matches */
    MATCHES,
    
    /** Specification: combines */
    COMBINES,
    
    /** Specification: satisfies */
    SATISFIES,
    
    /** Specification: eligible */
    ELIGIBLE,
    
    // ========== ARITHMETIC OPERATORS (NATURAL LANGUAGE) ==========
    
    /** Addition: plus */
    PLUS,
    
    /** Subtraction: minus */
    MINUS,
    
    /** Multiplication: times */
    TIMES,
    
    /** Division: divided */
    DIVIDED,
    
    // ========== TEMPORAL KEYWORDS ==========
    
    /** Temporal: now */
    NOW,
    
    /** Temporal: today */
    TODAY,
    
    /** Temporal: yesterday */
    YESTERDAY,
    
    /** Temporal: tomorrow */
    TOMORROW,
    
    /** Temporal: last */
    LAST,
    
    /** Temporal: next */
    NEXT,
    
    /** Temporal: ago */
    AGO,
    
    /** Temporal: before */
    BEFORE,
    
    /** Temporal: after */
    AFTER,
    
    /** Temporal: between */
    BETWEEN,
    
    /** Temporal: occurred */
    OCCURRED,
    
    /** Duration: second/seconds */
    SECONDS,
    
    /** Duration: minute/minutes */
    MINUTES,
    
    /** Duration: hour/hours */
    HOURS,
    
    /** Duration: days */
    DAYS,
    
    /** Duration: week/weeks */
    WEEKS,
    
    /** Duration: month/months */
    MONTHS,
    
    /** Duration: year/years */
    YEARS,
    
    /** Temporal: new */
    NEW,
    
    /** Temporal: generated */
    GENERATED,
    
    // ========== STATE MACHINE KEYWORDS ==========
    
    /** State machine: state */
    STATE,
    
    /** State machine: machine */
    MACHINE,
    
    /** State machine: states */
    STATES,
    
    /** State machine: transitions */
    TRANSITIONS,
    
    /** State machine: guards */
    GUARDS,
    
    /** State machine: entry */
    ENTRY,
    
    /** State machine: exit */
    EXIT,
    
    /** State machine: entering */
    ENTERING,
    
    /** State machine: leaving */
    LEAVING,
    
    /** State machine: initial */
    INITIAL,
    
    /** State machine: final */
    FINAL,
    
    /** Guard: cannot */
    CANNOT,
    
    /** Guard: must */
    MUST,
    
    /** Guard: transition */
    TRANSITION,
    
    /** Guard: only */
    ONLY,

    /** Transition condition: always */
    ALWAYS,

    /** Transition condition: never */
    NEVER,
    
    // ========== ERROR ACCUMULATION KEYWORDS ==========
    
    /** Error: collect */
    COLLECT,
    
    /** Error: errors */
    ERRORS,
    
    /** Error: warning */
    WARNING,
    
    /** Error: fail */
    FAIL,
    
    /** Error: critical */
    CRITICAL,
    
    /** Error: up */
    UP,
    
    /** Error: group */
    GROUP,
    
    // ========== MATCH EXPRESSION KEYWORDS ==========
    
    /** Match keyword */
    MATCH,
    
    // ========== STRING OPERATION KEYWORDS ==========
    
    /** String: contains */
    CONTAINS,
    
    /** String: starts */
    STARTS,
    
    /** String: ends */
    ENDS,
    
    /** String: blank */
    BLANK,
    
    /** String: format */
    FORMAT,
    
    /** Format: email */
    EMAIL,
    
    /** Format: phone */
    PHONE,
    
    /** Format: url */
    URL,
    
    /** Format: alphanumeric */
    ALPHANUMERIC,
    
    /** Format: numeric */
    NUMERIC,
    
    /** String: truncated */
    TRUNCATED,
    
    /** String: concatenated */
    CONCATENATED,
    
    /** String: replaced */
    REPLACED,
    
    /** String: length */
    LENGTH,
    
    /** String: characters */
    CHARACTERS,
    
    /** Quantifier: more */
    MORE,
    
    /** Quantifier: first */
    FIRST,
    
    /** Quantifier: exactly */
    EXACTLY,
    
    // ========== RESULT KEYWORDS ==========
    
    /** Success result */
    SUCCESS,
    
    /** Failure result */
    FAILURE,
    
    /** Result type */
    RESULT,
    
    // ========== TYPE KEYWORDS ==========
    
    /** Primitive: String */
    STRING_TYPE,
    
    /** Primitive: Int */
    INT_TYPE,
    
    /** Primitive: Decimal */
    DECIMAL_TYPE,
    
    /** Primitive: Boolean */
    BOOLEAN_TYPE,
    
    /** Primitive: DateTime */
    DATETIME_TYPE,
    
    /** Primitive: UUID */
    UUID_TYPE,
    
    /** Generic: List */
    LIST_TYPE,
    
    /** Generic: Set */
    SET_TYPE,
    
    /** Generic: Map */
    MAP_TYPE,
    
    /** Void type */
    VOID_TYPE,
    
    // ========== LITERALS ==========
    
    /** String literal: "..." */
    STRING_LITERAL,
    
    /** Integer literal: 123 */
    INTEGER_LITERAL,
    
    /** Decimal literal: 123.45 */
    DECIMAL_LITERAL,
    
    /** Boolean literal: true */
    TRUE,
    
    /** Boolean literal: false */
    FALSE,
    
    /** Null literal */
    NULL,
    
    // ========== IDENTIFIERS ==========
    
    /** User-defined identifier */
    IDENTIFIER,
    
    // ========== ANNOTATIONS ==========
    
    /** Annotation marker: @ */
    AT_SIGN,
    
    /** @identity annotation */
    IDENTITY,
    
    /** @required annotation */
    REQUIRED,
    
    /** @unique annotation */
    UNIQUE,
    
    /** @min annotation */
    MIN,
    
    /** @max annotation */
    MAX,
    
    /** @minLength annotation */
    MIN_LENGTH,
    
    /** @maxLength annotation */
    MAX_LENGTH,
    
    /** @precision annotation */
    PRECISION,
    
    /** @default annotation */
    DEFAULT,
    
    /** @computed annotation */
    COMPUTED,
    
    /** @pattern annotation */
    PATTERN,
    
    // ========== DELIMITERS ==========
    
    /** Left brace: { */
    LEFT_BRACE,
    
    /** Right brace: } */
    RIGHT_BRACE,
    
    /** Left parenthesis: ( */
    LEFT_PAREN,
    
    /** Right parenthesis: ) */
    RIGHT_PAREN,
    
    /** Left bracket: [ */
    LEFT_BRACKET,
    
    /** Right bracket: ] */
    RIGHT_BRACKET,
    
    /** Left angle bracket: < */
    LEFT_ANGLE,
    
    /** Right angle bracket: > */
    RIGHT_ANGLE,
    
    /** Colon: : */
    COLON,
    
    /** Semicolon: ; */
    SEMICOLON,
    
    /** Comma: , */
    COMMA,
    
    /** Dot: . */
    DOT,
    
    /** Dash/hyphen: - (list marker) */
    DASH,
    
    /** Question mark: ? (optional type) */
    QUESTION,
    
    /** Equals sign: = */
    ASSIGN,
    
    // ========== SYMBOLIC OPERATORS ==========
    
    /** Greater than: > */
    GT,
    
    /** Less than: < */
    LT,
    
    /** Greater than or equal: >= */
    GTE,
    
    /** Less than or equal: <= */
    LTE,
    
    /** Not equal: != */
    NEQ,
    
    /** Equality: == */
    EQ,
    
    /** Plus symbol: + */
    PLUS_SYMBOL,
    
    /** Minus symbol: - */
    MINUS_SYMBOL,
    
    /** Multiply symbol: * */
    STAR,
    
    /** Divide symbol: / */
    SLASH,
    
    // ========== SPECIAL TOKENS ==========
    
    /** End of file */
    EOF,
    
    /** Newline (significant for list items) */
    NEWLINE,
    
    /** Indent (for block structure) */
    INDENT,
    
    /** Dedent (for block structure) */
    DEDENT,
    
    /** Error token */
    ERROR;
    
    /**
     * Check if this token type is a keyword.
     */
    public boolean isKeyword() {
        return ordinal() >= BOUNDED_CONTEXT.ordinal() && ordinal() <= USE_CASE.ordinal()
            || ordinal() >= INVARIANTS.ordinal() && ordinal() <= FLOW.ordinal()
            || ordinal() >= WHEN.ordinal() && ordinal() <= EVENT.ordinal();
    }
    
    /**
     * Check if this token type is a literal.
     */
    public boolean isLiteral() {
        return this == STRING_LITERAL || this == INTEGER_LITERAL || this == DECIMAL_LITERAL
            || this == TRUE || this == FALSE || this == NULL;
    }
    
    /**
     * Check if this token type is an operator.
     */
    public boolean isOperator() {
        return ordinal() >= GT.ordinal() && ordinal() <= SLASH.ordinal();
    }
    
    /**
     * Check if this token type is a delimiter.
     */
    public boolean isDelimiter() {
        return ordinal() >= LEFT_BRACE.ordinal() && ordinal() <= ASSIGN.ordinal();
    }
    
    /**
     * Check if this token type is a comparison predicate.
     */
    public boolean isComparisonPredicate() {
        return ordinal() >= IS.ordinal() && ordinal() <= ONE.ordinal();
    }
    
    /**
     * Check if this token type is a collection predicate.
     */
    public boolean isCollectionPredicate() {
        return ordinal() >= ALL.ordinal() && ordinal() <= DETERMINED.ordinal();
    }
}
