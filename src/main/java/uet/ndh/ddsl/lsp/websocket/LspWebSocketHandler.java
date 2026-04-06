package uet.ndh.ddsl.lsp.websocket;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import uet.ndh.ddsl.lsp.DdslLanguageServer;
import uet.ndh.ddsl.lsp.core.DdslLanguageServerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebSocket handler for LSP communication.
 * <p>
 * This handler bridges WebSocket messages to LSP4J's JSON-RPC processing.
 * <p>
 * Architecture:
 * 1. Client connects via WebSocket
 * 2. Messages are forwarded to LSP4J Launcher
 * 3. LSP4J processes JSON-RPC and invokes server methods
 * 4. Responses are sent back through WebSocket
 * <p>
 * Why WebSocket?
 * - Bidirectional communication (server can push diagnostics)
 * - Works in browsers (for Monaco Editor)
 * - Stateful connection per session
 * <p>
 * Transport:
 * - Uses LSP4J's standard JSON-RPC launcher
 * - Custom streams bridge WebSocket to InputSteram/OutputStream
 */
@Slf4j
public class LspWebSocketHandler extends TextWebSocketHandler {


    /**
     * Map of session ID to language server instance
     */
    private final ConcurrentHashMap<String, LspSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("LSP WebSocket connection established: {}", session.getId());

        // Create a new language server instance for this session
        DdslLanguageServer server = DdslLanguageServerFactory.createEmbedded();

        // Create piped streams for communication
        PipedInputStream serverInput = new PipedInputStream();
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);

        PipedInputStream clientInput = new PipedInputStream();
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput);

        // Create LSP4J launcher
        Launcher<LanguageClient> launcher = Launcher.createLauncher(
                server,
                LanguageClient.class,
                serverInput,
                serverOutput
        );

        // Connect the server to the client proxy
        LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);

        // Create session holder
        LspSession lspSession = new LspSession(
                clientOutput,
                clientInput,
                session
        );
        sessions.put(session.getId(), lspSession);

        // Start listening for server responses in a separate thread
        lspSession.startResponseReader();

        // Start the LSP4J message processing
        launcher.startListening();

        log.info("LSP session initialized for: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        LspSession lspSession = sessions.get(sessionId);

        if (lspSession == null) {
            log.error("No LSP session found for WebSocket: {}", sessionId);
            return;
        }

        String payload = message.getPayload();
        log.debug("Received LSP message: {}", payload);

        // Forward the message to LSP4J
        // LSP uses Content-Length header, so we need to wrap the message
        String lspMessage = "Content-Length: " + payload.getBytes().length + "\r\n\r\n" + payload;

        try {
            lspSession.getClientOutput().write(lspMessage.getBytes());
            lspSession.getClientOutput().flush();
        } catch (IOException e) {
            log.error("Error writing to LSP input stream", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        log.info("LSP WebSocket connection closed: {} ({})", sessionId, status);

        LspSession lspSession = sessions.remove(sessionId);
        if (lspSession != null) {
            lspSession.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("LSP WebSocket transport error for session {}: {}",
                session.getId(), exception.getMessage());
    }

    /**
     * Holds the state for an LSP session.
     */
    private static class LspSession {
        private final OutputStream clientOutput;
        private final InputStream clientInput;
        private final WebSocketSession webSocketSession;
        private final ExecutorService executor;
        private volatile boolean running = true;

        LspSession(OutputStream clientOutput,
                   InputStream clientInput,
                   WebSocketSession webSocketSession) {
            this.clientOutput = clientOutput;
            this.clientInput = clientInput;
            this.webSocketSession = webSocketSession;
            this.executor = Executors.newSingleThreadExecutor();
        }

        OutputStream getClientOutput() {
            return clientOutput;
        }

        /**
         * Start reading responses from the server and forwarding to WebSocket.
         */
        void startResponseReader() {
            executor.submit(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(clientInput));

                    while (running && !Thread.currentThread().isInterrupted()) {
                        // Read LSP message (Content-Length header + body)
                        String headerLine = reader.readLine();
                        if (headerLine == null) break;

                        // Parse Content-Length
                        int contentLength = 0;
                        while (headerLine != null && !headerLine.isEmpty()) {
                            if (headerLine.startsWith("Content-Length:")) {
                                contentLength = Integer.parseInt(
                                        headerLine.substring(15).trim());
                            }
                            headerLine = reader.readLine();
                        }

                        if (contentLength > 0) {
                            // Read the JSON body
                            char[] body = new char[contentLength];
                            int read = reader.read(body, 0, contentLength);

                            if (read > 0) {
                                String jsonMessage = new String(body, 0, read);
                                log.debug("Sending LSP response: {}", jsonMessage);

                                // Send to WebSocket
                                if (webSocketSession.isOpen()) {
                                    webSocketSession.sendMessage(
                                            new TextMessage(jsonMessage));
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    if (running) {
                        log.error("Error reading LSP responses", e);
                    }
                }
            });
        }

        void close() {
            running = false;
            executor.shutdownNow();
            try {
                clientOutput.close();
                clientInput.close();
            } catch (IOException e) {
                log.warn("Error closing LSP session streams", e);
            }
        }
    }
}
