package uet.ndh.ddsl.lsp;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.ndh.ddsl.parser.lexer.Scanner;
import uet.ndh.ddsl.parser.lexer.Token;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Text Document Service for DDSL Language Server.
 * 
 * Handles all document-related LSP requests:
 * - Document synchronization (open/change/save/close)
 * - Semantic tokens (syntax highlighting)
 * - Completion (auto-complete)
 * - Hover (documentation on hover)
 * - Definition (go-to-definition)
 * - References (find all references)
 * - Diagnostics (errors/warnings)
 */
@Slf4j
public class DdslTextDocumentService implements TextDocumentService {

    /** Reference to the parent language server */
    private final DdslLanguageServer server;
    
    /** The language client for sending notifications */
    private LanguageClient client;
    
    /** Cache of open documents: URI -> content */
    private final Map<String, TextDocumentItem> documents = new ConcurrentHashMap<>();
    
    /** Cache of parsed tokens per document: URI -> tokens */
    private final Map<String, List<Token>> tokenCache = new ConcurrentHashMap<>();
    
    /** Providers */
    private final DdslCompletionProvider completionProvider;
    private final DdslHoverProvider hoverProvider;
    private final DdslDiagnosticProvider diagnosticProvider;
    private final DdslDefinitionProvider definitionProvider;
    
    public DdslTextDocumentService(DdslLanguageServer server) {
        this.server = server;
        this.completionProvider = new DdslCompletionProvider();
        this.hoverProvider = new DdslHoverProvider();
        this.diagnosticProvider = new DdslDiagnosticProvider();
        this.definitionProvider = new DdslDefinitionProvider();
    }
    
    public void setClient(LanguageClient client) {
        this.client = client;
    }
    
    // ========== Document Synchronization ==========
    
    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem document = params.getTextDocument();
        String uri = document.getUri();
        log.info("Document opened: {}", uri);
        
        documents.put(uri, document);
        
        // Parse and cache tokens
        parseAndCacheDocument(uri, document.getText());
        
