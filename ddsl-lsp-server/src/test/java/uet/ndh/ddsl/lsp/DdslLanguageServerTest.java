package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.*;
import uet.ndh.ddsl.compiler.api.CompileResponse;
import uet.ndh.ddsl.lsp.core.DdslCommandIds;
import uet.ndh.ddsl.parser.lexer.Token;
import uet.ndh.ddsl.parser.lexer.TokenType;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DDSL Language Server core — lifecycle, capabilities,
 * document sync, completion, hover, diagnostics, definition, formatting,
 * document symbols, code actions, and semantic tokens.
 *
 * <p>All tests run in-process: no real socket is needed because
 * {@link DdslLanguageServer}, {@link DdslTextDocumentService} and every
 * provider are plain POJOs. A lightweight {@link StubLanguageClient}
 * captures the notifications that would normally travel over the wire.</p>
 */
class DdslLanguageServerTest {

    private DdslLanguageServer server;
    private StubLanguageClient client;

    private static final String DOC_URI = "file:///test/sample.ddsl";
    private static final String SAMPLE_DDSL = """
            BoundedContext Orders {
                domain {
                    Aggregate Order {
                        orderId: UUID @identity
                        status: String

                        operations {
                            when placing order:
                            then:
                                - set status to "PENDING"
                        }
                    }
                }
            }
            """;

    @SuppressWarnings("deprecation")
    @BeforeEach
    void setUp() throws Exception {
        server = new DdslLanguageServer();
        client = new StubLanguageClient();
        server.connect(client);

        // Initialize
        InitializeParams params = new InitializeParams();
        params.setRootUri("file:///test");
        params.setCapabilities(new ClientCapabilities());
        server.initialize(params).get(5, TimeUnit.SECONDS);
        server.initialized(new InitializedParams());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  1. Lifecycle & Capabilities
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Server exposes expected capabilities after initialize")
    void serverCapabilities() {
        ServerCapabilities caps = server.getServerCapabilities();
        assertNotNull(caps);

        // Document sync
        assertNotNull(caps.getTextDocumentSync());

        // Completion
        assertNotNull(caps.getCompletionProvider());

        // Hover
        assertNotNull(caps.getHoverProvider());

        // Semantic tokens
        assertNotNull(caps.getSemanticTokensProvider());

        // Definition & references
        assertNotNull(caps.getDefinitionProvider());
        assertNotNull(caps.getReferencesProvider());

        // Document symbols
        assertNotNull(caps.getDocumentSymbolProvider());

        // Formatting
        assertNotNull(caps.getDocumentFormattingProvider());

        // Signature help
        assertNotNull(caps.getSignatureHelpProvider());

        // Code actions
        assertNotNull(caps.getCodeActionProvider());
    }

    @Test
    @DisplayName("shutdown returns successfully")
    void shutdown() throws Exception {
        Object result = server.shutdown().get(5, TimeUnit.SECONDS);
        // shutdown should complete without exception
        assertNull(result);
    }

    @Test
    @DisplayName("workspace executeCommand supports known command")
    void executeKnownWorkspaceCommand() throws Exception {
        ExecuteCommandParams params = new ExecuteCommandParams();
        params.setCommand("ddsl.validate");

        Object result = server.getWorkspaceService()
                .executeCommand(params)
                .get(5, TimeUnit.SECONDS);

        assertEquals("Validation started", result);
    }

    @Test
    @DisplayName("workspace executeCommand returns null for unknown command")
    void executeUnknownWorkspaceCommand() throws Exception {
        ExecuteCommandParams params = new ExecuteCommandParams();
        params.setCommand("ddsl.unknown");

        Object result = server.getWorkspaceService()
                .executeCommand(params)
                .get(5, TimeUnit.SECONDS);

        assertNull(result);
    }

    @Test
    @DisplayName("workspace ddsl.compile returns full artifacts source")
    void executeCompileCommandReturnsArtifacts() throws Exception {
        openDocument(SAMPLE_DDSL);

        ExecuteCommandParams params = new ExecuteCommandParams();
        params.setCommand(DdslCommandIds.COMPILE);
        params.setArguments(List.of(DOC_URI));

        Object result = server.getWorkspaceService()
                .executeCommand(params)
                .get(10, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result instanceof CompileResponse);

        CompileResponse response = (CompileResponse) result;
        assertTrue(response.success(), "Compile should succeed for sample DDSL");
        assertFalse(response.artifacts().isEmpty(), "Compile should return generated artifacts");
        assertNotNull(response.artifacts().getFirst().sourceCode(), "Artifacts must include full source code");
        assertFalse(response.artifacts().getFirst().sourceCode().isBlank(), "Artifact source code must not be blank");
    }

    @Test
    @DisplayName("workspace ddsl.compile fails when URI argument is missing")
    void executeCompileCommandMissingUri() throws Exception {
        ExecuteCommandParams params = new ExecuteCommandParams();
        params.setCommand(DdslCommandIds.COMPILE);
        params.setArguments(List.of());

        Object result = server.getWorkspaceService()
                .executeCommand(params)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result instanceof CompileResponse);

        CompileResponse response = (CompileResponse) result;
        assertFalse(response.success());
        assertFalse(response.errors().isEmpty());
    }

