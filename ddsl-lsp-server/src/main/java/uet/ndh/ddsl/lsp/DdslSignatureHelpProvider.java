package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.*;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.List;

/**
 * Signature Help provider for DDSL Language Server.
 * 
 * Provides function/method signature information when typing:
 * - Factory creation methods
 * - Operation parameters
 * - Repository methods
 */
public class DdslSignatureHelpProvider {
    
    /**
     * Get signature help for the current position.
     */
    public static SignatureHelp getSignatureHelp(String content, List<Token> tokens, Position position) {
        // Find what context we're in
        SignatureContext ctx = findSignatureContext(tokens, position);
        
        if (ctx == null) {
            return null;
        }
        
        SignatureHelp help = new SignatureHelp();
        
        switch (ctx.type) {
            case FACTORY_CREATING -> {
                // Factory creation signature
                SignatureInformation sig = new SignatureInformation();
                sig.setLabel("creating " + ctx.typeName + " from parameters...");
                sig.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN,
                    "Create a new instance of **" + ctx.typeName + "** using the factory."));
                
                // Add parameters (would need semantic analysis for actual params)
                sig.setParameters(List.of(
                    createParameter("params", "Parameters for creating the instance")
                ));
                
                help.setSignatures(List.of(sig));
                help.setActiveSignature(0);
                help.setActiveParameter(ctx.parameterIndex);
            }
            
            case WHEN_CLAUSE -> {
                // Behavior trigger signature
                SignatureInformation sig = new SignatureInformation();
                sig.setLabel("when action with parameters { ... }");
                sig.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN,
                    "Define a behavior that triggers **when** an action occurs.\n\n" +
                    "Parameters become available in the behavior body."));
                
                sig.setParameters(List.of(
                    createParameter("action", "The action that triggers this behavior"),
                    createParameter("parameters", "Input parameters for the behavior")
                ));
                
                help.setSignatures(List.of(sig));
                help.setActiveSignature(0);
                help.setActiveParameter(ctx.parameterIndex);
            }
            
            case REPOSITORY_METHOD -> {
                // Repository method signature
                SignatureInformation sig = new SignatureInformation();
                sig.setLabel(ctx.methodName + "(parameters): ReturnType");
                sig.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN,
                    "Repository method **" + ctx.methodName + "**"));
                
                // Standard repository method signatures
                if (ctx.methodName.startsWith("findBy")) {
                    sig.setParameters(List.of(
                        createParameter("criteria", "Search criteria")
                    ));
                } else if (ctx.methodName.equals("save")) {
                    sig.setParameters(List.of(
                        createParameter("entity", "The entity to save")
                    ));
                } else if (ctx.methodName.equals("delete")) {
                    sig.setParameters(List.of(
                        createParameter("entity", "The entity to delete")
                    ));
                }
                
                help.setSignatures(List.of(sig));
                help.setActiveSignature(0);
                help.setActiveParameter(ctx.parameterIndex);
            }
            
            case CREATE_STATEMENT -> {
                // Create statement signature
                SignatureInformation sig = new SignatureInformation();
                sig.setLabel("create Type with parameters");
                sig.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN,
                    "Create a new instance of a type with the specified parameters."));
                
                sig.setParameters(List.of(
                    createParameter("Type", "The type to create"),
                    createParameter("parameters", "Construction parameters")
                ));
                
                help.setSignatures(List.of(sig));
                help.setActiveSignature(0);
                help.setActiveParameter(ctx.parameterIndex);
            }
            
            default -> {
                return null;
            }
        }
        
        return help;
    }
    
    /**
     * Find the signature context at the given position.
     */
    private static SignatureContext findSignatureContext(List<Token> tokens, Position position) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        
        int line = position.getLine() + 1;
        int column = position.getCharacter() + 1;
        
        // Find the token at or before the position
        int tokenIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getLine() > line || 
                (token.getLine() == line && token.getColumn() > column)) {
                break;
            }
            tokenIndex = i;
        }
        
        if (tokenIndex < 0) {
            return null;
        }
        
        // Look backwards to find context
        int parenDepth = 0;
        int parameterIndex = 0;
        
        for (int i = tokenIndex; i >= 0; i--) {
            Token token = tokens.get(i);
            TokenType type = token.getType();
            
            if (type == TokenType.RIGHT_PAREN) {
                parenDepth++;
            } else if (type == TokenType.LEFT_PAREN) {
                if (parenDepth > 0) {
                    parenDepth--;
                } else {
                    // Found the opening paren
                    // Look at what precedes it
                    if (i > 0) {
                        Token prevToken = tokens.get(i - 1);
                        
                        if (prevToken.getType() == TokenType.IDENTIFIER) {
                            // Method call
                            SignatureContext ctx = new SignatureContext();
                            ctx.type = SignatureContextType.REPOSITORY_METHOD;
                            ctx.methodName = prevToken.getLexeme();
                            ctx.parameterIndex = parameterIndex;
                            return ctx;
                        }
                    }
                }
            } else if (type == TokenType.COMMA && parenDepth == 0) {
                parameterIndex++;
            } else if (type == TokenType.CREATING) {
                SignatureContext ctx = new SignatureContext();
                ctx.type = SignatureContextType.FACTORY_CREATING;
                ctx.parameterIndex = parameterIndex;
                
                // Get the type name
                if (i + 1 < tokens.size()) {
                    Token nextToken = tokens.get(i + 1);
                    if (nextToken.getType() == TokenType.IDENTIFIER) {
                        ctx.typeName = nextToken.getLexeme();
                    }
                }
                return ctx;
            } else if (type == TokenType.WHEN) {
                SignatureContext ctx = new SignatureContext();
                ctx.type = SignatureContextType.WHEN_CLAUSE;
                ctx.parameterIndex = parameterIndex;
                return ctx;
            } else if (type == TokenType.CREATE) {
                SignatureContext ctx = new SignatureContext();
                ctx.type = SignatureContextType.CREATE_STATEMENT;
                ctx.parameterIndex = parameterIndex;
                return ctx;
            }
        }
        
        return null;
    }
    
    /**
     * Create a parameter information object.
     */
    private static ParameterInformation createParameter(String name, String description) {
        ParameterInformation param = new ParameterInformation();
        param.setLabel(name);
        param.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN, description));
        return param;
    }
    
    // ========== Inner Classes ==========
    
    private static class SignatureContext {
        SignatureContextType type;
        String typeName;
        String methodName;
        int parameterIndex;
    }
    
    private enum SignatureContextType {
        FACTORY_CREATING,
        WHEN_CLAUSE,
        REPOSITORY_METHOD,
        CREATE_STATEMENT
    }
}
