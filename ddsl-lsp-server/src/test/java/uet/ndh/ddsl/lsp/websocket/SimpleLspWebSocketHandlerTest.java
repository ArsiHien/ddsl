package uet.ndh.ddsl.lsp.websocket;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link SimpleLspWebSocketHandler} — the JSON-RPC bridge
 * between WebSocket clients and the DDSL Language Server.
 *
 * <p>Uses a Mockito-stubbed {@link WebSocketSession} so no real
 * server/socket is required. Verifies the full JSON-RPC
 * request → response lifecycle over simulated WebSocket frames.</p>
 */
class SimpleLspWebSocketHandlerTest {

    private SimpleLspWebSocketHandler handler;
    private WebSocketSession session;
    private final List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());
    private final Gson gson = new GsonBuilder().create();

    @BeforeEach
    void setUp() throws Exception {
        handler  = new SimpleLspWebSocketHandler();
        session  = mockSession("test-session-1");

        // Establish connection
        handler.afterConnectionEstablished(session);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        } catch (Exception ignored) { }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  1. Connection lifecycle
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Connection established creates a session context")
    void connectionEstablished() {
        // If we got here without exception, the handler accepted the connection
        // Verify by sending a message (would fail if no context)
        assertDoesNotThrow(() ->
                sendJsonRpc("initialize", initializeParams(), 1));
    }

    @Test
    @DisplayName("Connection close does not throw")
    void connectionClosed() {
        assertDoesNotThrow(() ->
                handler.afterConnectionClosed(session, CloseStatus.NORMAL));
    }

    @Test
    @DisplayName("Transport error is handled gracefully")
    void transportError() {
        assertDoesNotThrow(() ->
                handler.handleTransportError(session, new RuntimeException("network")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  2. LSP initialize handshake
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("initialize request returns result with server capabilities")
    void initializeHandshake() throws Exception {
        sendJsonRpc("initialize", initializeParams(), 1);

        // Wait briefly for async processing
        Thread.sleep(500);

        assertFalse(sentMessages.isEmpty(), "Server should send a response");
        JsonObject response = JsonParser.parseString(sentMessages.getFirst()).getAsJsonObject();

        assertEquals("2.0", response.get("jsonrpc").getAsString());
        assertNotNull(response.get("id"));
        assertNotNull(response.get("result"), "initialize must return a result");

        JsonObject result = response.getAsJsonObject("result");
        assertTrue(result.has("capabilities"),
                "Result must contain 'capabilities'");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  3. textDocument/didOpen notification
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("textDocument/didOpen triggers publishDiagnostics notification")
    void didOpenNotification() throws Exception {
        // First initialize
        sendJsonRpc("initialize", initializeParams(), 1);
        Thread.sleep(300);
        sentMessages.clear();

        // Send didOpen (notification — no id)
        JsonObject didOpenParams = new JsonObject();
        JsonObject textDocument = new JsonObject();
        textDocument.addProperty("uri", "file:///test.ddsl");
        textDocument.addProperty("languageId", "ddsl");
        textDocument.addProperty("version", 1);
        textDocument.addProperty("text", """
                BoundedContext Test {
                    domain {
                        Aggregate Foo {
                            @identity fooId: UUID
                        }
                    }
                }
                """);
        didOpenParams.add("textDocument", textDocument);
        sendJsonRpcNotification("textDocument/didOpen", didOpenParams);

        Thread.sleep(500);

        // Server should push diagnostics
        boolean hasDiagNotification = sentMessages.stream().anyMatch(msg -> {
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            return json.has("method") &&
                    "textDocument/publishDiagnostics".equals(json.get("method").getAsString());
        });
        assertTrue(hasDiagNotification,
                "Server should push textDocument/publishDiagnostics after didOpen");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  4. textDocument/completion request
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("textDocument/completion returns a response")
    void completionRequest() throws Exception {
        initializeAndOpen();
        sentMessages.clear();

        JsonObject params = new JsonObject();
        JsonObject td = new JsonObject();
        td.addProperty("uri", "file:///test.ddsl");
        params.add("textDocument", td);
        JsonObject pos = new JsonObject();
        pos.addProperty("line", 0);
        pos.addProperty("character", 0);
        params.add("position", pos);

        sendJsonRpc("textDocument/completion", params, 10);
        Thread.sleep(500);

        assertTrue(sentMessages.stream().anyMatch(msg -> {
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            return json.has("id") && json.get("id").getAsInt() == 10 &&
                    json.has("result");
        }), "Should receive completion response with id=10");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  5. textDocument/hover request
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("textDocument/hover returns a response")
    void hoverRequest() throws Exception {
        initializeAndOpen();
        sentMessages.clear();

        JsonObject params = new JsonObject();
        JsonObject td = new JsonObject();
        td.addProperty("uri", "file:///test.ddsl");
        params.add("textDocument", td);
        JsonObject pos = new JsonObject();
        pos.addProperty("line", 0);
        pos.addProperty("character", 5); // on "BoundedContext"
        params.add("position", pos);

        sendJsonRpc("textDocument/hover", params, 20);
        Thread.sleep(500);

        assertTrue(sentMessages.stream().anyMatch(msg -> {
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            return json.has("id") && json.get("id").getAsInt() == 20;
        }), "Should receive hover response with id=20");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  6. textDocument/semanticTokens/full request
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("textDocument/semanticTokens/full returns token data")
    void semanticTokensRequest() throws Exception {
        initializeAndOpen();
        sentMessages.clear();

        JsonObject params = new JsonObject();
        JsonObject td = new JsonObject();
        td.addProperty("uri", "file:///test.ddsl");
        params.add("textDocument", td);

        sendJsonRpc("textDocument/semanticTokens/full", params, 30);
        Thread.sleep(500);

        assertTrue(sentMessages.stream().anyMatch(msg -> {
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            return json.has("id") && json.get("id").getAsInt() == 30 &&
                    json.has("result");
        }), "Should receive semantic tokens response with id=30");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  7. textDocument/documentSymbol request
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("textDocument/documentSymbol returns symbol list")
    void documentSymbolRequest() throws Exception {
        initializeAndOpen();
        sentMessages.clear();

        JsonObject params = new JsonObject();
        JsonObject td = new JsonObject();
        td.addProperty("uri", "file:///test.ddsl");
        params.add("textDocument", td);

        sendJsonRpc("textDocument/documentSymbol", params, 40);
        Thread.sleep(500);

        assertTrue(sentMessages.stream().anyMatch(msg -> {
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            return json.has("id") && json.get("id").getAsInt() == 40;
        }), "Should receive documentSymbol response with id=40");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  8. textDocument/formatting request
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("textDocument/formatting returns text edits")
    void formattingRequest() throws Exception {
        initializeAndOpen();
        sentMessages.clear();

        JsonObject params = new JsonObject();
        JsonObject td = new JsonObject();
        td.addProperty("uri", "file:///test.ddsl");
        params.add("textDocument", td);
        JsonObject opts = new JsonObject();
        opts.addProperty("tabSize", 4);
        opts.addProperty("insertSpaces", true);
        params.add("options", opts);

        sendJsonRpc("textDocument/formatting", params, 50);
        Thread.sleep(500);

        assertTrue(sentMessages.stream().anyMatch(msg -> {
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            return json.has("id") && json.get("id").getAsInt() == 50;
        }), "Should receive formatting response with id=50");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  9. shutdown request
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("shutdown request returns response")
    void shutdownRequest() throws Exception {
        sendJsonRpc("initialize", initializeParams(), 1);
        Thread.sleep(300);
        sentMessages.clear();

        sendJsonRpc("shutdown", null, 99);
        Thread.sleep(300);

        assertTrue(sentMessages.stream().anyMatch(msg -> {
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            return json.has("id") && json.get("id").getAsInt() == 99;
        }), "Should receive shutdown response with id=99");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  10. Unknown method is handled gracefully
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Unknown method does not throw")
    void unknownMethod() throws Exception {
        sendJsonRpc("initialize", initializeParams(), 1);
        Thread.sleep(300);

        assertDoesNotThrow(() ->
                sendJsonRpc("custom/nonExistent", new JsonObject(), 999));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  11. Multiple concurrent sessions
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Handler supports multiple concurrent WebSocket sessions")
    void multipleSessions() throws Exception {
        List<String> sent2 = Collections.synchronizedList(new ArrayList<>());
        WebSocketSession session2 = mockSession("test-session-2", sent2);
        handler.afterConnectionEstablished(session2);

        // Send initialize to both
        sendJsonRpc("initialize", initializeParams(), 1);
        sendJsonRpcTo(session2, "initialize", initializeParams(), 1);
        Thread.sleep(500);

        // Both should receive responses
        assertFalse(sentMessages.isEmpty(), "Session 1 should receive response");
        assertFalse(sent2.isEmpty(), "Session 2 should receive response");

        handler.afterConnectionClosed(session2, CloseStatus.NORMAL);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  12. Invalid JSON is handled gracefully
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Malformed JSON does not crash the handler")
    void malformedJson() {
        assertDoesNotThrow(() ->
                handler.handleTextMessage(session, new TextMessage("not json {{")));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════

    private void initializeAndOpen() throws Exception {
        sendJsonRpc("initialize", initializeParams(), 1);
        Thread.sleep(300);

        sendJsonRpcNotification("initialized", new JsonObject());

        JsonObject didOpenParams = new JsonObject();
        JsonObject textDocument = new JsonObject();
        textDocument.addProperty("uri", "file:///test.ddsl");
        textDocument.addProperty("languageId", "ddsl");
        textDocument.addProperty("version", 1);
        textDocument.addProperty("text", """
                BoundedContext Demo {
                    domain {
                        Aggregate Widget {
                            @identity widgetId: UUID
                            label: String
                        }
                    }
                }
                """);
        didOpenParams.add("textDocument", textDocument);
        sendJsonRpcNotification("textDocument/didOpen", didOpenParams);
        Thread.sleep(300);
    }

    private JsonObject initializeParams() {
        JsonObject p = new JsonObject();
        p.addProperty("processId", 12345);
        p.addProperty("rootUri", "file:///test");
        p.add("capabilities", new JsonObject());
        return p;
    }

    private void sendJsonRpc(String method, JsonElement params, int id) throws Exception {
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("method", method);
        msg.addProperty("id", id);
        if (params != null) {
            msg.add("params", params);
        }
        handler.handleTextMessage(session, new TextMessage(gson.toJson(msg)));
    }

    private void sendJsonRpcNotification(String method, JsonElement params) throws Exception {
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("method", method);
        if (params != null) {
            msg.add("params", params);
        }
        handler.handleTextMessage(session, new TextMessage(gson.toJson(msg)));
    }

    private void sendJsonRpcTo(WebSocketSession ws, String method, JsonElement params, int id)
            throws Exception {
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("method", method);
        msg.addProperty("id", id);
        if (params != null) {
            msg.add("params", params);
        }
        handler.handleTextMessage(ws, new TextMessage(gson.toJson(msg)));
    }

    /**
     * Create a mock {@link WebSocketSession} that captures sent messages
     * in the test's {@code sentMessages} list.
     */
    private WebSocketSession mockSession(String id) throws IOException {
        return mockSession(id, sentMessages);
    }

    private WebSocketSession mockSession(String id, List<String> sink) throws IOException {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn(id);
        when(ws.isOpen()).thenReturn(true);
        when(ws.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 0));
        when(ws.getUri()).thenReturn(URI.create("ws://localhost/lsp"));

        doAnswer(invocation -> {
            TextMessage m = invocation.getArgument(0);
            sink.add(m.getPayload());
            return null;
        }).when(ws).sendMessage(any(TextMessage.class));

        return ws;
    }
}