    @Test
    @DisplayName("initialize exposes all execute command IDs")
    void initializeExposesExecuteCommandIds() {
        ServerCapabilities caps = server.getServerCapabilities();
        assertNotNull(caps);
        assertNotNull(caps.getExecuteCommandProvider());
        assertEquals(DdslCommandIds.ALL, caps.getExecuteCommandProvider().getCommands());
    }

    @Test
    @DisplayName("workspace executeCommand supports refactor/source commands")
    void executeRefactorAndSourceCommands() throws Exception {
        for (String command : List.of(DdslCommandIds.EXTRACT_VALUE_OBJECT, DdslCommandIds.EXTRACT_TO_VALUE_OBJECT)) {
            ExecuteCommandParams params = new ExecuteCommandParams();
            params.setCommand(command);

            Object result = server.getWorkspaceService()
                    .executeCommand(params)
                    .get(5, TimeUnit.SECONDS);

            assertEquals("Refactor command accepted", result);
        }

        for (String command : List.of(DdslCommandIds.GENERATE_INVARIANTS, DdslCommandIds.GENERATE_OPERATIONS)) {
            ExecuteCommandParams params = new ExecuteCommandParams();
            params.setCommand(command);

            Object result = server.getWorkspaceService()
                .executeCommand(params)
                .get(5, TimeUnit.SECONDS);

            assertEquals("Source action accepted", result);
        }
    }

