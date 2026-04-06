package uet.ndh.ddsl.lsp;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import uet.ndh.ddsl.lsp.core.DdslCommandIds;
import uet.ndh.ddsl.lsp.core.ExitHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main Language Server implementation for DDSL.
 * 
 * This class implements the LSP4J LanguageServer interface and coordinates
 * all language server functionality including:
 * - Document synchronization
 * - Semantic tokens (syntax highlighting)
 * - Auto-completion
 * - Hover information
 * - Diagnostics (errors/warnings)
 * - Go-to-definition
 * 
 * Architecture Decision: Using LSP4J because:
 * 1. It's the standard Java implementation of LSP
 * 2. It handles JSON-RPC serialization/deserialization
 * 3. It provides type-safe interfaces for all LSP features
 * 4. It's actively maintained by Eclipse Foundation
 */
@Slf4j
public class DdslLanguageServer implements LanguageServer, LanguageClientAware {

    private final ExitHandler exitHandler;

    /** The client proxy for sending notifications/requests to the editor */
    private LanguageClient client;
    
    /** The text document service handling document operations */
    private final DdslTextDocumentService textDocumentService;
    
    /** The workspace service handling workspace operations */
    private final DdslWorkspaceService workspaceService;
    
    /** Server capabilities that will be sent to the client */
    private ServerCapabilities serverCapabilities;
    
    /** Shutdown request received flag */
    private boolean shutdownReceived = false;
    
    public DdslLanguageServer() {
        this(ExitHandler.noOp());
    }

    public DdslLanguageServer(ExitHandler exitHandler) {
        this.exitHandler = exitHandler;
        this.textDocumentService = new DdslTextDocumentService(this);
        this.workspaceService = new DdslWorkspaceService(this);
    }
    
    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        this.textDocumentService.setClient(client);
        log.info("DDSL Language Server connected to client");
    }
    
    /**
     * Initialize the language server with client capabilities.
     * 
     * This is the first request sent by the client and we respond with
     * our server capabilities telling the client what features we support.
     */
    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        log.info("Initializing DDSL Language Server...");
        log.info("Client info: {}", params.getClientInfo());
        log.info("Workspace folders: {}", params.getWorkspaceFolders());
        
        // Build server capabilities
        serverCapabilities = new ServerCapabilities();
        
        // Document sync: Full sync is simpler and sufficient for our DSL
        // Incremental sync would be more efficient for large files but adds complexity
        TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setOpenClose(true);
        syncOptions.setChange(TextDocumentSyncKind.Full);
        syncOptions.setSave(new SaveOptions(true)); // Include text on save
        serverCapabilities.setTextDocumentSync(syncOptions);
        
        // Completion: Provide auto-completion for keywords, types, etc.
        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(List.of(".", "@", " ", "{", "(", "<"));
        completionOptions.setResolveProvider(true); // Support completion item resolve
        serverCapabilities.setCompletionProvider(completionOptions);
        
        // Hover: Show documentation on hover
        HoverOptions hoverOptions = new HoverOptions();
        serverCapabilities.setHoverProvider(hoverOptions);
        
        // Semantic Tokens: Rich syntax highlighting
        // Using semantic tokens instead of TextMate because:
        // 1. Server-side tokenization allows semantic understanding
        // 2. Can differentiate between entity names, value objects, etc.
        // 3. More accurate than regex-based TextMate grammars
        SemanticTokensWithRegistrationOptions semanticTokensOptions = 
            new SemanticTokensWithRegistrationOptions();
        semanticTokensOptions.setLegend(DdslSemanticTokens.getLegend());
        semanticTokensOptions.setFull(true);
        semanticTokensOptions.setRange(false); // Full document only for now
        serverCapabilities.setSemanticTokensProvider(semanticTokensOptions);
        
        // Definition: Go-to-definition for types and references
        serverCapabilities.setDefinitionProvider(true);
        
        // References: Find all references to a symbol
        serverCapabilities.setReferencesProvider(true);
        
        // Document symbols: Outline view
        serverCapabilities.setDocumentSymbolProvider(true);
        
        // Formatting: Code formatting
        serverCapabilities.setDocumentFormattingProvider(true);
        
        // Signature help: Function parameter hints
        SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions();
        signatureHelpOptions.setTriggerCharacters(List.of("(", ","));
        serverCapabilities.setSignatureHelpProvider(signatureHelpOptions);
        
        // Code actions: Quick fixes, refactoring
        CodeActionOptions codeActionOptions = new CodeActionOptions();
        codeActionOptions.setCodeActionKinds(List.of(
            CodeActionKind.QuickFix,
            CodeActionKind.Refactor,
            CodeActionKind.Source
        ));
        serverCapabilities.setCodeActionProvider(codeActionOptions);

        // Execute command: allows command-based code actions and server commands.
        ExecuteCommandOptions executeCommandOptions = new ExecuteCommandOptions(DdslCommandIds.ALL);
        serverCapabilities.setExecuteCommandProvider(executeCommandOptions);
        
        // Build result
        InitializeResult result = new InitializeResult(serverCapabilities);
        
        // Server info
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setName("DDSL Language Server");
        serverInfo.setVersion("1.0.0");
        result.setServerInfo(serverInfo);
        
        log.info("DDSL Language Server initialized successfully");
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * Called after the client receives InitializeResult.
     * The server can perform additional initialization here.
     */
    @Override
    public void initialized(InitializedParams params) {
        log.info("DDSL Language Server received initialized notification");
        
        // Register for file watching if needed
        if (client != null) {
            // Could register dynamic capabilities here
            log.info("Ready to serve DDSL language features");
        }
    }
    
    @Override
    public CompletableFuture<Object> shutdown() {
        log.info("DDSL Language Server shutting down...");
        shutdownReceived = true;
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void exit() {
        log.info("DDSL Language Server exiting");
        exitHandler.exit(shutdownReceived);
    }
    
    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }
    
    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }
    
    // ========== Accessors ==========
    
    public LanguageClient getClient() {
        return client;
    }
    
    public ServerCapabilities getServerCapabilities() {
        return serverCapabilities;
    }
    
    /**
     * Publish diagnostics to the client.
     * Called by the diagnostic provider when errors/warnings are detected.
     */
    public void publishDiagnostics(String uri, List<Diagnostic> diagnostics) {
        if (client != null) {
            PublishDiagnosticsParams params = new PublishDiagnosticsParams();
            params.setUri(uri);
            params.setDiagnostics(diagnostics);
            client.publishDiagnostics(params);
            log.debug("Published {} diagnostics for {}", diagnostics.size(), uri);
        }
    }
    
    /**
     * Log a message to the client's output channel.
     */
    public void logMessage(MessageType type, String message) {
        if (client != null) {
            client.logMessage(new MessageParams(type, message));
        }
    }
    
    /**
     * Show a message to the user.
     */
    public void showMessage(MessageType type, String message) {
        if (client != null) {
            client.showMessage(new MessageParams(type, message));
        }
    }
}
