package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.DdslParser.ParseError;
import uet.ndh.ddsl.parser.ParseException;
import uet.ndh.ddsl.parser.lexer.Scanner;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic provider for DDSL Language Server.
 * 
 * Provides error and warning diagnostics by:
 * 1. Checking for lexical errors (invalid tokens)
 * 2. Checking for parse errors (syntax errors)
 * 3. Running semantic validation (type checking, reference resolution)
 * 4. Applying DDD best practice rules
 */
public class DdslDiagnosticProvider {
    
    /** Diagnostic source identifier */
    private static final String DIAGNOSTIC_SOURCE = "ddsl";
    
    /**
     * Get diagnostics for a document.
     * 
     * @param content the document content
     * @return list of diagnostics
     */
    public List<Diagnostic> getDiagnostics(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        // Phase 1: Lexical analysis
        diagnostics.addAll(getLexicalDiagnostics(content));
        
        // Phase 2: Syntactic analysis (if no lexical errors)
        if (diagnostics.stream().noneMatch(d -> d.getSeverity() == DiagnosticSeverity.Error)) {
            diagnostics.addAll(getSyntacticDiagnostics(content));
        }
        
        // Phase 3: Semantic analysis (would require full AST)
        // diagnostics.addAll(getSemanticDiagnostics(content));
        
        // Phase 4: DDD best practices (warnings)
        diagnostics.addAll(getDddBestPracticeDiagnostics(content));
        
        return diagnostics;
    }
    
    /**
     * Get lexical (scanning) diagnostics.
     */
    private List<Diagnostic> getLexicalDiagnostics(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        Scanner scanner = new Scanner(content);
        List<Token> tokens = scanner.scanTokens();
        
        // Check for error tokens
        for (Token token : tokens) {
            if (token.getType() == TokenType.ERROR) {
                Diagnostic diagnostic = new Diagnostic();
                diagnostic.setRange(tokenToRange(token));
                diagnostic.setSeverity(DiagnosticSeverity.Error);
                diagnostic.setSource(DIAGNOSTIC_SOURCE);
                diagnostic.setMessage("Unexpected character: " + token.getLexeme());
                diagnostic.setCode("lexical-error");
                diagnostics.add(diagnostic);
            }
        }
        
        // Check for scanner errors
        if (scanner.hasErrors()) {
            for (Scanner.LexicalError error : scanner.getErrors()) {
                Diagnostic diagnostic = new Diagnostic();
                diagnostic.setRange(new Range(
                    new Position(error.getLine() - 1, error.getColumn() - 1),
                    new Position(error.getLine() - 1, error.getColumn())
                ));
                diagnostic.setSeverity(DiagnosticSeverity.Error);
                diagnostic.setSource(DIAGNOSTIC_SOURCE);
                diagnostic.setMessage(error.getMessage());
                diagnostic.setCode("lexical-error");
                diagnostics.add(diagnostic);
            }
        }
        
        return diagnostics;
    }
    