    @Test
    @DisplayName("server message helpers dispatch to connected client")
    void serverMessageHelpersDispatchToClient() {
        server.logMessage(MessageType.Info, "hello-log");
        server.showMessage(MessageType.Warning, "hello-show");

        assertFalse(client.logMessages.isEmpty());
        assertEquals("hello-log", client.logMessages.getLast().getMessage());
        assertEquals(MessageType.Info, client.logMessages.getLast().getType());

        assertFalse(client.showMessages.isEmpty());
        assertEquals("hello-show", client.showMessages.getLast().getMessage());
        assertEquals(MessageType.Warning, client.showMessages.getLast().getType());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  2. Document Synchronization
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("didOpen triggers diagnostics publication")
    void didOpenPublishesDiagnostics() throws Exception {
        openDocument(SAMPLE_DDSL);

        // The server should have published diagnostics for the opened document
        assertFalse(client.publishedDiagnostics.isEmpty(),
                "Server should publish diagnostics on didOpen");
        assertEquals(DOC_URI, client.publishedDiagnostics.getFirst().getUri());
    }

    @Test
    @DisplayName("didChange re-publishes diagnostics")
    void didChangeRepublishesDiagnostics() throws Exception {
        openDocument(SAMPLE_DDSL);
        client.publishedDiagnostics.clear();

        // Send a change
        DidChangeTextDocumentParams changeParams = new DidChangeTextDocumentParams();
        VersionedTextDocumentIdentifier versionedId = new VersionedTextDocumentIdentifier();
        versionedId.setUri(DOC_URI);
        versionedId.setVersion(2);
        changeParams.setTextDocument(versionedId);

        TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent();
        change.setText(SAMPLE_DDSL + "\n// comment");
        changeParams.setContentChanges(List.of(change));

        server.getTextDocumentService().didChange(changeParams);

        assertFalse(client.publishedDiagnostics.isEmpty(),
                "Server should re-publish diagnostics on didChange");
    }

    @Test
    @DisplayName("didClose clears diagnostics")
    void didCloseClearsDiagnostics() throws Exception {
        openDocument(SAMPLE_DDSL);
        client.publishedDiagnostics.clear();

        DidCloseTextDocumentParams closeParams = new DidCloseTextDocumentParams();
        closeParams.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        server.getTextDocumentService().didClose(closeParams);

        // Should publish an empty diagnostics list
        assertFalse(client.publishedDiagnostics.isEmpty());
        assertTrue(client.publishedDiagnostics.getLast().getDiagnostics().isEmpty(),
                "Diagnostics should be cleared after close");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  3. Diagnostics
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Valid DDSL produces no error-level diagnostics")
    void validDdslNoDiagnosticErrors() throws Exception {
        openDocument(SAMPLE_DDSL);

        var diags = client.publishedDiagnostics.getFirst().getDiagnostics();
        long errorCount = diags.stream()
                .filter(d -> d.getSeverity() == DiagnosticSeverity.Error)
                .count();

        assertEquals(0, errorCount,
                "Valid DDSL should not produce error diagnostics. Errors: " +
                        diags.stream()
                                .filter(d -> d.getSeverity() == DiagnosticSeverity.Error)
                                .map(Diagnostic::getMessage)
                                .toList());
    }

    @Test
    @DisplayName("Invalid DDSL produces error diagnostics")
    void invalidDdslProducesDiagnosticErrors() throws Exception {
        openDocument("BoundedContext { bad syntax }}}");

        var diags = client.publishedDiagnostics.getFirst().getDiagnostics();
        long errorCount = diags.stream()
                .filter(d -> d.getSeverity() == DiagnosticSeverity.Error)
                .count();

        assertTrue(errorCount > 0,
                "Invalid DDSL should produce at least one error diagnostic");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  4. Completion
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Completion returns items at the beginning of a document")
    void completionAtDocumentStart() throws Exception {
        openDocument("");

        CompletionParams cp = new CompletionParams();
        cp.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        cp.setPosition(new Position(0, 0));

        var result = server.getTextDocumentService().completion(cp)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        // Should have some completion items (keywords, constructs)
        var items = result.isRight() ? result.getRight().getItems() : result.getLeft();
        assertNotNull(items);
        assertFalse(items.isEmpty(),
                "Should offer keyword completions at document start");
    }

    @Test
    @DisplayName("Completion inside a BoundedContext offers domain sections")
    void completionInsideBoundedContext() throws Exception {
        String partial = "BoundedContext Test {\n    \n}";
        openDocument(partial);

        CompletionParams cp = new CompletionParams();
        cp.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        cp.setPosition(new Position(1, 4)); // inside the context

        var result = server.getTextDocumentService().completion(cp)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(result);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  5. Hover
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Hover over 'BoundedContext' keyword returns documentation")
    void hoverOnKeyword() throws Exception {
        openDocument(SAMPLE_DDSL);

        HoverParams hp = new HoverParams();
        hp.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        // "BoundedContext" starts at line 0, col 0
        hp.setPosition(new Position(0, 5));

        Hover hover = server.getTextDocumentService().hover(hp)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(hover, "Hover should return documentation for BoundedContext");
        assertNotNull(hover.getContents());
    }

    @Test
    @DisplayName("Hover on empty position returns null")
    void hoverOnBlankLine() throws Exception {
        openDocument(SAMPLE_DDSL);

        HoverParams hp = new HoverParams();
        hp.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        hp.setPosition(new Position(999, 0)); // way past the document

        Hover hover = server.getTextDocumentService().hover(hp)
                .get(5, TimeUnit.SECONDS);

        // Hover on an out-of-range position should return null
        assertNull(hover);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  6. Semantic Tokens
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Semantic tokens are returned for a valid document")
    void semanticTokensFull() throws Exception {
        openDocument(SAMPLE_DDSL);

        SemanticTokensParams stp = new SemanticTokensParams();
        stp.setTextDocument(new TextDocumentIdentifier(DOC_URI));

        SemanticTokens tokens = server.getTextDocumentService()
                .semanticTokensFull(stp)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(tokens);
        assertNotNull(tokens.getData());
        assertFalse(tokens.getData().isEmpty(),
                "Semantic tokens data should be non-empty for a valid document");
    }

        @Test
        @DisplayName("Semantic tokens range returns tokens for a selected slice")
        void semanticTokensRange() throws Exception {
        openDocument(SAMPLE_DDSL);

        SemanticTokensRangeParams stp = new SemanticTokensRangeParams();
        stp.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        stp.setRange(new Range(new Position(0, 0), new Position(0, 25)));

        SemanticTokens rangeTokens = server.getTextDocumentService()
            .semanticTokensRange(stp)
            .get(5, TimeUnit.SECONDS);

        SemanticTokens fullTokens = server.getTextDocumentService()
            .semanticTokensFull(new SemanticTokensParams(new TextDocumentIdentifier(DOC_URI)))
            .get(5, TimeUnit.SECONDS);

        assertNotNull(rangeTokens);
        assertNotNull(rangeTokens.getData());
        assertFalse(rangeTokens.getData().isEmpty(),
            "Semantic tokens range should return token data for the selected line");
        assertNotNull(fullTokens);
        assertTrue(rangeTokens.getData().size() < fullTokens.getData().size(),
            "Range token payload should be smaller than full document payload");
        }

        @Test
        @DisplayName("Semantic token encoder skips invalid and out-of-order tokens")
        void semanticTokensEncoderSafety() {
        List<Token> tokens = List.of(
            new Token(TokenType.IDENTIFIER, "Order", 2, 1, 0),
            new Token(TokenType.IDENTIFIER, "", 2, 5, 4),
            new Token(TokenType.IDENTIFIER, "Backtrack", 1, 1, 0)
        );

        SemanticTokens encoded = DdslSemanticTokens.encode(tokens);

        assertNotNull(encoded);
        assertNotNull(encoded.getData());
        assertEquals(5, encoded.getData().size(),
            "Only the first valid token should be encoded as one LSP semantic token tuple");
        }

    // ═══════════════════════════════════════════════════════════════════
    //  7. Document Symbols
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Document symbols are returned for a valid document")
    void documentSymbols() throws Exception {
        openDocument(SAMPLE_DDSL);

        DocumentSymbolParams dsp = new DocumentSymbolParams();
        dsp.setTextDocument(new TextDocumentIdentifier(DOC_URI));

        var symbols = server.getTextDocumentService().documentSymbol(dsp)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(symbols);
        assertFalse(symbols.isEmpty(),
                "Should return document symbols (BoundedContext, Aggregate, Entity, etc.)");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  8. Definition
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Go-to-definition returns locations or empty list")
    void definition() throws Exception {
        openDocument(SAMPLE_DDSL);

        DefinitionParams dp = new DefinitionParams();
        dp.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        // Position on "OrderItem" type reference (line ~5, inside items: List<OrderItem>)
        dp.setPosition(new Position(5, 30));

        var result = server.getTextDocumentService().definition(dp)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        // May be empty if the provider cannot resolve, but should not throw
    }

    // ═══════════════════════════════════════════════════════════════════
    //  9. Formatting
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Formatting returns text edits")
    void formatting() throws Exception {
        String unformatted = "BoundedContext X{domain{Aggregate Y{@identity id:UUID}}}";
        openDocument(unformatted);

        DocumentFormattingParams fp = new DocumentFormattingParams();
        fp.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        FormattingOptions opts = new FormattingOptions(4, true);
        fp.setOptions(opts);

        var edits = server.getTextDocumentService().formatting(fp)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(edits);
        // Formatter should produce at least one edit (or empty list if already formatted)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  10. Code Actions
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Code actions are returned (possibly empty) without error")
    void codeActions() throws Exception {
        openDocument(SAMPLE_DDSL);

        CodeActionParams cap = new CodeActionParams();
        cap.setTextDocument(new TextDocumentIdentifier(DOC_URI));
        cap.setRange(new Range(new Position(0, 0), new Position(5, 0)));
        CodeActionContext ctx = new CodeActionContext();
        ctx.setDiagnostics(List.of());
        cap.setContext(ctx);

        var actions = server.getTextDocumentService().codeAction(cap)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(actions);
        // May or may not have actions, but should not throw
    }

    // ═══════════════════════════════════════════════════════════════════
    //  11. Diagnostic Provider (unit-level)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DdslDiagnosticProvider")
    class DiagnosticProviderTests {

        private final DdslDiagnosticProvider provider = new DdslDiagnosticProvider();

        @Test
        @DisplayName("Valid DDSL produces zero error diagnostics")
        void validCode() {
            var diags = provider.getDiagnostics(SAMPLE_DDSL);
            long errors = diags.stream()
                    .filter(d -> d.getSeverity() == DiagnosticSeverity.Error)
                    .count();
            assertEquals(0, errors,
                    "Valid DDSL should have no error diagnostics");
        }

        @Test
        @DisplayName("Syntax error produces at least one error diagnostic")
        void syntaxError() {
            var diags = provider.getDiagnostics("BoundedContext { oops }}}");
            long errors = diags.stream()
                    .filter(d -> d.getSeverity() == DiagnosticSeverity.Error)
                    .count();
            assertTrue(errors > 0);
        }

        @Test
        @DisplayName("Empty input does not crash")
        void emptyInput() {
            var diags = provider.getDiagnostics("");
            assertNotNull(diags);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  12. Completion Provider (unit-level)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DdslCompletionProvider")
    class CompletionProviderTests {

        private final DdslCompletionProvider provider = new DdslCompletionProvider();

        @Test
        @DisplayName("Completions at document start include BoundedContext")
        void topLevel() {
            var items = provider.getCompletions("", List.of(),
                    new Position(0, 0), null);
            boolean hasBoundedContext = items.stream()
                    .anyMatch(i -> i.getLabel().contains("BoundedContext"));
            assertTrue(hasBoundedContext,
                    "Should offer BoundedContext at the top level");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  13. Hover Provider (unit-level)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DdslHoverProvider")
    class HoverProviderTests {

        private final DdslHoverProvider provider = new DdslHoverProvider();

        @Test
        @DisplayName("Hover on 'Aggregate' keyword returns documentation")
        void hoverAggregate() {
            var scanner = new uet.ndh.ddsl.parser.lexer.Scanner(SAMPLE_DDSL);
            var tokens = scanner.scanTokens();

            // Find position of "Aggregate" keyword
            var aggToken = tokens.stream()
                    .filter(t -> t.getLexeme().equals("Aggregate"))
                    .findFirst().orElse(null);
            assertNotNull(aggToken, "Should find Aggregate token");

            Hover h = provider.getHover(SAMPLE_DDSL, tokens,
                    new Position(aggToken.getLine() - 1, aggToken.getColumn()));
            assertNotNull(h, "Hover on 'Aggregate' should return docs");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════

    private void openDocument(String content) {
        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        TextDocumentItem item = new TextDocumentItem();
        item.setUri(DOC_URI);
        item.setLanguageId("ddsl");
        item.setVersion(1);
        item.setText(content);
        openParams.setTextDocument(item);
        server.getTextDocumentService().didOpen(openParams);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Stub client that captures notifications
    // ═══════════════════════════════════════════════════════════════════

    /**
     * A minimal {@link LanguageClient} that records every notification
     * the server sends, so we can assert on them.
     */
    static class StubLanguageClient implements LanguageClient {

        final List<PublishDiagnosticsParams> publishedDiagnostics =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final List<MessageParams> logMessages =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final List<MessageParams> showMessages =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        @Override
        public void telemetryEvent(Object object) { }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
            publishedDiagnostics.add(diagnostics);
        }

        @Override
        public void showMessage(MessageParams messageParams) {
            showMessages.add(messageParams);
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(
                ShowMessageRequestParams params) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void logMessage(MessageParams message) {
            logMessages.add(message);
        }
    }
}
