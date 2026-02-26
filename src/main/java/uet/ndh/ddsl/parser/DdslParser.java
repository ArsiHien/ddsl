package uet.ndh.ddsl.parser;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.application.ApplicationServiceDecl;
import uet.ndh.ddsl.ast.application.UseCaseDecl;
import uet.ndh.ddsl.ast.application.UseCaseStepDecl;
import uet.ndh.ddsl.ast.behavior.BehaviorDecl;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;
import uet.ndh.ddsl.ast.behavior.NaturalLanguagePhrase;
import uet.ndh.ddsl.ast.behavior.clause.EmitClause;
import uet.ndh.ddsl.ast.behavior.clause.GivenClause;
import uet.ndh.ddsl.ast.behavior.clause.RequireClause;
import uet.ndh.ddsl.ast.behavior.clause.ReturnClause;
import uet.ndh.ddsl.ast.behavior.clause.ThenClause;
import uet.ndh.ddsl.ast.common.Constraint;
import uet.ndh.ddsl.ast.common.Constraint.ConstraintType;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.common.Visibility;
import uet.ndh.ddsl.ast.expr.BinaryExpr;
import uet.ndh.ddsl.ast.expr.CollectionAggregation;
import uet.ndh.ddsl.ast.expr.CollectionFilter;
import uet.ndh.ddsl.ast.expr.CollectionFlatten;
import uet.ndh.ddsl.ast.expr.CollectionGroupBy;
import uet.ndh.ddsl.ast.expr.Expr;
import uet.ndh.ddsl.ast.expr.FieldAccessExpr;
import uet.ndh.ddsl.ast.expr.ListExpr;
import uet.ndh.ddsl.ast.expr.LiteralExpr;
import uet.ndh.ddsl.ast.expr.MatchExpr;
import uet.ndh.ddsl.ast.expr.MethodCallExpr;
import uet.ndh.ddsl.ast.expr.NewInstanceExpr;
import uet.ndh.ddsl.ast.expr.NullExpr;
import uet.ndh.ddsl.ast.expr.SpecificationCondition;
import uet.ndh.ddsl.ast.expr.StringCondition;
import uet.ndh.ddsl.ast.expr.StringOperation;
import uet.ndh.ddsl.ast.expr.TemporalComparison;
import uet.ndh.ddsl.ast.expr.TemporalExpr;
import uet.ndh.ddsl.ast.expr.TemporalRange;
import uet.ndh.ddsl.ast.expr.TemporalRelative;
import uet.ndh.ddsl.ast.expr.TemporalSequence;
import uet.ndh.ddsl.ast.expr.UnaryExpr;
import uet.ndh.ddsl.ast.expr.VariableExpr;
import uet.ndh.ddsl.ast.expr.temporal.Duration;
import uet.ndh.ddsl.ast.expr.temporal.TemporalAnchor;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.member.InvariantDecl;
import uet.ndh.ddsl.ast.member.MethodDecl;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.model.BoundedContextDecl;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.ast.model.ModuleDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.entity.IdentityFieldDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.factory.FactoryCreationRuleDecl;
import uet.ndh.ddsl.ast.model.factory.FactoryDecl;
import uet.ndh.ddsl.ast.model.factory.FactoryMethodDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryMethodDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.model.specification.SpecificationDecl;
import uet.ndh.ddsl.ast.model.valueobject.OperationDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.parser.lexer.Scanner;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent parser for the DDSL language.
 * 
 * This parser reads a stream of tokens from the Scanner and produces
 * an Abstract Syntax Tree (AST) representing the domain model.
 */
public class DdslParser {
    
    /** The list of tokens to parse */
    private final List<Token> tokens;
    
    /** Current position in the token list */
    private int current = 0;
    
    /** List of parse errors */
    private final List<ParseError> errors = new ArrayList<>();
    
    /** Source file name for error reporting */
    private final String sourceName;
    
    /**
     * Creates a new parser for the given source code.
     * 
     * @param source     the source code to parse
     * @param sourceName the name of the source file
     */
    public DdslParser(String source, String sourceName) {
        Scanner scanner = new Scanner(source);
        this.tokens = scanner.scanTokens();
        this.sourceName = sourceName;
        
        // Check for scanner errors
        if (scanner.hasErrors()) {
            for (Scanner.LexicalError error : scanner.getErrors()) {
                errors.add(new ParseError(error.getMessage(), 
                    currentSpan()));
            }
        }
    }
    
    /**
     * Creates a parser from pre-scanned tokens.
     * 
     * @param tokens     the tokens to parse
     * @param sourceName the name of the source file
     */
    public DdslParser(List<Token> tokens, String sourceName) {
        this.tokens = tokens;
        this.sourceName = sourceName;
    }
    
    // ========== Public API ==========
    
    /**
     * Parses a file and returns the domain model.
     * 
     * @param filePath the path to the source file
     * @return the parsed domain model
     * @throws IOException if the file cannot be read
     * @throws ParseException if there are parse errors
     */
    public static DomainModel parseFile(String filePath) throws IOException, ParseException {
        Path path = Path.of(filePath);
        String source = Files.readString(path);
        String fileName = path.getFileName().toString();
        DdslParser parser = new DdslParser(source, fileName);
        return parser.parse();
    }
    
    /**
     * Parses the token stream and returns the domain model.
     * 
     * @return the parsed domain model
     * @throws ParseException if there are parse errors
     */
    public DomainModel parse() throws ParseException {
        try {
            DomainModel model = program();
            
            if (!errors.isEmpty()) {
                throw new ParseException("Parse failed with " + errors.size() + " error(s)", errors);
            }
            
            return model;
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            errors.add(new ParseError("Unexpected error: " + e.getMessage(), currentSpan()));
            throw new ParseException("Parse failed", errors);
        }
    }
    
    /**
     * Get the list of parse errors.
     */
    public List<ParseError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Check if there were parse errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    // ========== Grammar Rules ==========
    
    /**
     * Program ::= BoundedContextDeclaration*
     */
    private DomainModel program() {
        SourceSpan span = currentSpan();
        List<BoundedContextDecl> contexts = new ArrayList<>();
        
        // Parse bounded context(s)
        while (!isAtEnd()) {
            int before = current;
            BoundedContextDecl context = boundedContextDeclaration();
            if (context != null) {
                contexts.add(context);
            }
            // Safety: if no progress was made, skip the token to avoid infinite loop
            if (current == before) {
                advance();
            }
        }
        
        return new DomainModel(span, "DomainModel", contexts, null);
    }
    