    /**
     * Get syntactic (parsing) diagnostics.
     */
    private List<Diagnostic> getSyntacticDiagnostics(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        try {
            DdslParser parser = new DdslParser(content, "document.ddsl");
            parser.parse();
            
            // Check for parse errors
            if (parser.hasErrors()) {
                for (ParseError error : parser.getErrors()) {
                    Diagnostic diagnostic = new Diagnostic();
                    
                    if (error.location() != null) {
                        diagnostic.setRange(new Range(
                            new Position(error.location().startLine() - 1, error.location().startColumn() - 1),
                            new Position(error.location().startLine() - 1, error.location().startColumn())
                        ));
                    } else {
                        diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));
                    }
                    
                    diagnostic.setSeverity(DiagnosticSeverity.Error);
                    diagnostic.setSource(DIAGNOSTIC_SOURCE);
                    diagnostic.setMessage(error.message());
                    diagnostic.setCode("syntax-error");
                    diagnostics.add(diagnostic);
                }
            }
        } catch (ParseException e) {
            // Add parse exception as diagnostic
            Diagnostic diagnostic = new Diagnostic();
            
            if (e.getLocation() != null) {
                diagnostic.setRange(new Range(
                    new Position(e.getLocation().startLine() - 1, e.getLocation().startColumn() - 1),
                    new Position(e.getLocation().startLine() - 1, e.getLocation().startColumn())
                ));
            } else {
                diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));
            }
            
            diagnostic.setSeverity(DiagnosticSeverity.Error);
            diagnostic.setSource(DIAGNOSTIC_SOURCE);
            diagnostic.setMessage(e.getMessage());
            diagnostic.setCode("parse-error");
            diagnostics.add(diagnostic);
            
            // Add individual errors if available
            if (e.getErrors() != null) {
                for (ParseError error : e.getErrors()) {
                    Diagnostic errorDiag = new Diagnostic();
                    
                    if (error.location() != null) {
                        errorDiag.setRange(new Range(
                            new Position(error.location().startLine() - 1, error.location().startColumn() - 1),
                            new Position(error.location().startLine() - 1, error.location().startColumn())
                        ));
                    } else {
                        errorDiag.setRange(new Range(new Position(0, 0), new Position(0, 1)));
                    }
                    
                    errorDiag.setSeverity(DiagnosticSeverity.Error);
                    errorDiag.setSource(DIAGNOSTIC_SOURCE);
                    errorDiag.setMessage(error.message());
                    errorDiag.setCode("parse-error");
                    diagnostics.add(errorDiag);
                }
            }
        }
        
        return diagnostics;
    }
    
    /**
     * Get DDD best practice diagnostics (warnings and hints).
     */
    private List<Diagnostic> getDddBestPracticeDiagnostics(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        Scanner scanner = new Scanner(content);
        List<Token> tokens = scanner.scanTokens();
        
        // Track context for analysis
        boolean inAggregate = false;
        boolean hasIdentity = false;
        Token aggregateToken = null;
        
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            TokenType type = token.getType();
            
            // Check for aggregate without identity
            if (type == TokenType.AGGREGATE) {
                // If we were in an aggregate without identity, report warning
                if (inAggregate && !hasIdentity && aggregateToken != null) {
                    diagnostics.add(createWarning(
                        aggregateToken,
                        "Aggregate root should have an @identity field",
                        "ddd-aggregate-identity"
                    ));
                }
                
                inAggregate = true;
                hasIdentity = false;
                aggregateToken = token;
            } else if (type == TokenType.ENTITY && inAggregate) {
                // Entity inside aggregate - check for identity
                inAggregate = false; // Reset for nested entity tracking
            } else if (type == TokenType.IDENTITY) {
                hasIdentity = true;
            } else if (type == TokenType.RIGHT_BRACE) {
                // End of block - check aggregate
                if (inAggregate && !hasIdentity && aggregateToken != null) {
                    diagnostics.add(createWarning(
                        aggregateToken,
                        "Aggregate root should have an @identity field",
                        "ddd-aggregate-identity"
                    ));
                }
                inAggregate = false;
                hasIdentity = false;
                aggregateToken = null;
            }
            
            // Check for value object with @identity (anti-pattern)
            if (type == TokenType.VALUE_OBJECT) {
                // Look ahead for @identity
                for (int j = i + 1; j < tokens.size() && j < i + 50; j++) {
                    Token lookAhead = tokens.get(j);
                    if (lookAhead.getType() == TokenType.RIGHT_BRACE) {
                        break;
                    }
                    if (lookAhead.getType() == TokenType.IDENTITY) {
                        diagnostics.add(createWarning(
                            lookAhead,
                            "Value objects should not have identity fields. Consider using Entity instead.",
                            "ddd-value-object-identity"
                        ));
                    }
                }
            }
            
            // Check for domain service with state (look for fields)
            if (type == TokenType.DOMAIN_SERVICE) {
                Token serviceToken = token;
                boolean hasFields = false;
                int braceCount = 0;
                
                for (int j = i + 1; j < tokens.size(); j++) {
                    Token lookAhead = tokens.get(j);
                    if (lookAhead.getType() == TokenType.LEFT_BRACE) {
                        braceCount++;
                    } else if (lookAhead.getType() == TokenType.RIGHT_BRACE) {
                        braceCount--;
                        if (braceCount == 0) break;
                    } else if (lookAhead.getType() == TokenType.COLON && braceCount == 1) {
                        // Field declaration
                        hasFields = true;
                    } else if (lookAhead.getType() == TokenType.OPERATIONS) {
                        // Operations block is fine
                        hasFields = false;
                        break;
                    }
                }
                
                if (hasFields) {
                    diagnostics.add(createHint(
                        serviceToken,
                        "Domain services should be stateless. Consider moving state to entities.",
                        "ddd-service-stateless"
                    ));
                }
            }
            
            // Check for large aggregate (too many fields)
            if (type == TokenType.AGGREGATE) {
                int fieldCount = 0;
                int braceCount = 0;
                
                for (int j = i + 1; j < tokens.size(); j++) {
                    Token lookAhead = tokens.get(j);
                    if (lookAhead.getType() == TokenType.LEFT_BRACE) {
                        braceCount++;
                    } else if (lookAhead.getType() == TokenType.RIGHT_BRACE) {
                        braceCount--;
                        if (braceCount == 0) break;
                    } else if (lookAhead.getType() == TokenType.COLON && braceCount == 1) {
                        fieldCount++;
                    }
                }
                
                if (fieldCount > 10) {
                    diagnostics.add(createWarning(
                        token,
                        "Aggregate has " + fieldCount + " fields. Consider splitting into smaller aggregates.",
                        "ddd-aggregate-size"
                    ));
                }
            }
        }
        
        return diagnostics;
    }
    
    // ========== Helper Methods ==========
    
    private Range tokenToRange(Token token) {
        int line = token.getLine() - 1; // Convert to 0-based
        int startCol = token.getColumn() - 1;
        int endCol = startCol + token.getLexeme().length();
        
        return new Range(
            new Position(line, startCol),
            new Position(line, endCol)
        );
    }
    
    private Diagnostic createWarning(Token token, String message, String code) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(tokenToRange(token));
        diagnostic.setSeverity(DiagnosticSeverity.Warning);
        diagnostic.setSource(DIAGNOSTIC_SOURCE);
        diagnostic.setMessage(message);
        diagnostic.setCode(code);
        return diagnostic;
    }
    
    private Diagnostic createHint(Token token, String message, String code) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(tokenToRange(token));
        diagnostic.setSeverity(DiagnosticSeverity.Hint);
        diagnostic.setSource(DIAGNOSTIC_SOURCE);
        diagnostic.setMessage(message);
        diagnostic.setCode(code);
        return diagnostic;
    }
}