        // Publish diagnostics
        publishDiagnostics(uri, document.getText());
    }
    
    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        
        // With Full sync, we get the entire document content
        List<TextDocumentContentChangeEvent> changes = params.getContentChanges();
        if (!changes.isEmpty()) {
            String newContent = changes.get(0).getText();
            
            // Update document cache
            TextDocumentItem existing = documents.get(uri);
            if (existing != null) {
                existing = new TextDocumentItem(
                    uri,
                    existing.getLanguageId(),
                    params.getTextDocument().getVersion(),
                    newContent
                );
                documents.put(uri, existing);
            }
            
            // Re-parse and update caches
            parseAndCacheDocument(uri, newContent);
            
            // Publish updated diagnostics
            publishDiagnostics(uri, newContent);
        }
    }
    
    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        log.info("Document closed: {}", uri);
        
        documents.remove(uri);
        tokenCache.remove(uri);
        
        // Clear diagnostics for closed document
        server.publishDiagnostics(uri, Collections.emptyList());
    }
    
    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        log.info("Document saved: {}", uri);
        
        // If text is included, use it; otherwise use cached
        String text = params.getText();
        if (text != null) {
            parseAndCacheDocument(uri, text);
            publishDiagnostics(uri, text);
        }
    }
    
    // ========== Semantic Tokens ==========
    
    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        String uri = params.getTextDocument().getUri();
        log.debug("Semantic tokens requested for: {}", uri);
        
        return CompletableFuture.supplyAsync(() -> {
            List<Token> tokens = tokenCache.get(uri);
            if (tokens == null) {
                TextDocumentItem doc = documents.get(uri);
                if (doc != null) {
                    tokens = parseDocument(doc.getText());
                    tokenCache.put(uri, tokens);
                } else {
                    return new SemanticTokens(Collections.emptyList());
                }
            }
            
            return DdslSemanticTokens.encode(tokens);
        });
    }
    
    // ========== Completion ==========
    
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            CompletionParams params) {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();
        log.debug("Completion requested at {}:{}:{}", uri, position.getLine(), position.getCharacter());
        
        return CompletableFuture.supplyAsync(() -> {
            TextDocumentItem doc = documents.get(uri);
            List<Token> tokens = tokenCache.get(uri);
            
            String content = doc != null ? doc.getText() : "";
            List<CompletionItem> items = completionProvider.getCompletions(
                content, tokens, position, params.getContext()
            );
            
            return Either.forRight(new CompletionList(false, items));
        });
    }
    
    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem item) {
        // Resolve additional details for the completion item
        return CompletableFuture.supplyAsync(() -> 
            completionProvider.resolveCompletionItem(item)
        );
    }
    
    // ========== Hover ==========
    
    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();
        log.debug("Hover requested at {}:{}:{}", uri, position.getLine(), position.getCharacter());
        
        return CompletableFuture.supplyAsync(() -> {
            TextDocumentItem doc = documents.get(uri);
            List<Token> tokens = tokenCache.get(uri);
            
            if (doc == null || tokens == null) {
                return null;
            }
            
            return hoverProvider.getHover(doc.getText(), tokens, position);
        });
    }
    
    // ========== Definition ==========
    
    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
            DefinitionParams params) {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();
        log.debug("Definition requested at {}:{}:{}", uri, position.getLine(), position.getCharacter());
        
        return CompletableFuture.supplyAsync(() -> {
            TextDocumentItem doc = documents.get(uri);
            List<Token> tokens = tokenCache.get(uri);
            
            if (doc == null || tokens == null) {
                return Either.forLeft(Collections.emptyList());
            }
            
            List<Location> locations = definitionProvider.getDefinition(
                uri, doc.getText(), tokens, position
            );
            return Either.forLeft(locations);
        });
    }
    
    // ========== References ==========
    
    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();
        
        return CompletableFuture.supplyAsync(() -> {
            TextDocumentItem doc = documents.get(uri);
            List<Token> tokens = tokenCache.get(uri);
            
            if (doc == null || tokens == null) {
                return Collections.emptyList();
            }
            
            return definitionProvider.getReferences(
                uri, doc.getText(), tokens, position, params.getContext().isIncludeDeclaration()
            );
        });
    }
    
    // ========== Document Symbols ==========
    
    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
            DocumentSymbolParams params) {
        String uri = params.getTextDocument().getUri();
        
        return CompletableFuture.supplyAsync(() -> {
            TextDocumentItem doc = documents.get(uri);
            List<Token> tokens = tokenCache.get(uri);
            
            if (doc == null || tokens == null) {
                return Collections.emptyList();
            }
            
            return DdslDocumentSymbolProvider.getSymbols(doc.getText(), tokens);
        });
    }
    
    // ========== Formatting ==========
    
    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        String uri = params.getTextDocument().getUri();
        
        return CompletableFuture.supplyAsync(() -> {
            TextDocumentItem doc = documents.get(uri);
            if (doc == null) {
                return Collections.emptyList();
            }
            
            return DdslFormatter.format(doc.getText(), params.getOptions());
        });
    }
    
    // ========== Signature Help ==========
    
    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();
        
        return CompletableFuture.supplyAsync(() -> {
            TextDocumentItem doc = documents.get(uri);
            List<Token> tokens = tokenCache.get(uri);
            
            if (doc == null || tokens == null) {
                return null;
            }
            
            return DdslSignatureHelpProvider.getSignatureHelp(doc.getText(), tokens, position);
        });
    }
    
    // ========== Code Actions ==========
    
    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        String uri = params.getTextDocument().getUri();
        
        return CompletableFuture.supplyAsync(() -> {
            TextDocumentItem doc = documents.get(uri);
            if (doc == null) {
                return Collections.emptyList();
            }
            
            return DdslCodeActionProvider.getCodeActions(
                uri, doc.getText(), params.getRange(), params.getContext()
            );
        });
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Parse document and cache tokens.
     */
    private void parseAndCacheDocument(String uri, String content) {
        List<Token> tokens = parseDocument(content);
        tokenCache.put(uri, tokens);
    }
    
    /**
     * Parse document content into tokens using the DDSL Scanner.
     */
    private List<Token> parseDocument(String content) {
        Scanner scanner = new Scanner(content);
        return scanner.scanTokens();
    }
    
    /**
     * Publish diagnostics for a document.
     */
    private void publishDiagnostics(String uri, String content) {
        List<Diagnostic> diagnostics = diagnosticProvider.getDiagnostics(content);
        server.publishDiagnostics(uri, diagnostics);
    }
    
    /**
     * Get cached document content.
     */
    public String getDocumentContent(String uri) {
        TextDocumentItem doc = documents.get(uri);
        return doc != null ? doc.getText() : null;
    }
    
    /**
     * Get cached tokens for a document.
     */
    public List<Token> getDocumentTokens(String uri) {
        return tokenCache.get(uri);
    }
}