    /**
     * BoundedContextDeclaration ::= 
     *     'BoundedContext' Identifier '{'
     *         UbiquitousLanguageSection?
     *         DomainSection
     *         EventsSection?
     *         FactoriesSection?
     *         RepositoriesSection?
     *         SpecificationsSection?
     *         UseCasesSection?
     *     '}'
     */
    private BoundedContextDecl boundedContextDeclaration() {
        SourceSpan span = currentSpan();
        
        if (!check(TokenType.BOUNDED_CONTEXT)) {
            error("Expected 'BoundedContext'");
            return null;
        }
        advance(); // consume 'BoundedContext'
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected bounded context name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after bounded context name");
        
        List<ModuleDecl> modules = new ArrayList<>();
        List<AggregateDecl> aggregates = new ArrayList<>();
        List<ValueObjectDecl> valueObjects = new ArrayList<>();
        List<DomainServiceDecl> domainServices = new ArrayList<>();
        List<DomainEventDecl> domainEvents = new ArrayList<>();
        List<RepositoryDecl> repositories = new ArrayList<>();
        List<FactoryDecl> factories = new ArrayList<>();
        List<SpecificationDecl> specifications = new ArrayList<>();
        List<ApplicationServiceDecl> applicationServices = new ArrayList<>();
        
        // Parse sections in any order
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.UBIQUITOUS_LANGUAGE)) {
                // Skip ubiquitous-language section for now
                advance();
                consume(TokenType.LEFT_BRACE, "Expected '{' after 'ubiquitous-language'");
                skipBlock();
            } else if (check(TokenType.DOMAIN)) {
                parseDomainSection(aggregates, valueObjects, domainServices);
            } else if (check(TokenType.EVENTS)) {
                parseEventsSection(domainEvents);
            } else if (check(TokenType.FACTORIES)) {
                parseFactoriesSection(factories);
            } else if (check(TokenType.REPOSITORIES)) {
                parseRepositoriesSection(repositories);
            } else if (check(TokenType.SPECIFICATIONS)) {
                parseSpecificationsSection(specifications);
            } else if (check(TokenType.USE_CASES)) {
                parseUseCasesSection(applicationServices);
            } else {
                error("Unexpected token in bounded context: " + peek().getLexeme());
                advance(); // skip the unexpected token
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of bounded context");
        
        return new BoundedContextDecl(
            span, name, modules, aggregates, valueObjects, domainServices,
            domainEvents, repositories, factories, specifications, applicationServices, null
        );
    }
    
    /**
     * DomainSection ::=
     *     'domain' '{'
     *         (AggregateDeclaration | EntityDeclaration | ValueObjectDeclaration | DomainServiceDeclaration)*
     *     '}'
     */
    private void parseDomainSection(List<AggregateDecl> aggregates, 
                                     List<ValueObjectDecl> valueObjects,
                                     List<DomainServiceDecl> domainServices) {
        advance(); // consume 'domain'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'domain'");
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.AGGREGATE)) {
                AggregateDecl aggregate = aggregateDeclaration();
                if (aggregate != null) {
                    aggregates.add(aggregate);
                }
            } else if (check(TokenType.ENTITY)) {
                // Standalone entities not directly added to context
                entityDeclaration();
            } else if (check(TokenType.VALUE_OBJECT)) {
                ValueObjectDecl vo = valueObjectDeclaration();
                if (vo != null) {
                    valueObjects.add(vo);
                }
            } else if (check(TokenType.DOMAIN_SERVICE)) {
                DomainServiceDecl service = domainServiceDeclaration();
                if (service != null) {
                    domainServices.add(service);
                }
            } else {
                error("Expected domain element (Aggregate, Entity, ValueObject, or DomainService)");
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of domain section");
    }
    
    /**
     * AggregateDeclaration ::=
     *     'Aggregate' Identifier '{'
     *         (FieldDeclaration)*
     *         InvariantsBlock?
     *         (BehaviorDeclaration)*
     *     '}'
     */
    private AggregateDecl aggregateDeclaration() {
        SourceSpan span = currentSpan();
        advance(); // consume 'Aggregate'
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected aggregate name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after aggregate name");
        
        // Collect fields, invariants, behaviors, child entities, value objects
        List<FieldDecl> rootFields = new ArrayList<>();
        List<InvariantDecl> invariants = new ArrayList<>();
        List<BehaviorDecl> behaviors = new ArrayList<>();
        List<EntityDecl> childEntities = new ArrayList<>();
        List<ValueObjectDecl> localValueObjects = new ArrayList<>();
        List<MethodDecl> commands = new ArrayList<>();
        IdentityFieldDecl identityField = null;
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.INVARIANTS)) {
                invariants.addAll(invariantsBlock());
            } else if (check(TokenType.OPERATIONS)) {
                // Parse operations block containing 'when' behaviors
                advance(); // consume 'operations'
                consume(TokenType.LEFT_BRACE, "Expected '{' after 'operations'");
                while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    if (check(TokenType.WHEN)) {
                        BehaviorDecl behavior = behaviorDeclaration();
                        if (behavior != null) behaviors.add(behavior);
                    } else {
                        error("Expected 'when' in operations block");
                        advance();
                    }
                }
                consume(TokenType.RIGHT_BRACE, "Expected '}' at end of operations block");
            } else if (check(TokenType.WHEN)) {
                BehaviorDecl behavior = behaviorDeclaration();
                behaviors.add(behavior);
            } else if (check(TokenType.ENTITY)) {
                EntityDecl entity = entityDeclaration();
                childEntities.add(entity);
            } else if (check(TokenType.VALUE_OBJECT)) {
                ValueObjectDecl vo = valueObjectDeclaration();
                localValueObjects.add(vo);
            } else if (check(TokenType.AT_SIGN)) {
                // Leading annotations (e.g. @identity) before a field
                List<Constraint> leadingConstraints = annotationList();
                FieldDecl field = fieldDeclaration();
                if (field != null) {
                    // Merge leading constraints with any trailing constraints
                    List<Constraint> allConstraints = new ArrayList<>(leadingConstraints);
                    allConstraints.addAll(field.constraints());
                    field = new FieldDecl(field.span(), field.name(), field.type(),
                            field.visibility(), field.isFinal(), field.isNullable(),
                            field.defaultValue(), allConstraints, field.documentation());
                    rootFields.add(field);
                    if (hasIdentityConstraint(field)) {
                        identityField = toIdentityFieldDecl(field);
                    }
                }
            } else if (check(TokenType.IDENTIFIER)) {
                FieldDecl field = fieldDeclaration();
                if (field != null) {
                    rootFields.add(field);
                    // Check if this is an identity field
                    if (hasIdentityConstraint(field)) {
                        identityField = toIdentityFieldDecl(field);
                    }
                }
            } else {
                error("Unexpected token in aggregate: " + peek().getLexeme());
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of aggregate");
        
        // Create root entity for the aggregate
        EntityDecl rootEntity = new EntityDecl(
            span, name, identityField, rootFields, List.of(), List.of(),
            behaviors, invariants, List.of(), true, null
        );
        
        return new AggregateDecl(
            span, name, rootEntity, childEntities, localValueObjects,
            invariants, behaviors, commands, List.of(), List.of(), null
        );
    }
    
    /**
     * EntityDeclaration ::=
     *     'Entity' Identifier '{'
     *         (FieldDeclaration)*
     *         InvariantsBlock?
     *         (BehaviorDeclaration)*
     *     '}'
     */
    private EntityDecl entityDeclaration() {
        SourceSpan span = currentSpan();
        advance(); // consume 'Entity'
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected entity name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after entity name");
        
        List<FieldDecl> fields = new ArrayList<>();
        List<InvariantDecl> invariants = new ArrayList<>();
        List<BehaviorDecl> behaviors = new ArrayList<>();
        IdentityFieldDecl identityField = null;
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.INVARIANTS)) {
                invariants.addAll(invariantsBlock());
            } else if (check(TokenType.OPERATIONS)) {
                // Parse operations block containing 'when' behaviors
                advance(); // consume 'operations'
                consume(TokenType.LEFT_BRACE, "Expected '{' after 'operations'");
                while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    if (check(TokenType.WHEN)) {
                        BehaviorDecl behavior = behaviorDeclaration();
                        if (behavior != null) behaviors.add(behavior);
                    } else {
                        error("Expected 'when' in operations block");
                        advance();
                    }
                }
                consume(TokenType.RIGHT_BRACE, "Expected '}' at end of operations block");
            } else if (check(TokenType.WHEN)) {
                BehaviorDecl behavior = behaviorDeclaration();
                if (behavior != null) {
                    behaviors.add(behavior);
                }
            } else if (check(TokenType.AT_SIGN)) {
                // Leading annotations (e.g. @identity) before a field
                List<Constraint> leadingConstraints = annotationList();
                FieldDecl field = fieldDeclaration();
                if (field != null) {
                    List<Constraint> allConstraints = new ArrayList<>(leadingConstraints);
                    allConstraints.addAll(field.constraints());
                    field = new FieldDecl(field.span(), field.name(), field.type(),
                            field.visibility(), field.isFinal(), field.isNullable(),
                            field.defaultValue(), allConstraints, field.documentation());
                    fields.add(field);
                    if (hasIdentityConstraint(field)) {
                        identityField = toIdentityFieldDecl(field);
                    }
                }
            } else if (check(TokenType.IDENTIFIER)) {
                FieldDecl field = fieldDeclaration();
                if (field != null) {
                    fields.add(field);
                    if (hasIdentityConstraint(field)) {
                        identityField = toIdentityFieldDecl(field);
                    }
                }
            } else {
                error("Unexpected token in entity: " + peek().getLexeme());
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of entity");
        
        return new EntityDecl(
            span, name, identityField, fields, List.of(), List.of(),
            behaviors, invariants, List.of(), false, null
        );
    }
    
    /**
     * ValueObjectDeclaration ::=
     *     'ValueObject' Identifier '{'
     *         (FieldDeclaration)*
     *         InvariantsBlock?
     *         OperationsBlock?
     *     '}'
     */
    private ValueObjectDecl valueObjectDeclaration() {
        SourceSpan span = currentSpan();
        advance(); // consume 'ValueObject'
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected value object name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after value object name");
        
        List<FieldDecl> fields = new ArrayList<>();
        List<InvariantDecl> invariants = new ArrayList<>();
        List<OperationDecl> operations = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.INVARIANTS)) {
                invariants.addAll(invariantsBlock());
            } else if (check(TokenType.OPERATIONS)) {
                operations.addAll(operationsBlock());
            } else if (check(TokenType.IDENTIFIER)) {
                FieldDecl field = fieldDeclaration();
                if (field != null) {
                    fields.add(field);
                }
            } else {
                error("Unexpected token in value object: " + peek().getLexeme());
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of value object");
        
        return new ValueObjectDecl(span, name, fields, List.of(), operations, invariants, List.of(), null);
    }
    
    /**
     * DomainServiceDeclaration ::=
     *     'DomainService' Identifier '{'
     *         (ServiceOperationDeclaration)*
     *     '}'
     */
    private DomainServiceDecl domainServiceDeclaration() {
        SourceSpan span = currentSpan();
        advance(); // consume 'DomainService'
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected domain service name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after domain service name");
        
        List<MethodDecl> methods = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.WHEN)) {
                BehaviorDecl behavior = behaviorDeclaration();
                if (behavior != null) {
                    // Convert behavior to method
                    methods.add(behaviorToMethod(behavior));
                }
            } else {
                error("Expected 'when' for service operation");
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of domain service");
        
        return new DomainServiceDecl(span, name, methods, List.of(), null);
    }
    
    /**
     * FieldDeclaration ::=
     *     Identifier ':' TypeReference AnnotationList?
     */
    private FieldDecl fieldDeclaration() {
        SourceSpan span = currentSpan();
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected field name");
        if (nameToken == null) return null;
        
        consume(TokenType.COLON, "Expected ':' after field name");
        
        TypeRef type = typeReference();
        List<Constraint> constraints = annotationList();
        
        return new FieldDecl(
            span, nameToken.getLexeme(), type, Visibility.PRIVATE,
            false, type.isNullable(), null, constraints, null
        );
    }
    
    /**
     * TypeReference ::= 
     *     PrimitiveType
     *     | Identifier  // Domain type
     *     | GenericType
     *     | Type '?'    // Optional type
     */
    private TypeRef typeReference() {
        SourceSpan span = currentSpan();
        String typeName;
        
        if (checkType()) {
            Token typeToken = advance();
            typeName = typeToken.getLexeme();
        } else if (check(TokenType.IDENTIFIER)) {
            Token typeToken = advance();
            typeName = typeToken.getLexeme();
        } else {
            error("Expected type");
            return TypeRef.simple(currentSpan(), "Unknown");
        }
        
        // Handle generic types: List<T>, Set<T>, Map<K,V>
        List<TypeRef> typeArgs = new ArrayList<>();
        TypeRef.CollectionKind collectionKind = null;
        
        if (check(TokenType.LEFT_ANGLE)) {
            advance(); // consume '<'
            
            // Determine collection kind
            collectionKind = switch (typeName.toLowerCase()) {
                case "list" -> TypeRef.CollectionKind.LIST;
                case "set" -> TypeRef.CollectionKind.SET;
                case "map" -> TypeRef.CollectionKind.MAP;
                default -> null;
            };
            
            do {
                if (check(TokenType.COMMA)) advance();
                typeArgs.add(typeReference());
            } while (check(TokenType.COMMA));
            
            consume(TokenType.RIGHT_ANGLE, "Expected '>' after type arguments");
        }
        
        // Handle optional types: Type?
        boolean isNullable = false;
        if (check(TokenType.QUESTION)) {
            advance();
            isNullable = true;
        }
        
        return new TypeRef(span, typeName, typeArgs, isNullable, collectionKind != null, collectionKind);
    }
    
    /**
     * Check if current token is a type keyword.
     */
    private boolean checkType() {
        return check(TokenType.STRING_TYPE) ||
               check(TokenType.INT_TYPE) ||
               check(TokenType.DECIMAL_TYPE) ||
               check(TokenType.BOOLEAN_TYPE) ||
               check(TokenType.DATETIME_TYPE) ||
               check(TokenType.UUID_TYPE) ||
               check(TokenType.LIST_TYPE) ||
               check(TokenType.SET_TYPE) ||
               check(TokenType.MAP_TYPE) ||
               check(TokenType.VOID_TYPE) ||
               check(TokenType.RESULT);
    }
    
    /**
     * AnnotationList ::= Annotation*
     */
    private List<Constraint> annotationList() {
        List<Constraint> constraints = new ArrayList<>();
        
        while (check(TokenType.AT_SIGN)) {
            Constraint constraint = annotation();
            if (constraint != null) {
                constraints.add(constraint);
            }
        }
        
        return constraints;
    }
    
    /**
     * Annotation ::= '@' Identifier ('(' AnnotationArguments ')')?
     */
    private Constraint annotation() {
        SourceSpan span = currentSpan();
        advance(); // consume '@'
        
        if (!check(TokenType.IDENTIFIER) && !checkAnnotationType()) {
            error("Expected annotation name after '@'");
            return null;
        }
        
        Token nameToken = advance();
        String name = nameToken.getLexeme();
        Expr valueExpr = null;
        String message = null;
        
        // Parse annotation arguments if present
        if (check(TokenType.LEFT_PAREN)) {
            advance(); // consume '('
            
            // Parse the argument value
            if (check(TokenType.INTEGER_LITERAL)) {
                Token valToken = advance();
                valueExpr = new LiteralExpr(span, valToken.getLiteral(), LiteralExpr.LiteralType.INTEGER);
            } else if (check(TokenType.DECIMAL_LITERAL)) {
                Token valToken = advance();
                valueExpr = new LiteralExpr(span, valToken.getLiteral(), LiteralExpr.LiteralType.DECIMAL);
            } else if (check(TokenType.STRING_LITERAL)) {
                Token valToken = advance();
                valueExpr = new LiteralExpr(span, valToken.getStringValue(), LiteralExpr.LiteralType.STRING);
            } else if (check(TokenType.TRUE) || check(TokenType.FALSE)) {
                Token valToken = advance();
                valueExpr = new LiteralExpr(span, valToken.getBooleanValue(), LiteralExpr.LiteralType.BOOLEAN);
            }
            
            // Handle precision(p, s)
            if (check(TokenType.COMMA)) {
                advance();
                // Skip second argument for now
                if (check(TokenType.INTEGER_LITERAL)) {
                    advance();
                }
            }
            
            consume(TokenType.RIGHT_PAREN, "Expected ')' after annotation arguments");
        }
        
        // Map annotation name to constraint type
        ConstraintType type = mapAnnotationToConstraintType(name);
        
        return new Constraint(span, type, valueExpr, message);
    }
    
    /**
     * Check if current token is an annotation type keyword.
     */
    private boolean checkAnnotationType() {
        return check(TokenType.IDENTITY) ||
               check(TokenType.REQUIRED) ||
               check(TokenType.UNIQUE) ||
               check(TokenType.MIN) ||
               check(TokenType.MAX) ||
               check(TokenType.MIN_LENGTH) ||
               check(TokenType.MAX_LENGTH) ||
               check(TokenType.PRECISION) ||
               check(TokenType.DEFAULT) ||
               check(TokenType.COMPUTED) ||
               check(TokenType.PATTERN);
    }
    
    /**
     * Map annotation name to constraint type.
     */
    private ConstraintType mapAnnotationToConstraintType(String name) {
        return switch (name.toLowerCase()) {
            case "identity" -> ConstraintType.IDENTITY;
            case "required" -> ConstraintType.REQUIRED;
            case "unique" -> ConstraintType.UNIQUE;
            case "min" -> ConstraintType.MIN;
            case "max" -> ConstraintType.MAX;
            case "minlength" -> ConstraintType.MIN_LENGTH;
            case "maxlength" -> ConstraintType.MAX_LENGTH;
            case "precision" -> ConstraintType.PRECISION;
            case "default" -> ConstraintType.DEFAULT;
            case "computed" -> ConstraintType.COMPUTED;
            case "pattern" -> ConstraintType.PATTERN;
            default -> ConstraintType.CUSTOM;
        };
    }
    
    /**
     * Check if a field has an @identity constraint.
     */
    private boolean hasIdentityConstraint(FieldDecl field) {
        return field.constraints().stream()
            .anyMatch(c -> c.type() == ConstraintType.IDENTITY);
    }
    
    /**
     * Convert a FieldDecl with @identity constraint to IdentityFieldDecl.
     */
    private IdentityFieldDecl toIdentityFieldDecl(FieldDecl field) {
        return new IdentityFieldDecl(
            field.span(), 
            field.name(), 
            field.type(),
            IdentityFieldDecl.IdentityType.UUID, 
            IdentityFieldDecl.IdGenerationStrategy.UUID
        );
    }
    
    /**
     * Convert a BehaviorDecl to a MethodDecl.
     */
    private MethodDecl behaviorToMethod(BehaviorDecl behavior) {
        String methodName = behavior.phrase() != null ? behavior.phrase().rawText() : "unknownMethod";
        // Convert "changing name" to "changeName"
        if (methodName.contains(" ")) {
            String[] words = methodName.split("\\s+");
            StringBuilder sb = new StringBuilder(words[0]);
            for (int i = 1; i < words.length; i++) {
                if (!words[i].isEmpty()) {
                    sb.append(Character.toUpperCase(words[i].charAt(0)));
                    if (words[i].length() > 1) {
                        sb.append(words[i].substring(1));
                    }
                }
            }
            methodName = sb.toString();
        }
        
        return new MethodDecl(
            behavior.span(),
            methodName,
            null,  // returnType
            behavior.parameters(),
            null,  // body
            Visibility.PUBLIC,
            false, // isStatic
            false, // isAbstract
            MethodDecl.MethodKind.COMMAND,  // kind
            null   // documentation
        );
    }
    
    /**
     * InvariantsBlock ::=
     *     'invariants' '{'
     *         (InvariantRule)*
     *     '}'
     */
    private List<InvariantDecl> invariantsBlock() {
        List<InvariantDecl> invariants = new ArrayList<>();
        
        advance(); // consume 'invariants'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'invariants'");
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            int before = current;
            InvariantDecl invariant = invariantRule();
            if (invariant != null) {
                invariants.add(invariant);
            }
            if (current == before) {
                advance(); // skip to avoid infinite loop
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of invariants block");
        
        return invariants;
    }
    
    /**
     * InvariantRule ::= StringLiteral ':' Expression
     */
    private InvariantDecl invariantRule() {
        SourceSpan span = currentSpan();
        
        Token messageToken = consume(TokenType.STRING_LITERAL, "Expected invariant message");
        if (messageToken == null) return null;
        
        String message = messageToken.getStringValue();
        String name = toInvariantName(message);
        
        consume(TokenType.COLON, "Expected ':' after invariant message");
        
        Expr condition = expression();
        
        return new InvariantDecl(span, name, message, condition, null);
    }
    
    /**
     * Convert an invariant message to a method-safe name.
     */
    private String toInvariantName(String message) {
        // Remove non-alphanumeric, capitalize words
        String[] words = message.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * OperationsBlock ::=
     *     'operations' '{'
     *         (OperationDeclaration)*
     *     '}'
     */
    private List<OperationDecl> operationsBlock() {
        List<OperationDecl> operations = new ArrayList<>();
        
        advance(); // consume 'operations'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'operations'");
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            OperationDecl operation = operationDeclaration();
            if (operation != null) {
                operations.add(operation);
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of operations block");
        
        return operations;
    }
    
    /**
     * OperationDeclaration ::=
     *     Identifier '(' ParameterList? ')' ':' TypeReference '{'
     *         ('require' Condition ':' StringLiteral)*
     *         'return' Expression
     *     '}'
     */
    private OperationDecl operationDeclaration() {
        SourceSpan span = currentSpan();
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected operation name");
        if (nameToken == null) return null;
        
        consume(TokenType.LEFT_PAREN, "Expected '(' after operation name");
        
        List<ParameterDecl> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            parameters = parameterList();
        }
        
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
        consume(TokenType.COLON, "Expected ':' after ')'");
        
        TypeRef returnType = typeReference();
        
        consume(TokenType.LEFT_BRACE, "Expected '{' for operation body");
        
        Expr returnExpr = null;
        
        // Parse require conditions and return expression
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.REQUIRE)) {
                requireClause(); // parse and skip for now
            } else if (check(TokenType.RETURN)) {
                advance(); // consume 'return'
                returnExpr = expression();
            } else {
                error("Expected 'require' or 'return' in operation body");
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of operation");
        
        return new OperationDecl(span, nameToken.getLexeme(), returnType, parameters, returnExpr, null);
    }
    
    // ========== Behavior Parsing ==========
    
    private BehaviorDecl behaviorDeclaration() {
        SourceSpan span = currentSpan();
        
        advance(); // consume 'when'
        
        // Parse natural language phrase
        NaturalLanguagePhrase phrase = parseNaturalLanguagePhrase();
        
        // Parse optional parameters: with parameterList
        List<ParameterDecl> parameters = new ArrayList<>();
        if (check(TokenType.WITH)) {
            advance();
            parameters = parameterList();
        }
        
        consume(TokenType.COLON, "Expected ':' after behavior header");
        
        // Parse clauses
        RequireClause requireClauseVal = null;
        GivenClause givenClauseVal = null;
        List<ThenClause> thenClauses = new ArrayList<>();
        EmitClause emitClauseVal = null;
        ReturnClause returnClauseVal = null;
        
        while (!check(TokenType.WHEN) && !check(TokenType.RIGHT_BRACE) && 
               !check(TokenType.INVARIANTS) && !isAtEnd()) {
            if (check(TokenType.REQUIRE)) {
                requireClauseVal = requireClause();
            } else if (check(TokenType.GIVEN)) {
                givenClauseVal = givenClause();
            } else if (check(TokenType.THEN)) {
                thenClauses.add(thenClause());
            } else if (check(TokenType.AND) && checkNext(TokenType.EMIT)) {
                advance(); // consume 'and'
                emitClauseVal = emitClause();
            } else if (check(TokenType.EMIT)) {
                emitClauseVal = emitClause();
            } else if (check(TokenType.RETURN)) {
                returnClauseVal = returnClause();
                break;
            } else {
                break;
            }
        }
        
        return new BehaviorDecl(
            span, phrase, parameters, requireClauseVal, givenClauseVal,
            thenClauses, emitClauseVal, returnClauseVal, null
        );
    }
    
    private void parseEventsSection(List<DomainEventDecl> domainEvents) {
        advance(); // consume 'events'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'events'");
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            DomainEventDecl event = domainEventDeclaration();
            if (event != null) {
                domainEvents.add(event);
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of events section");
    }
    
    private DomainEventDecl domainEventDeclaration() {
        SourceSpan span = currentSpan();
        
        if (!check(TokenType.DOMAIN_EVENT)) {
            error("Expected 'DomainEvent'");
            return null;
        }
        advance();
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected event name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after event name");
        
        List<FieldDecl> fields = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.METADATA)) {
                // Skip metadata block for now
                advance();
                consume(TokenType.LEFT_BRACE, "Expected '{' after 'metadata'");
                skipBlock();
            } else if (check(TokenType.IDENTIFIER)) {
                FieldDecl field = fieldDeclaration();
                if (field != null) {
                    fields.add(field);
                }
            } else {
                error("Unexpected token in event: " + peek().getLexeme());
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of event");
        
        return new DomainEventDecl(span, name, fields, null, null);
    }
    
    private void parseFactoriesSection(List<FactoryDecl> factories) {
        advance(); // consume 'factories'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'factories'");
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            FactoryDecl factory = factoryDeclaration();
            if (factory != null) {
                factories.add(factory);
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of factories section");
    }
    
    private FactoryDecl factoryDeclaration() {
        SourceSpan span = currentSpan();
        
        if (!check(TokenType.FACTORY)) {
            error("Expected 'Factory'");
            return null;
        }
        advance();
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected factory name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after factory name");
        
        // Determine created type from factory name (e.g., OrderFactory -> Order)
        String createdTypeName = name.replace("Factory", "");
        TypeRef producedType = new TypeRef(span, createdTypeName, null, false, false, null);
        
        List<FactoryMethodDecl> creationMethods = new ArrayList<>();
        List<FactoryCreationRuleDecl> creationRules = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.WHEN)) {
                FactoryCreationRuleDecl rule = factoryCreationRule();
                if (rule != null) {
                    creationRules.add(rule);
                }
            } else {
                error("Expected 'when' for factory creation rule");
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of factory");
        
        return new FactoryDecl(span, name, producedType, creationMethods, creationRules, null);
    }
    
    private FactoryCreationRuleDecl factoryCreationRule() {
        SourceSpan span = currentSpan();
        
        advance(); // consume 'when'
        consume(TokenType.CREATING, "Expected 'creating' after 'when' in factory");
        
        // Parse: creating EntityType from SourceType with params
        TypeRef entityType = typeReference();
        
        consume(TokenType.FROM, "Expected 'from' after entity type");
        
        TypeRef sourceType = typeReference();
        
        // Parse optional parameters
        List<ParameterDecl> params = new ArrayList<>();
        if (check(TokenType.WITH)) {
            advance();
            params = parameterList();
        }
        
        consume(TokenType.COLON, "Expected ':' after factory rule header");
        
        // Parse clauses - skip for now, just build the rule
        StringBuilder description = new StringBuilder();
        description.append("creating ").append(entityType.name()).append(" from ").append(sourceType.name());
        
        while (!check(TokenType.WHEN) && !check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.REQUIRE) || check(TokenType.GIVEN) || check(TokenType.THEN) || check(TokenType.RETURN)) {
                // Skip clause parsing for now
                advance();
                skipUntilClauseOrEnd();
            } else {
                break;
            }
        }
        
        return new FactoryCreationRuleDecl(span, "create", null, description.toString());
    }
    
    private void parseRepositoriesSection(List<RepositoryDecl> repositories) {
        advance(); // consume 'repositories'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'repositories'");
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            RepositoryDecl repo = repositoryDeclaration();
            if (repo != null) {
                repositories.add(repo);
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of repositories section");
    }
    
    private RepositoryDecl repositoryDeclaration() {
        SourceSpan span = currentSpan();
        
        if (!check(TokenType.REPOSITORY)) {
            error("Expected 'Repository'");
            return null;
        }
        advance();
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected repository name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.FOR, "Expected 'for' after repository name");
        
        TypeRef aggregateType = typeReference();
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after aggregate type");
        
        List<RepositoryMethodDecl> methods = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            int before = current;
            RepositoryMethodDecl method = repositoryMethod();
            if (method != null) {
                methods.add(method);
            } else if (current == before) {
                // No progress — skip token to avoid infinite loop
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of repository");
        
        return new RepositoryDecl(span, name, aggregateType, methods, null);
    }
    
    private RepositoryMethodDecl repositoryMethod() {
        SourceSpan span = currentSpan();
        
        // Method name can be an identifier or a keyword used as method name (save, remove, add, count, etc.)
        Token nameToken;
        if (check(TokenType.IDENTIFIER) || check(TokenType.SAVE) || check(TokenType.REMOVE) 
                || check(TokenType.ADD) || check(TokenType.COUNT)) {
            nameToken = advance();
        } else {
            nameToken = consume(TokenType.IDENTIFIER, "Expected method name");
            if (nameToken == null) return null;
        }
        
        String methodName = nameToken.getLexeme();
        
        consume(TokenType.LEFT_PAREN, "Expected '(' after method name");
        
        List<ParameterDecl> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            parameters = parameterList();
        }
        
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
        consume(TokenType.COLON, "Expected ':' after ')'");
        
        TypeRef returnType = typeReference();
        
        // Determine method type from name
        RepositoryMethodDecl.RepositoryMethodType methodType = determineRepositoryMethodType(methodName);
        
        return new RepositoryMethodDecl(span, methodName, returnType, parameters, methodType, null);
    }
    
    private RepositoryMethodDecl.RepositoryMethodType determineRepositoryMethodType(String name) {
        String lowerName = name.toLowerCase();
        if (lowerName.equals("findbyid") || lowerName.equals("getbyid")) {
            return RepositoryMethodDecl.RepositoryMethodType.FIND_BY_ID;
        } else if (lowerName.equals("findall") || lowerName.equals("getall")) {
            return RepositoryMethodDecl.RepositoryMethodType.FIND_ALL;
        } else if (lowerName.startsWith("findby") || lowerName.startsWith("getby")) {
            return RepositoryMethodDecl.RepositoryMethodType.FIND_BY;
        } else if (lowerName.equals("save") || lowerName.equals("add") || lowerName.equals("insert")) {
            return RepositoryMethodDecl.RepositoryMethodType.SAVE;
        } else if (lowerName.equals("delete") || lowerName.equals("remove")) {
            return RepositoryMethodDecl.RepositoryMethodType.DELETE;
        } else if (lowerName.startsWith("exists")) {
            return RepositoryMethodDecl.RepositoryMethodType.EXISTS;
        } else if (lowerName.equals("count")) {
            return RepositoryMethodDecl.RepositoryMethodType.COUNT;
        } else {
            return RepositoryMethodDecl.RepositoryMethodType.CUSTOM;
        }
    }
    
    private void parseSpecificationsSection(List<SpecificationDecl> specifications) {
        advance(); // consume 'specifications'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'specifications'");
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            SpecificationDecl spec = specificationDeclaration();
            if (spec != null) {
                specifications.add(spec);
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of specifications section");
    }
    
    private SpecificationDecl specificationDeclaration() {
        SourceSpan span = currentSpan();
        
        if (!check(TokenType.SPECIFICATION)) {
            error("Expected 'Specification'");
            return null;
        }
        advance();
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected specification name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        // Parse optional parameters: given parameterList
        List<ParameterDecl> parameters = new ArrayList<>();
        if (check(TokenType.GIVEN)) {
            advance();
            parameters = parameterList();
        }
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after specification name");
        
        TypeRef targetType = null;
        Expr predicate = null;
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.MATCHES)) {
                // Parse: matches EntityType where: conditions
                advance();
                targetType = typeReference();
                
                consume(TokenType.WHERE, "Expected 'where' after entity type");
                consume(TokenType.COLON, "Expected ':' after 'where'");
                
                // Parse match conditions as a predicate
                List<Expr> conditions = new ArrayList<>();
                while (check(TokenType.DASH)) {
                    advance();
                    Expr condition = expression();
                    conditions.add(condition);
                }
                // Combine conditions with AND
                if (!conditions.isEmpty()) {
                    predicate = conditions.get(0);
                    for (int i = 1; i < conditions.size(); i++) {
                        predicate = new BinaryExpr(span, predicate, BinaryExpr.BinaryOperator.AND, conditions.get(i));
                    }
                }
            } else if (check(TokenType.AND)) {
                // Parse: and combines: SpecificationList - skip for now
                advance();
                consume(TokenType.COMBINES, "Expected 'combines' after 'and'");
                consume(TokenType.COLON, "Expected ':' after 'combines'");
                
                while (check(TokenType.IDENTIFIER)) {
                    advance(); // skip specification name
                    if (check(TokenType.OR) || check(TokenType.AND)) {
                        advance();
                    }
                }
            } else {
                error("Expected 'matches' or 'and combines' in specification");
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of specification");
        
        return new SpecificationDecl(span, name, targetType, predicate, parameters, null);
    }
    
    private void parseUseCasesSection(List<ApplicationServiceDecl> services) {
        advance(); // consume 'use-cases'
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'use-cases'");
        
        SourceSpan serviceSpan = currentSpan();
        List<UseCaseDecl> useCases = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            UseCaseDecl useCase = useCaseDeclaration();
            if (useCase != null) {
                useCases.add(useCase);
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of use-cases section");
        
        if (!useCases.isEmpty()) {
            // Create default application service to hold use cases
            ApplicationServiceDecl appService = new ApplicationServiceDecl(
                serviceSpan, "ApplicationService", useCases, List.of(), null);
            services.add(appService);
        }
    }
    
    private UseCaseDecl useCaseDeclaration() {
        SourceSpan span = currentSpan();
        
        if (!check(TokenType.USE_CASE)) {
            error("Expected 'UseCase'");
            return null;
        }
        advance();
        
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected use case name");
        String name = nameToken != null ? nameToken.getLexeme() : "Unknown";
        
        consume(TokenType.LEFT_BRACE, "Expected '{' after use case name");
        
        List<ParameterDecl> inputs = new ArrayList<>();
        TypeRef returnType = null;
        List<BehaviorDecl> behaviors = new ArrayList<>();
        List<UseCaseStepDecl> steps = new ArrayList<>();
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.INPUT)) {
                advance();
                consume(TokenType.COLON, "Expected ':' after 'input'");
                inputs = parseUseCaseInputs();
            } else if (check(TokenType.OUTPUT)) {
                advance();
                consume(TokenType.COLON, "Expected ':' after 'output'");
                returnType = typeReference();
            } else if (check(TokenType.FLOW)) {
                advance();
                consume(TokenType.COLON, "Expected ':' after 'flow'");
                steps = parseUseCaseFlowSteps();
            } else {
                error("Expected 'input', 'output', or 'flow' in use case");
                advance();
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "Expected '}' at end of use case");
        
        return new UseCaseDecl(span, name, inputs, returnType, null, behaviors, steps, null);
    }
    
    private List<ParameterDecl> parseUseCaseInputs() {
        List<ParameterDecl> inputs = new ArrayList<>();
        
        // Input can be a type reference or an inline definition
        if (check(TokenType.IDENTIFIER) && !checkNext(TokenType.LEFT_BRACE)) {
            TypeRef inputType = typeReference();
            // Create a single parameter from the type
            inputs.add(new ParameterDecl(inputType.span(), "input", inputType, false, null));
        } else {
            // Inline input definition: TypeName { fields }
            Token nameToken = consume(TokenType.IDENTIFIER, "Expected input type name");
            
            if (check(TokenType.LEFT_BRACE)) {
                advance();
                while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    FieldDecl field = fieldDeclaration();
                    if (field != null) {
                        inputs.add(new ParameterDecl(field.span(), field.name(), field.type(), false, null));
                    }
                }
                consume(TokenType.RIGHT_BRACE, "Expected '}' after input definition");
            }
        }
        
        return inputs;
    }
    
    private List<UseCaseStepDecl> parseUseCaseFlowSteps() {
        List<UseCaseStepDecl> steps = new ArrayList<>();
        int stepOrder = 1;
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.REQUIRE) || check(TokenType.GIVEN) || check(TokenType.THEN) || check(TokenType.RETURN)) {
                SourceSpan stepSpan = currentSpan();
                String stepType = peek().getLexeme();
                advance();
                
                // Determine step type
                UseCaseStepDecl.StepType type = switch (stepType.toLowerCase()) {
                    case "require" -> UseCaseStepDecl.StepType.VALIDATE;
                    case "given" -> UseCaseStepDecl.StepType.LOAD;
                    case "then" -> UseCaseStepDecl.StepType.EXECUTE;
                    case "return" -> UseCaseStepDecl.StepType.RETURN;
                    default -> UseCaseStepDecl.StepType.EXECUTE;
                };
                
                // Skip step details for now
                skipUntilClauseOrEnd();
                
                steps.add(new UseCaseStepDecl(stepSpan, stepOrder++, stepType, stepType, type));
            } else {
                break;
            }
        }
        
        return steps;
    }
    
    // ========== Behavior Parsing ==========
    
    private BehaviorDecl parseBehavior() {
        SourceSpan span = currentSpan();
        
        advance(); // consume 'when'
        
        // Parse natural language phrase
        NaturalLanguagePhrase phrase = parseNaturalLanguagePhrase();
        
        // Parse optional parameters: with parameterList
        List<ParameterDecl> params = new ArrayList<>();
        if (check(TokenType.WITH)) {
            advance();
            params = parameterList();
        }
        
        consume(TokenType.COLON, "Expected ':' after behavior header");
        
        // Parse clauses
        RequireClause requireClause = null;
        GivenClause givenClause = null;
        List<ThenClause> thenClauses = new ArrayList<>();
        EmitClause emitClause = null;
        
        while (!check(TokenType.WHEN) && !check(TokenType.RIGHT_BRACE) && 
               !check(TokenType.INVARIANTS) && !isAtEnd()) {
            if (check(TokenType.REQUIRE)) {
                requireClause = requireClause();
            } else if (check(TokenType.GIVEN)) {
                givenClause = givenClause();
            } else if (check(TokenType.THEN)) {
                ThenClause then = thenClause();
                if (then != null) {
                    thenClauses.add(then);
                }
            } else if (check(TokenType.AND) && checkNext(TokenType.EMIT)) {
                advance(); // consume 'and'
                emitClause = emitClause();
            } else if (check(TokenType.EMIT)) {
                emitClause = emitClause();
            } else {
                break;
            }
        }
        
        return new BehaviorDecl(span, phrase, params, requireClause, givenClause, thenClauses, emitClause, null);
    }
    
    private OperationDecl parseServiceOperation() {
        SourceSpan span = currentSpan();
        
        advance(); // consume 'when'
        
        NaturalLanguagePhrase phrase = parseNaturalLanguagePhrase();
        
        List<ParameterDecl> params = new ArrayList<>();
        if (check(TokenType.WITH)) {
            advance();
            params = parameterList();
        }
        
        consume(TokenType.COLON, "Expected ':' after service operation header");
        
        RequireClause requireClause = null;
        GivenClause givenClause = null;
        List<ThenClause> thenClauses = new ArrayList<>();
        TypeRef returnType = null;
        
        while (!check(TokenType.WHEN) && !check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(TokenType.REQUIRE)) {
                requireClause = requireClause();
            } else if (check(TokenType.GIVEN)) {
                givenClause = givenClause();
            } else if (check(TokenType.THEN)) {
                ThenClause then = thenClause();
                if (then != null) {
                    thenClauses.add(then);
                }
            } else if (check(TokenType.RETURN)) {
                advance();
                // Skip return expression for now
                skipUntilClauseOrEnd();
                break;
            } else {
                break;
            }
        }
        
        // Convert phrase to method name
        String methodName = phrase.toMethodName();
        
        return new OperationDecl(span, methodName, returnType, params, null, null);
    }
    
    private NaturalLanguagePhrase parseNaturalLanguagePhrase() {
        SourceSpan span = currentSpan();
        StringBuilder phraseText = new StringBuilder();
        
        // Consume tokens until we hit 'with' or ':'
        while (!check(TokenType.WITH) && !check(TokenType.COLON) && !isAtEnd()) {
            if (!phraseText.isEmpty()) {
                phraseText.append(" ");
            }
            phraseText.append(peek().getLexeme());
            advance();
        }
        
        return NaturalLanguagePhrase.from(span, phraseText.toString());
    }
    
    private List<ParameterDecl> parameterList() {
        List<ParameterDecl> parameters = new ArrayList<>();
        
        do {
            if (check(TokenType.AND) || check(TokenType.COMMA)) {
                advance();
            }
            
            SourceSpan paramSpan = currentSpan();
            Token nameToken = consume(TokenType.IDENTIFIER, "Expected parameter name");
            if (nameToken == null) break;
            
            TypeRef type = null;
            if (check(TokenType.AS) || (check(TokenType.COLON) && checkNextIsTypeStart())) {
                advance();
                type = typeReference();
            }
            
            if (type == null) {
                type = new TypeRef(paramSpan, "Object", null, false, false, null);
            }
            
            parameters.add(new ParameterDecl(paramSpan, nameToken.getLexeme(), type, false, null));
            
        } while (check(TokenType.AND) || check(TokenType.COMMA));
        
        return parameters;
    }
    
    // ========== Clause Parsing ==========
    
    private RequireClause requireClause() {
        SourceSpan span = currentSpan();
        advance(); // consume 'require'
        
        consume(TokenType.THAT, "Expected 'that' after 'require'");
        consume(TokenType.COLON, "Expected ':' after 'require that'");
        
        List<RequireClause.RequireCondition> conditions = new ArrayList<>();
        
        while (check(TokenType.DASH)) {
            advance(); // consume '-'
            
            NaturalLanguageCondition condition = parseNaturalLanguageCondition();
            String errorMessage = null;
            
            if (check(TokenType.COMMA)) {
                advance();
                consume(TokenType.OTHERWISE, "Expected 'otherwise' after ','");
                Token msgToken = consume(TokenType.STRING_LITERAL, "Expected error message");
                if (msgToken != null) {
                    errorMessage = msgToken.getStringValue();
                }
            }
            
            conditions.add(new RequireClause.RequireCondition(currentSpan(), condition, errorMessage));
        }
        
        return new RequireClause(span, conditions);
    }
    
    private NaturalLanguageCondition parseNaturalLanguageCondition() {
        SourceSpan span = currentSpan();
        StringBuilder conditionText = new StringBuilder();
        
        // Consume tokens until comma, newline, or end of conditions
        while (!check(TokenType.COMMA) && !check(TokenType.DASH) && 
               !check(TokenType.GIVEN) && !check(TokenType.THEN) && 
               !check(TokenType.REQUIRE) && !check(TokenType.RIGHT_BRACE) && 
               !check(TokenType.COLON) && !isAtEnd()) {
            if (!conditionText.isEmpty()) {
                conditionText.append(" ");
            }
            conditionText.append(peek().getLexeme());
            advance();
        }
        
        return NaturalLanguageCondition.simple(span, conditionText.toString(), null);
    }
    
    private GivenClause givenClause() {
        SourceSpan span = currentSpan();
        advance(); // consume 'given'
        
        consume(TokenType.COLON, "Expected ':' after 'given'");
        
        List<GivenClause.GivenStatement> statements = new ArrayList<>();
        
        while (check(TokenType.DASH)) {
            advance(); // consume '-'
            
            SourceSpan stmtSpan = currentSpan();
            Token identToken = consume(TokenType.IDENTIFIER, "Expected identifier");
            if (identToken == null) continue;
            
            GivenClause.GivenStatement.GivenStatementType type = GivenClause.GivenStatement.GivenStatementType.AS;
            Expr expr = null;
            
            if (check(TokenType.CREATED)) {
                advance();
                consume(TokenType.FROM, "Expected 'from' after 'created'");
                type = GivenClause.GivenStatement.GivenStatementType.CREATED_FROM;
                expr = expression();
            } else if (check(TokenType.CALCULATED)) {
                advance();
                consume(TokenType.BY, "Expected 'by' after 'calculated'");
                type = GivenClause.GivenStatement.GivenStatementType.CALCULATED_BY;
                expr = expression();
            } else if (check(TokenType.DETERMINED)) {
                advance();
                consume(TokenType.BY, "Expected 'by' after 'determined'");
                type = GivenClause.GivenStatement.GivenStatementType.DETERMINED_BY;
                expr = expression();
            } else if (check(TokenType.AS)) {
                advance();
                type = GivenClause.GivenStatement.GivenStatementType.AS;
                expr = expression();
            } else if (check(TokenType.FROM)) {
                advance();
                type = GivenClause.GivenStatement.GivenStatementType.FROM;
                expr = expression();
            } else {
                // Default: identifier as expression
                expr = expression();
            }
            
            if (expr != null) {
                statements.add(new GivenClause.GivenStatement(stmtSpan, identToken.getLexeme(), type, expr));
            }
        }
        
        return new GivenClause(span, statements);
    }
    
    private ThenClause thenClause() {
        SourceSpan span = currentSpan();
        advance(); // consume 'then'
        
        consume(TokenType.COLON, "Expected ':' after 'then'");
        
        List<ThenClause.ThenStatement> statements = new ArrayList<>();
        
        while (check(TokenType.DASH)) {
            ThenClause.ThenStatement statement = parseThenStatement();
            if (statement != null) {
                statements.add(statement);
            }
        }
        
        return new ThenClause(span, statements);
    }
    
    private ThenClause.ThenStatement parseThenStatement() {
        SourceSpan span = currentSpan();
        
        advance(); // consume '-'
        
        ThenClause.ThenStatement.ThenStatementType type = ThenClause.ThenStatement.ThenStatementType.SET;
        String target = null;
        Expr value = null;
        
        if (check(TokenType.SET)) {
            advance();
            type = ThenClause.ThenStatement.ThenStatementType.SET;
            Token targetToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'set'");
            target = targetToken != null ? targetToken.getLexeme() : "";
            consume(TokenType.TO, "Expected 'to' after identifier");
            value = expression();
        } else if (check(TokenType.CHANGE)) {
            advance();
            type = ThenClause.ThenStatement.ThenStatementType.CHANGE;
            Token targetToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'change'");
            target = targetToken != null ? targetToken.getLexeme() : "";
            consume(TokenType.TO, "Expected 'to' after identifier");
            value = expression();
        } else if (check(TokenType.RECORD)) {
            advance();
            type = ThenClause.ThenStatement.ThenStatementType.RECORD;
            Token targetToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'record'");
            target = targetToken != null ? targetToken.getLexeme() : "";
            consume(TokenType.AS, "Expected 'as' after identifier");
            value = expression();
        } else if (check(TokenType.CALCULATE)) {
            advance();
            type = ThenClause.ThenStatement.ThenStatementType.CALCULATE;
            Token targetToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'calculate'");
            target = targetToken != null ? targetToken.getLexeme() : "";
            consume(TokenType.AS, "Expected 'as' after identifier");
            value = expression();
        } else if (check(TokenType.CREATE)) {
            advance();
            type = ThenClause.ThenStatement.ThenStatementType.CREATE;
            Token targetToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'create'");
            target = targetToken != null ? targetToken.getLexeme() : "";
            consume(TokenType.FROM, "Expected 'from' after identifier");
            value = expression();
        } else if (check(TokenType.ADD)) {
            advance();
            type = ThenClause.ThenStatement.ThenStatementType.ADD;
            value = expression();
            consume(TokenType.TO, "Expected 'to' after expression");
            Token targetToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'to'");
            target = targetToken != null ? targetToken.getLexeme() : "";
        } else if (check(TokenType.REMOVE)) {
            advance();
            type = ThenClause.ThenStatement.ThenStatementType.REMOVE;
            value = expression();
            consume(TokenType.FROM, "Expected 'from' after expression");
            Token targetToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'from'");
            target = targetToken != null ? targetToken.getLexeme() : "";
        } else if (check(TokenType.SAVE)) {
            advance();
            type = ThenClause.ThenStatement.ThenStatementType.METHOD_CALL;
            Token targetToken = consume(TokenType.IDENTIFIER, "Expected identifier after 'save'");
            target = targetToken != null ? targetToken.getLexeme() : "";
            consume(TokenType.TO, "Expected 'to' after identifier");
            Token repoToken = consume(TokenType.IDENTIFIER, "Expected repository name");
            value = new VariableExpr(currentSpan(), repoToken != null ? repoToken.getLexeme() : "repository");
        } else if (check(TokenType.IF)) {
            return parseConditionalStatement();
        } else if (check(TokenType.FOR)) {
            return parseLoopStatement();
        } else {
            // Try to parse as method call or expression
            value = expression();
            type = ThenClause.ThenStatement.ThenStatementType.METHOD_CALL;
        }
        
        return ThenClause.ThenStatement.simple(span, type, target, value);
    }
    
    private ThenClause.ThenStatement parseConditionalStatement() {
        SourceSpan span = currentSpan();
        advance(); // consume 'if'
        
        NaturalLanguageCondition condition = parseNaturalLanguageCondition();
        consume(TokenType.COLON, "Expected ':' after condition");
        
        List<ThenClause.ThenStatement> thenStatements = new ArrayList<>();
        while (check(TokenType.DASH)) {
            ThenClause.ThenStatement stmt = parseThenStatement();
            if (stmt != null) {
                thenStatements.add(stmt);
            }
        }
        
        List<ThenClause.ThenStatement> elseStatements = new ArrayList<>();
        if (check(TokenType.OTHERWISE)) {
            advance();
            consume(TokenType.COLON, "Expected ':' after 'otherwise'");
            while (check(TokenType.DASH)) {
                ThenClause.ThenStatement stmt = parseThenStatement();
                if (stmt != null) {
                    elseStatements.add(stmt);
                }
            }
        }
        
        return ThenClause.ThenStatement.ifStatement(span, condition, thenStatements, List.of(), elseStatements);
    }
    
    private ThenClause.ThenStatement parseLoopStatement() {
        SourceSpan span = currentSpan();
        advance(); // consume 'for'
        
        consume(TokenType.EACH, "Expected 'each' after 'for'");
        
        Token itemToken = consume(TokenType.IDENTIFIER, "Expected loop variable name");
        String itemName = itemToken != null ? itemToken.getLexeme() : "item";
        
        consume(TokenType.IN, "Expected 'in' after loop variable");
        
        Token collectionToken = consume(TokenType.IDENTIFIER, "Expected collection name");
        String collectionName = collectionToken != null ? collectionToken.getLexeme() : "collection";
        
        consume(TokenType.COLON, "Expected ':' after collection");
        
        List<ThenClause.ThenStatement> bodyStatements = new ArrayList<>();
        while (check(TokenType.DASH)) {
            ThenClause.ThenStatement stmt = parseThenStatement();
            if (stmt != null) {
                bodyStatements.add(stmt);
            }
        }
        
        return ThenClause.ThenStatement.forEach(span, itemName, collectionName, bodyStatements);
    }
    
    private EmitClause emitClause() {
        SourceSpan span = currentSpan();
        advance(); // consume 'emit'
        
        Token eventNameToken = consume(TokenType.IDENTIFIER, "Expected event name");
        String eventName = eventNameToken != null ? eventNameToken.getLexeme() : "";
        
        // Consume 'event' if present
        if (check(TokenType.EVENT)) {
            advance();
        }
        
        List<String> eventArgs = new ArrayList<>();
        List<EmitClause.EventPropertyMapping> propertyMappings = new ArrayList<>();
        
        // Parse optional property mappings
        if (check(TokenType.WITH)) {
            advance();
            
            if (check(TokenType.COLON)) {
                consume(TokenType.COLON, "Expected ':' after 'with'");
                
                while (check(TokenType.DASH)) {
                    advance();
                    Token propToken = consume(TokenType.IDENTIFIER, "Expected property name");
                    if (propToken != null) {
                        if (check(TokenType.SET) || check(TokenType.AS)) {
                            advance();
                            if (check(TokenType.TO)) advance();
                        }
                        Expr value = expression();
                        propertyMappings.add(new EmitClause.EventPropertyMapping(
                            currentSpan(), propToken.getLexeme(), value));
                    }
                }
            } else {
                // Simple argument list: "with reason and time"
                do {
                    if (check(TokenType.AND) || check(TokenType.COMMA)) {
                        advance();
                    }
                    Token argToken = consume(TokenType.IDENTIFIER, "Expected argument name");
                    if (argToken != null) {
                        eventArgs.add(argToken.getLexeme());
                    }
                } while (check(TokenType.AND) || check(TokenType.COMMA));
            }
        }
        
        return new EmitClause(span, eventName, eventArgs, propertyMappings);
    }
    
    private ReturnClause returnClause() {
        SourceSpan span = currentSpan();
        advance(); // consume 'return'
        
        // Check for success/failure
        if (check(TokenType.SUCCESS)) {
            advance();
            Expr expr = null;
            if (check(TokenType.WITH)) {
                advance();
                expr = expression();
                return ReturnClause.successWith(span, expr);
            }
            return ReturnClause.success(span);
        } else if (check(TokenType.FAILURE)) {
            advance();
            String message = "";
            if (check(TokenType.WITH)) {
                advance();
                Token msgToken = consume(TokenType.STRING_LITERAL, "Expected error message");
                if (msgToken != null) {
                    message = msgToken.getStringValue();
                }
            }
            return ReturnClause.failure(span, message);
        }
        
        // Check for object construction: return TypeName with:
        Token typeToken = consume(TokenType.IDENTIFIER, "Expected return type");
        String entityType = typeToken != null ? typeToken.getLexeme() : "";
        
        if (check(TokenType.WITH)) {
            consume(TokenType.WITH, "Expected 'with' after return type");
            consume(TokenType.COLON, "Expected ':' after 'with'");
            
            List<ReturnClause.PropertyInitialization> properties = new ArrayList<>();
            
            while (check(TokenType.DASH)) {
                advance();
                Token propToken = consume(TokenType.IDENTIFIER, "Expected property name");
                if (propToken != null) {
                    if (check(TokenType.SET)) {
                        advance();
                        consume(TokenType.TO, "Expected 'to' after 'set'");
                    } else if (check(TokenType.AS)) {
                        advance();
                    }
                    
                    Expr value = expression();
                    properties.add(new ReturnClause.PropertyInitialization(
                        currentSpan(), propToken.getLexeme(), value));
                }
            }
            
            return ReturnClause.object(span, entityType, properties);
        }
        
        // Simple expression return
        return ReturnClause.expression(span, new VariableExpr(span, entityType));
    }
    
    // ========== Expression Parsing ==========
    
    private Expr expression() {
        return orExpression();
    }
    
    private Expr orExpression() {
        Expr expr = andExpression();
        
        while (check(TokenType.OR)) {
            advance();
            Expr right = andExpression();
            expr = new BinaryExpr(currentSpan(), expr, BinaryExpr.BinaryOperator.OR, right);
        }
        
        return expr;
    }
    
    private Expr andExpression() {
        Expr expr = comparisonExpression();
        
        while (check(TokenType.AND)) {
            advance();
            Expr right = comparisonExpression();
            expr = new BinaryExpr(currentSpan(), expr, BinaryExpr.BinaryOperator.AND, right);
        }
        
        return expr;
    }
    
    private Expr comparisonExpression() {
        Expr expr = additiveExpression();
        
        while (check(TokenType.IS) || check(TokenType.EQUALS) || 
               check(TokenType.GT) || check(TokenType.LT) ||
               check(TokenType.GTE) || check(TokenType.LTE) ||
               check(TokenType.NEQ) || check(TokenType.EQ) ||
               check(TokenType.EXCEEDS)) {
            
            BinaryExpr.BinaryOperator op = parseComparisonOperator();
            Expr right = additiveExpression();
            expr = new BinaryExpr(currentSpan(), expr, op, right);
        }
        
        return expr;
    }
    
    private BinaryExpr.BinaryOperator parseComparisonOperator() {
        if (check(TokenType.IS)) {
            advance();
            if (check(TokenType.NOT)) {
                advance();
                if (check(TokenType.EQUAL)) {
                    advance();
                    if (check(TokenType.TO)) advance();
                } else if (check(TokenType.EMPTY)) {
                    advance();
                }
                return BinaryExpr.BinaryOperator.NOT_EQUALS;
            } else if (check(TokenType.EQUAL)) {
                advance();
                if (check(TokenType.TO)) advance();
                return BinaryExpr.BinaryOperator.EQUALS;
            } else if (check(TokenType.GREATER)) {
                advance();
                consume(TokenType.THAN, "Expected 'than' after 'greater'");
                return BinaryExpr.BinaryOperator.GREATER_THAN;
            } else if (check(TokenType.LESS)) {
                advance();
                consume(TokenType.THAN, "Expected 'than' after 'less'");
                return BinaryExpr.BinaryOperator.LESS_THAN;
            } else if (check(TokenType.AT)) {
                advance();
                if (check(TokenType.LEAST)) {
                    advance();
                    return BinaryExpr.BinaryOperator.GREATER_THAN_OR_EQUAL;
                } else if (check(TokenType.MOST)) {
                    advance();
                    return BinaryExpr.BinaryOperator.LESS_THAN_OR_EQUAL;
                }
            } else if (check(TokenType.ONE)) {
                advance();
                consume(TokenType.OF, "Expected 'of' after 'one'");
                return BinaryExpr.BinaryOperator.IN;
            } else if (check(TokenType.EMPTY)) {
                advance();
                return BinaryExpr.BinaryOperator.EQUALS; // Handle specially
            }
            return BinaryExpr.BinaryOperator.EQUALS;
        } else if (check(TokenType.EQUALS)) {
            advance();
            return BinaryExpr.BinaryOperator.EQUALS;
        } else if (check(TokenType.GT) || check(TokenType.RIGHT_ANGLE)) {
            advance();
            return BinaryExpr.BinaryOperator.GREATER_THAN;
        } else if (check(TokenType.LT) || check(TokenType.LEFT_ANGLE)) {
            advance();
            return BinaryExpr.BinaryOperator.LESS_THAN;
        } else if (check(TokenType.GTE)) {
            advance();
            return BinaryExpr.BinaryOperator.GREATER_THAN_OR_EQUAL;
        } else if (check(TokenType.LTE)) {
            advance();
            return BinaryExpr.BinaryOperator.LESS_THAN_OR_EQUAL;
        } else if (check(TokenType.NEQ)) {
            advance();
            return BinaryExpr.BinaryOperator.NOT_EQUALS;
        } else if (check(TokenType.EQ)) {
            advance();
            return BinaryExpr.BinaryOperator.EQUALS;
        } else if (check(TokenType.EXCEEDS)) {
            advance();
            return BinaryExpr.BinaryOperator.GREATER_THAN;
        }
        
        return BinaryExpr.BinaryOperator.EQUALS;
    }
    
    private Expr additiveExpression() {
        Expr expr = multiplicativeExpression();
        
        while (check(TokenType.PLUS) || check(TokenType.PLUS_SYMBOL) ||
               check(TokenType.MINUS) || check(TokenType.MINUS_SYMBOL) ||
               (check(TokenType.DASH) && !isDashListBullet())) {
            
            BinaryExpr.BinaryOperator op;
            if (check(TokenType.PLUS) || check(TokenType.PLUS_SYMBOL)) {
                advance();
                op = BinaryExpr.BinaryOperator.ADD;
            } else {
                advance();
                op = BinaryExpr.BinaryOperator.SUBTRACT;
            }
            
            Expr right = multiplicativeExpression();
            expr = new BinaryExpr(currentSpan(), expr, op, right);
        }
        
        return expr;
    }
    
    /**
     * Determine if a DASH token is a list bullet rather than a minus sign.
     * A dash is a list bullet if it's followed by a then-statement keyword,
     * an identifier (field/method reference), or another natural-language keyword
     * used at the start of a statement.
     */
    private boolean isDashListBullet() {
        if (!check(TokenType.DASH)) return false;
        if (current + 1 >= tokens.size()) return false;
        TokenType next = tokens.get(current + 1).getType();
        return next == TokenType.SET || next == TokenType.CHANGE ||
               next == TokenType.CALCULATE || next == TokenType.CREATE ||
               next == TokenType.ADD || next == TokenType.REMOVE ||
               next == TokenType.SAVE || next == TokenType.RECORD ||
               next == TokenType.IF || next == TokenType.FOR ||
               next == TokenType.ENABLE || next == TokenType.DISABLE ||
               next == TokenType.IDENTIFIER;
    }
    
    private Expr multiplicativeExpression() {
        Expr expr = unaryExpression();
        
        while (check(TokenType.TIMES) || check(TokenType.STAR) ||
               check(TokenType.DIVIDED) || check(TokenType.SLASH)) {
            
            BinaryExpr.BinaryOperator op;
            if (check(TokenType.TIMES) || check(TokenType.STAR)) {
                advance();
                op = BinaryExpr.BinaryOperator.MULTIPLY;
            } else {
                advance();
                if (check(TokenType.BY)) advance();
                op = BinaryExpr.BinaryOperator.DIVIDE;
            }
            
            Expr right = unaryExpression();
            expr = new BinaryExpr(currentSpan(), expr, op, right);
        }
        
        return expr;
    }
    
    private Expr unaryExpression() {
        if (check(TokenType.NOT)) {
            advance();
            Expr expr = unaryExpression();
            return new UnaryExpr(currentSpan(), UnaryExpr.UnaryOperator.NOT, expr);
        }
        
        if (check(TokenType.MINUS) || check(TokenType.MINUS_SYMBOL) || 
            (check(TokenType.DASH) && !isDashListBullet())) {
            advance();
            Expr expr = unaryExpression();
            return new UnaryExpr(currentSpan(), UnaryExpr.UnaryOperator.NEGATE, expr);
        }
        
        return callExpression();
    }
    
    private Expr callExpression() {
        Expr expr = primaryExpression();
        
        while (true) {
            if (check(TokenType.LEFT_PAREN)) {
                advance();
                List<Expr> arguments = new ArrayList<>();
                
                if (!check(TokenType.RIGHT_PAREN)) {
                    do {
                        if (check(TokenType.COMMA)) advance();
                        arguments.add(expression());
                    } while (check(TokenType.COMMA));
                }
                
                consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
                
                if (expr instanceof FieldAccessExpr fa) {
                    expr = new MethodCallExpr(currentSpan(), fa.object(), fa.fieldName(), arguments);
                } else if (expr instanceof VariableExpr var) {
                    expr = new MethodCallExpr(currentSpan(), null, var.name(), arguments);
                }
            } else if (check(TokenType.DOT)) {
                advance();
                Token nameToken = consume(TokenType.IDENTIFIER, "Expected property name after '.'");
                String name = nameToken != null ? nameToken.getLexeme() : "";
                expr = new FieldAccessExpr(currentSpan(), expr, name);
            } else if (check(TokenType.LEFT_BRACKET)) {
                advance();
                Expr index = expression();
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after index");
                // Use IndexExpr or just FieldAccess for now
                expr = new FieldAccessExpr(currentSpan(), expr, "[index]");
            } else {
                break;
            }
        }
        
        return expr;
    }
    
    private Expr primaryExpression() {
        SourceSpan span = currentSpan();
        
        // Literals
        if (check(TokenType.INTEGER_LITERAL)) {
            Token token = advance();
            return new LiteralExpr(span, token.getLiteral(), LiteralExpr.LiteralType.INTEGER);
        }
        
        if (check(TokenType.DECIMAL_LITERAL)) {
            Token token = advance();
            return new LiteralExpr(span, token.getLiteral(), LiteralExpr.LiteralType.DECIMAL);
        }
        
        if (check(TokenType.STRING_LITERAL)) {
            Token token = advance();
            return new LiteralExpr(span, token.getStringValue(), LiteralExpr.LiteralType.STRING);
        }
        
        if (check(TokenType.TRUE)) {
            advance();
            return new LiteralExpr(span, true, LiteralExpr.LiteralType.BOOLEAN);
        }
        
        if (check(TokenType.FALSE)) {
            advance();
            return new LiteralExpr(span, false, LiteralExpr.LiteralType.BOOLEAN);
        }
        
        if (check(TokenType.NULL)) {
            advance();
            return new NullExpr(span);
        }
        
        if (check(TokenType.NOW)) {
            advance();
            return new MethodCallExpr(span, null, "now", List.of());
        }
        
        if (check(TokenType.NEW)) {
            advance();
            if (check(TokenType.GENERATED)) {
                advance();
            }
            Token typeToken = consume(TokenType.IDENTIFIER, "Expected type after 'new'");
            String typeName = typeToken != null ? typeToken.getLexeme() : "";
            
            List<Expr> args = new ArrayList<>();
            if (check(TokenType.LEFT_PAREN)) {
                advance();
                if (!check(TokenType.RIGHT_PAREN)) {
                    do {
                        if (check(TokenType.COMMA)) advance();
                        args.add(expression());
                    } while (check(TokenType.COMMA));
                }
                consume(TokenType.RIGHT_PAREN, "Expected ')' after constructor arguments");
            }
            
            TypeRef type = new TypeRef(span, typeName, null, false, false, null);
            return new NewInstanceExpr(span, type, args);
        }
        
        // Collection aggregation: sum of, count of
        if (check(TokenType.SUM)) {
            advance();
            consume(TokenType.OF, "Expected 'of' after 'sum'");
            Expr collection = expression();
            return new MethodCallExpr(span, collection, "sum", List.of());
        }
        
        if (check(TokenType.COUNT)) {
            advance();
            consume(TokenType.OF, "Expected 'of' after 'count'");
            Expr collection = expression();
            return new MethodCallExpr(span, collection, "count", List.of());
        }
        
        // Collection quantifiers: all items have, any item has
        if (check(TokenType.ALL)) {
            advance();
            Token collToken = consume(TokenType.IDENTIFIER, "Expected collection name");
            consume(TokenType.HAVE, "Expected 'have' after collection");
            Expr condition = expression();
            return new MethodCallExpr(span,
                new VariableExpr(span, collToken != null ? collToken.getLexeme() : "items"),
                "allMatch", List.of(condition));
        }
        
        if (check(TokenType.ANY)) {
            advance();
            Token collToken = consume(TokenType.IDENTIFIER, "Expected collection name");
            if (check(TokenType.HAS)) advance();
            else consume(TokenType.HAVE, "Expected 'has/have' after collection");
            Expr condition = expression();
            return new MethodCallExpr(span,
                new VariableExpr(span, collToken != null ? collToken.getLexeme() : "items"),
                "anyMatch", List.of(condition));
        }
        
        // Parenthesized expression
        if (check(TokenType.LEFT_PAREN)) {
            advance();
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return expr;
        }
        
        // List literal
        if (check(TokenType.LEFT_BRACKET)) {
            advance();
            List<Expr> elements = new ArrayList<>();
            
            if (!check(TokenType.RIGHT_BRACKET)) {
                do {
                    if (check(TokenType.COMMA)) advance();
                    elements.add(expression());
                } while (check(TokenType.COMMA));
            }
            
            consume(TokenType.RIGHT_BRACKET, "Expected ']' after list elements");
            return new ListExpr(span, elements);
        }
        
        // Identifier
        if (check(TokenType.IDENTIFIER)) {
            Token token = advance();
            return new VariableExpr(span, token.getLexeme());
        }
        
        // Handle natural language tokens as identifiers
        if (peek().getType().isComparisonPredicate() || 
            peek().getType().isCollectionPredicate()) {
            Token token = advance();
            return new VariableExpr(span, token.getLexeme());
        }
        
        error("Expected expression, got: " + peek().getLexeme());
        return new NullExpr(span);
    }
    
    // ========== Extended Expression Parsing ==========
    // Temporal, String, Collection, Specification, and Match expressions
    
    /**
     * Parse a temporal expression from the token stream.
     * 
     * Syntax:
     *   expression is before/after/on anchor
     *   expression is within last/next duration
     *   expression is between anchor1 and anchor2
     *   expression is more than N units ago/from now
     */
    public TemporalExpr parseTemporalExpression(SourceSpan span, Expr subject) {
        if (matchSequence(TokenType.IS, TokenType.BEFORE)) {
            TemporalAnchor anchor = parseTemporalAnchor(span);
            return new TemporalComparison(span, subject, TemporalComparison.TemporalComparisonOp.IS_BEFORE, anchor);
        }
        
        if (matchSequence(TokenType.IS, TokenType.AFTER)) {
            TemporalAnchor anchor = parseTemporalAnchor(span);
            return new TemporalComparison(span, subject, TemporalComparison.TemporalComparisonOp.IS_AFTER, anchor);
        }
        
        if (matchSequence(TokenType.IS, TokenType.ON)) {
            TemporalAnchor anchor = parseTemporalAnchor(span);
            return new TemporalComparison(span, subject, TemporalComparison.TemporalComparisonOp.IS_ON, anchor);
        }
        
        if (matchSequence(TokenType.IS, TokenType.WITHIN, TokenType.LAST)) {
            Duration duration = parseDuration(span);
            return new TemporalRange(span, subject, TemporalRange.RangeType.WITHIN_LAST, duration, null, null);
        }
        
        if (matchSequence(TokenType.IS, TokenType.WITHIN, TokenType.NEXT)) {
            Duration duration = parseDuration(span);
            return new TemporalRange(span, subject, TemporalRange.RangeType.WITHIN_NEXT, duration, null, null);
        }
        
        if (matchSequence(TokenType.IS, TokenType.BETWEEN)) {
            TemporalAnchor start = parseTemporalAnchor(span);
            consume(TokenType.AND, "Expected 'and' in between clause");
            TemporalAnchor end = parseTemporalAnchor(span);
            return TemporalRange.betweenAnchors(span, subject, start, end);
        }
        
        if (matchSequence(TokenType.IS, TokenType.MORE, TokenType.THAN)) {
            Duration duration = parseDuration(span);
            TemporalRelative.RelativeDirection direction = parseTemporalDirection();
            return new TemporalRelative(span, subject, TemporalRelative.RelativeOp.MORE_THAN, duration, direction);
        }
        
        if (matchSequence(TokenType.IS, TokenType.LESS, TokenType.THAN)) {
            Duration duration = parseDuration(span);
            TemporalRelative.RelativeDirection direction = parseTemporalDirection();
            return new TemporalRelative(span, subject, TemporalRelative.RelativeOp.LESS_THAN, duration, direction);
        }
        
        if (matchSequence(TokenType.OCCURRED, TokenType.BEFORE)) {
            Expr other = null; // Would parse another expression
            return new TemporalSequence(span, subject, TemporalSequence.SequenceOp.OCCURRED_BEFORE, other);
        }
        
        if (matchSequence(TokenType.OCCURRED, TokenType.AFTER)) {
            Expr other = null; // Would parse another expression
            return new TemporalSequence(span, subject, TemporalSequence.SequenceOp.OCCURRED_AFTER, other);
        }
        
        return null;
    }
    
    private TemporalAnchor parseTemporalAnchor(SourceSpan span) {
        if (match(TokenType.NOW)) {
            return TemporalAnchor.now(span);
        }
        if (match(TokenType.TODAY)) {
            return TemporalAnchor.today(span);
        }
        if (match(TokenType.YESTERDAY)) {
            return TemporalAnchor.yesterday(span);
        }
        if (match(TokenType.TOMORROW)) {
            return TemporalAnchor.tomorrow(span);
        }
        if (check(TokenType.INTEGER_LITERAL)) {
            Duration duration = parseDuration(span);
            TemporalRelative.RelativeDirection direction = parseTemporalDirection();
            if (direction == TemporalRelative.RelativeDirection.AGO) {
                return TemporalAnchor.ago(span, duration);
            } else {
                return TemporalAnchor.fromNow(span, duration);
            }
        }
        
        return null;
    }
    
    private Duration parseDuration(SourceSpan span) {
        int amount = 0;
        Duration.DurationUnit unit = Duration.DurationUnit.DAYS;
        
        if (check(TokenType.INTEGER_LITERAL)) {
            Token amountToken = advance();
            amount = Integer.parseInt(amountToken.getLexeme());
        }
        
        if (match(TokenType.SECONDS)) unit = Duration.DurationUnit.SECONDS;
        else if (match(TokenType.MINUTES)) unit = Duration.DurationUnit.MINUTES;
        else if (match(TokenType.HOURS)) unit = Duration.DurationUnit.HOURS;
        else if (match(TokenType.DAYS)) unit = Duration.DurationUnit.DAYS;
        else if (match(TokenType.WEEKS)) unit = Duration.DurationUnit.WEEKS;
        else if (match(TokenType.MONTHS)) unit = Duration.DurationUnit.MONTHS;
        else if (match(TokenType.YEARS)) unit = Duration.DurationUnit.YEARS;
        
        return Duration.of(span, amount, unit);
    }
    
    private TemporalRelative.RelativeDirection parseTemporalDirection() {
        if (match(TokenType.AGO)) {
            return TemporalRelative.RelativeDirection.AGO;
        }
        if (matchSequence(TokenType.FROM, TokenType.NOW)) {
            return TemporalRelative.RelativeDirection.FROM_NOW;
        }
        return TemporalRelative.RelativeDirection.AGO;
    }
    
    /**
     * Parse a string condition from the token stream.
     * 
     * Syntax:
     *   expression contains "text"
     *   expression starts with "prefix"
     *   expression ends with "suffix"
     *   expression matches "pattern"
     *   expression is empty/blank
     *   expression has valid email/phone/url format
     *   expression has length greater than N
     */
    public StringCondition parseStringCondition(SourceSpan span, Expr subject) {
        if (match(TokenType.CONTAINS)) {
            String value = parseStringLiteralValue();
            return StringCondition.contains(span, subject, value);
        }
        
        if (matchSequence(TokenType.STARTS, TokenType.WITH)) {
            String value = parseStringLiteralValue();
            return StringCondition.startsWith(span, subject, value);
        }
        
        if (matchSequence(TokenType.ENDS, TokenType.WITH)) {
            String value = parseStringLiteralValue();
            return StringCondition.endsWith(span, subject, value);
        }
        
        if (match(TokenType.MATCHES)) {
            String pattern = parseStringLiteralValue();
            return StringCondition.matches(span, subject, pattern);
        }
        
        if (matchSequence(TokenType.IS, TokenType.EMPTY)) {
            return StringCondition.isEmpty(span, subject);
        }
        
        if (matchSequence(TokenType.IS, TokenType.BLANK)) {
            return StringCondition.isBlank(span, subject);
        }
        
        if (matchSequence(TokenType.HAS, TokenType.VALID)) {
            StringCondition.FormatType formatType = parseFormatType();
            consume(TokenType.FORMAT, "Expected 'format' after format type");
            return StringCondition.hasValidFormat(span, subject, formatType);
        }
        
        if (matchSequence(TokenType.HAS, TokenType.LENGTH)) {
            if (matchSequence(TokenType.GREATER, TokenType.THAN)) {
                int value = parseIntegerLiteralValue();
                return StringCondition.hasLengthGreaterThan(span, subject, value);
            }
            if (matchSequence(TokenType.LESS, TokenType.THAN)) {
                int value = parseIntegerLiteralValue();
                return StringCondition.hasLengthLessThan(span, subject, value);
            }
            if (matchSequence(TokenType.AT, TokenType.LEAST)) {
                int value = parseIntegerLiteralValue();
                return StringCondition.hasLengthAtLeast(span, subject, value);
            }
            if (matchSequence(TokenType.AT, TokenType.MOST)) {
                int value = parseIntegerLiteralValue();
                return StringCondition.hasLengthAtMost(span, subject, value);
            }
            if (match(TokenType.EXACTLY)) {
                int value = parseIntegerLiteralValue();
                return StringCondition.hasLengthExactly(span, subject, value);
            }
        }
        
        return null;
    }
    
    /**
     * Parse a string operation (transformation).
     * 
     * Syntax:
     *   expression to uppercase/lowercase
     *   expression trimmed
     *   expression truncated to N characters
     *   expression concatenated with "suffix"
     */
    public StringOperation parseStringOperation(SourceSpan span, Expr subject) {
        if (check(TokenType.TO)) {
            advance();
            if (peek().getLexeme().equalsIgnoreCase("uppercase")) {
                advance();
                return StringOperation.toUppercase(span, subject);
            }
            if (peek().getLexeme().equalsIgnoreCase("lowercase")) {
                advance();
                return StringOperation.toLowercase(span, subject);
            }
        }
        
        if (match(TokenType.TRUNCATED)) {
            consume(TokenType.TO, "Expected 'to' after 'truncated'");
            int length = parseIntegerLiteralValue();
            match(TokenType.CHARACTERS); // Optional
            return StringOperation.truncatedTo(span, subject, length);
        }
        
        if (match(TokenType.CONCATENATED)) {
            consume(TokenType.WITH, "Expected 'with' after 'concatenated'");
            String suffix = parseStringLiteralValue();
            return StringOperation.concatenatedWith(span, subject, suffix);
        }
        
        if (match(TokenType.REPLACED)) {
            String target = parseStringLiteralValue();
            consume(TokenType.WITH, "Expected 'with' in replace operation");
            String replacement = parseStringLiteralValue();
            return StringOperation.replaced(span, subject, target, replacement);
        }
        
        if (match(TokenType.FIRST)) {
            int n = parseIntegerLiteralValue();
            match(TokenType.CHARACTERS);
            return StringOperation.firstNCharacters(span, subject, n);
        }
        
        if (match(TokenType.LAST)) {
            int n = parseIntegerLiteralValue();
            match(TokenType.CHARACTERS);
            return StringOperation.lastNCharacters(span, subject, n);
        }
        
        return null;
    }
    
    private StringCondition.FormatType parseFormatType() {
        if (match(TokenType.EMAIL)) return StringCondition.FormatType.EMAIL;
        if (match(TokenType.PHONE)) return StringCondition.FormatType.PHONE_NUMBER;
        if (match(TokenType.URL)) return StringCondition.FormatType.URL;
        if (match(TokenType.UUID_TYPE)) return StringCondition.FormatType.UUID;
        if (match(TokenType.DATETIME_TYPE)) return StringCondition.FormatType.DATE;
        if (match(TokenType.NUMERIC)) return StringCondition.FormatType.NUMERIC;
        if (match(TokenType.ALPHANUMERIC)) return StringCondition.FormatType.ALPHANUMERIC;
        return StringCondition.FormatType.ALPHANUMERIC;
    }
    
    /**
     * Parse a collection aggregation expression.
     * 
     * Syntax:
     *   sum of collection property where condition
     *   count of collection where condition
     *   maximum of collection property where condition
     */
    public CollectionAggregation parseCollectionAggregation(SourceSpan span) {
        CollectionAggregation.AggregationType type = null;
        
        if (match(TokenType.SUM)) type = CollectionAggregation.AggregationType.SUM;
        else if (match(TokenType.COUNT)) type = CollectionAggregation.AggregationType.COUNT;
        else if (match(TokenType.MAXIMUM)) type = CollectionAggregation.AggregationType.MAXIMUM;
        else if (match(TokenType.MINIMUM)) type = CollectionAggregation.AggregationType.MINIMUM;
        else if (match(TokenType.AVERAGE)) type = CollectionAggregation.AggregationType.AVERAGE;
        else return null;
        
        consume(TokenType.OF, "Expected 'of' after aggregation type");
        
        // Parse collection identifier
        Expr collection = parseSimpleIdentifierExpr(span);
        
        // Parse optional property path (for sum, max, min, avg)
        String property = null;
        if (type != CollectionAggregation.AggregationType.COUNT) {
            if (check(TokenType.IDENTIFIER)) {
                property = parsePropertyPath();
            }
        }
        
        // Parse optional where clause
        NaturalLanguageCondition where = null;
        if (match(TokenType.WHERE)) {
            where = parseWhereConditionExtended(span);
        }
        
        return new CollectionAggregation(span, type, collection, property, where);
    }
    
    /**
     * Parse a collection filter chain.
     * 
     * Syntax:
     *   collection where condition
     *   collection where condition1 where condition2
     */
    public CollectionFilter parseCollectionFilter(SourceSpan span, Expr collection) {
        List<NaturalLanguageCondition> conditions = new ArrayList<>();
        
        while (match(TokenType.WHERE)) {
            NaturalLanguageCondition condition = parseWhereConditionExtended(span);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        
        String ofTheirProperty = null;
        if (match(TokenType.OF)) {
            if (peek().getLexeme().equalsIgnoreCase("their")) {
                advance();
                ofTheirProperty = parsePropertyPath();
            }
        }
        
        return new CollectionFilter(span, collection, conditions, ofTheirProperty);
    }
    
    /**
     * Parse a collection flatten expression.
     * 
     * Syntax:
     *   all property across collection
     *   all property from collection
     */
    public CollectionFlatten parseCollectionFlatten(SourceSpan span) {
        if (!match(TokenType.ALL)) {
            return null;
        }
        
        String property = parsePropertyPath();
        
        CollectionFlatten.FlattenType type;
        if (match(TokenType.ACROSS)) {
            type = CollectionFlatten.FlattenType.ACROSS;
        } else if (match(TokenType.FROM)) {
            type = CollectionFlatten.FlattenType.FROM;
        } else {
            return null;
        }
        
        Expr collection = parseSimpleIdentifierExpr(span);
        
        NaturalLanguageCondition where = null;
        if (match(TokenType.WHERE)) {
            where = parseWhereConditionExtended(span);
        }
        
        return new CollectionFlatten(span, property, collection, where, type);
    }
    
    /**
     * Parse a collection group by expression.
     * 
     * Syntax:
     *   collection grouped by property
     *   count of collection grouped by property
     */
    public CollectionGroupBy parseCollectionGroupBy(SourceSpan span, Expr collection) {
        if (!matchSequence(TokenType.GROUPED, TokenType.BY)) {
            return null;
        }
        
        String groupByProperty = parsePropertyPath();
        
        return CollectionGroupBy.simple(span, collection, groupByProperty);
    }
    
    /**
     * Parse a specification condition.
     * 
     * Syntax:
     *   expression satisfies SpecificationRef
     *   expression does not satisfy SpecificationRef
     *   expression matches SpecificationRef
     *   expression is eligible for SpecificationRef
     */
    public SpecificationCondition parseSpecificationCondition(SourceSpan span, Expr subject) {
        if (match(TokenType.SATISFIES)) {
            SpecificationCondition.SpecificationRef spec = parseSpecificationRef();
            return SpecificationCondition.satisfies(span, subject, spec);
        }
        
        if (matchSequence(TokenType.DOES, TokenType.NOT, TokenType.SATISFIES)) {
            SpecificationCondition.SpecificationRef spec = parseSpecificationRef();
            return SpecificationCondition.doesNotSatisfy(span, subject, spec);
        }
        
        if (match(TokenType.MATCHES)) {
            SpecificationCondition.SpecificationRef spec = parseSpecificationRef();
            return SpecificationCondition.matches(span, subject, spec);
        }
        
        if (matchSequence(TokenType.IS, TokenType.ELIGIBLE, TokenType.FOR)) {
            SpecificationCondition.SpecificationRef spec = parseSpecificationRef();
            return SpecificationCondition.isEligibleFor(span, subject, spec);
        }
        
        return null;
    }
    
    private SpecificationCondition.SpecificationRef parseSpecificationRef() {
        if (match(TokenType.NOT)) {
            SpecificationCondition.SpecificationRef inner = parseSpecificationRef();
            return new SpecificationCondition.SpecificationRef.Negation(inner);
        }
        
        String name = advance().getLexeme();
        
        // Check for parameterized spec
        List<Expr> args = new ArrayList<>();
        if (match(TokenType.LEFT_PAREN)) {
            // Parse arguments
            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    if (check(TokenType.COMMA)) advance();
                    args.add(expression());
                } while (check(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_PAREN, "Expected ')' after spec arguments");
        }
        
        SpecificationCondition.SpecificationRef ref = args.isEmpty()
            ? new SpecificationCondition.SpecificationRef.Simple(name)
            : new SpecificationCondition.SpecificationRef.Parameterized(name, args);
        
        // Check for composite
        if (match(TokenType.AND)) {
            SpecificationCondition.SpecificationRef right = parseSpecificationRef();
            return new SpecificationCondition.SpecificationRef.Composite(
                ref, 
                SpecificationCondition.SpecificationRef.Composite.CompositeType.AND, 
                right
            );
        }
        if (match(TokenType.OR)) {
            SpecificationCondition.SpecificationRef right = parseSpecificationRef();
            return new SpecificationCondition.SpecificationRef.Composite(
                ref, 
                SpecificationCondition.SpecificationRef.Composite.CompositeType.OR, 
                right
            );
        }
        
        return ref;
    }
    
    /**
     * Parse a match expression.
     * 
     * Syntax:
     *   match expression
     *     value1: result1
     *     value2: result2
     *     default: defaultResult
     */
    public MatchExpr parseMatchExpression(SourceSpan span) {
        if (!match(TokenType.MATCH)) {
            return null;
        }
        
        Expr subject = parseSimpleIdentifierExpr(span);
        List<MatchExpr.MatchCase> cases = new ArrayList<>();
        MatchExpr.MatchCase defaultCase = null;
        
        // Parse cases until we see something that isn't a case
        while (check(TokenType.DASH) || check(TokenType.IDENTIFIER) || check(TokenType.DEFAULT)) {
            match(TokenType.DASH); // Optional list marker
            
            MatchExpr.CasePattern pattern;
            if (match(TokenType.DEFAULT)) {
                pattern = new MatchExpr.CasePattern.DefaultPattern();
            } else if (match(TokenType.NULL)) {
                pattern = new MatchExpr.CasePattern.NullValue();
            } else {
                String value = advance().getLexeme();
                pattern = MatchExpr.CasePattern.SingleValue.of(value);
            }
            
            consume(TokenType.COLON, "Expected ':' after match pattern");
            
            // Parse result expression
            Expr result = expression();
            
            MatchExpr.MatchCase matchCase = new MatchExpr.MatchCase(
                span, 
                pattern, 
                null, // No guard
                new MatchExpr.MatchCaseBody.ExpressionBody(result)
            );
            
            if (pattern instanceof MatchExpr.CasePattern.DefaultPattern) {
                defaultCase = matchCase;
            } else {
                cases.add(matchCase);
            }
        }
        
        return new MatchExpr(span, subject, cases, defaultCase);
    }
    
    // Extended expression helper methods
    
    private NaturalLanguageCondition parseWhereConditionExtended(SourceSpan span) {
        StringBuilder rawText = new StringBuilder();
        List<Token> conditionTokens = new ArrayList<>();
        
        while (!check(TokenType.WHERE) && !check(TokenType.GROUPED) && 
               !check(TokenType.NEWLINE) && !isAtEnd()) {
            Token token = advance();
            rawText.append(token.getLexeme()).append(" ");
            conditionTokens.add(token);
        }
        
        return NaturalLanguageCondition.simple(span, rawText.toString().trim(), null);
    }
    
    private Expr parseSimpleIdentifierExpr(SourceSpan span) {
        if (check(TokenType.IDENTIFIER)) {
            Token token = advance();
            return new VariableExpr(span, token.getLexeme());
        }
        return new NullExpr(span);
    }
    
    private String parsePropertyPath() {
        StringBuilder path = new StringBuilder();
        if (check(TokenType.IDENTIFIER)) {
            path.append(advance().getLexeme());
            while (match(TokenType.DOT) && check(TokenType.IDENTIFIER)) {
                path.append(".").append(advance().getLexeme());
            }
        }
        return path.toString();
    }
    
    private String parseStringLiteralValue() {
        if (check(TokenType.STRING_LITERAL)) {
            String literal = advance().getLexeme();
            // Remove quotes
            if (literal.length() >= 2) {
                return literal.substring(1, literal.length() - 1);
            }
            return literal;
        }
        return "";
    }
    
    private int parseIntegerLiteralValue() {
        if (check(TokenType.INTEGER_LITERAL)) {
            return Integer.parseInt(advance().getLexeme());
        }
        return 0;
    }
    
    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }
    
    private boolean matchSequence(TokenType... types) {
        int savedPosition = current;
        for (TokenType type : types) {
            if (!check(type)) {
                current = savedPosition;
                return false;
            }
            advance();
        }
        return true;
    }
    
    // ========== Helper Methods ==========
    
    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }
    
    private Token peek() {
        return tokens.get(current);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }
    
    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).getType() == type;
    }
    
    /**
     * Check if the next token (current + 1) is a valid type start.
     * Used by parameterList() to disambiguate ':' as type annotation vs. behavior header colon.
     */
    private boolean checkNextIsTypeStart() {
        if (current + 1 >= tokens.size()) return false;
        TokenType next = tokens.get(current + 1).getType();
        return next == TokenType.IDENTIFIER ||
               next == TokenType.STRING_TYPE || next == TokenType.INT_TYPE ||
               next == TokenType.DECIMAL_TYPE || next == TokenType.BOOLEAN_TYPE ||
               next == TokenType.DATETIME_TYPE || next == TokenType.UUID_TYPE ||
               next == TokenType.LIST_TYPE || next == TokenType.SET_TYPE ||
               next == TokenType.MAP_TYPE || next == TokenType.VOID_TYPE ||
               next == TokenType.RESULT;
    }
    
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        
        error(message + ", got '" + peek().getLexeme() + "'");
        return null;
    }
    
    private void error(String message) {
        errors.add(new ParseError(message, currentSpan()));
    }
    
    private SourceSpan currentSpan() {
        Token token = peek();
        return SourceSpan.at(sourceName, token.getLine(), token.getColumn());
    }
    
    private SourceLocation currentLocation() {
        Token token = peek();
        return new SourceLocation(sourceName, token.getLine(), token.getColumn());
    }
    
    /**
     * Skip tokens until we hit a clause keyword or end of block.
     */
    private void skipUntilClauseOrEnd() {
        while (!isAtEnd() && 
               !check(TokenType.REQUIRE) && !check(TokenType.GIVEN) && 
               !check(TokenType.THEN) && !check(TokenType.RETURN) &&
               !check(TokenType.WHEN) && !check(TokenType.RIGHT_BRACE)) {
            advance();
        }
    }
    
    /**
     * Skip a block (balanced braces).
     */
    private void skipBlock() {
        int braceCount = 1;
        while (braceCount > 0 && !isAtEnd()) {
            if (check(TokenType.LEFT_BRACE)) braceCount++;
            if (check(TokenType.RIGHT_BRACE)) braceCount--;
            advance();
        }
    }
    
    /**
     * Source location record for error reporting.
     * Local definition to avoid dependency on deleted core package.
     */
    public record SourceLocation(String sourceFile, int line, int column) {
        public SourceSpan toSpan() {
            return new SourceSpan(sourceFile, line, column, line, column);
        }
    }
    
    /**
     * Represents a parse error.
     */
    public static class ParseError {
        private final String message;
        private final SourceSpan span;
        
        public ParseError(String message, SourceSpan span) {
            this.message = message;
            this.span = span;
        }
        
        public ParseError(String message, SourceLocation location) {
            this.message = message;
            this.span = location.toSpan();
        }
        
        public String getMessage() {
            return message;
        }
        
        public String message() {
            return message;
        }
        
        public SourceSpan getSpan() {
            return span;
        }
        
        public SourceSpan location() {
            return span;
        }
        
        public int line() {
            return span != null ? span.startLine() : 0;
        }
        
        public int column() {
            return span != null ? span.startColumn() : 0;
        }
        
        public SourceLocation getLocation() {
            return new SourceLocation(span.fileName(), span.startLine(), span.startColumn());
        }
        
        @Override
        public String toString() {
            return String.format("[%s:%d:%d] Parse error: %s",
                span.fileName(), span.startLine(), span.startColumn(), message);
        }

    }
}
