package uet.ndh.ddsl.lsp.websocket;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import uet.ndh.ddsl.lsp.DdslLanguageServer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simple WebSocket handler for LSP communication.
 * 
 * This is a simplified implementation that directly handles JSON-RPC messages
 * without using LSP4J's Launcher (which expects stdio streams).
 * 
 * Flow:
 * 1. Client sends JSON-RPC request via WebSocket
 * 2. Handler parses the request and invokes appropriate server method
 * 3. Response is serialized and sent back via WebSocket
 * 
 * This approach gives us more control over the message handling and
 * is better suited for WebSocket transport.
 */
@Slf4j
public class SimpleLspWebSocketHandler extends TextWebSocketHandler {

    /** Gson instance for JSON serialization */
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(Either.class, new EitherTypeAdapter())
        .create();
    
    /** Map of session ID to server instance */
    private final ConcurrentHashMap<String, SessionContext> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("LSP WebSocket connection established: {}", session.getId());
        
        // Create language server and client proxy
        DdslLanguageServer server = new DdslLanguageServer();
        WebSocketLanguageClient client = new WebSocketLanguageClient(session, gson);
        server.connect(client);
        
        SessionContext context = new SessionContext(server, client);
        sessions.put(session.getId(), context);
        
        log.info("LSP session ready for: {}", session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        SessionContext context = sessions.get(sessionId);
        
        if (context == null) {
            log.error("No session context for: {}", sessionId);
            return;
        }
        
        String payload = message.getPayload();
        log.debug("Received: {}", payload);
        
        try {
            // Parse JSON-RPC message
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            
            String method = json.has("method") ? json.get("method").getAsString() : null;
            JsonElement id = json.get("id");
            JsonElement params = json.get("params");
            
            if (method != null) {
                // This is a request or notification
                CompletableFuture<?> result = handleMethod(context, method, params);
                
                if (id != null) {
                    // It's a request, send response
                    result.whenComplete((response, error) -> {
                        sendResponse(session, id, response, error);
                    });
                }
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
            // Send error response if we have an id
        }
    }
    
    /**
     * Handle an LSP method call.
     */
    private CompletableFuture<?> handleMethod(SessionContext context, String method, JsonElement params) {
        DdslLanguageServer server = context.server;
        
        return switch (method) {
            // Lifecycle
            case "initialize" -> {
                InitializeParams p = gson.fromJson(params, InitializeParams.class);
                yield server.initialize(p);
            }
            case "initialized" -> {
                InitializedParams p = gson.fromJson(params, InitializedParams.class);
                server.initialized(p);
                yield CompletableFuture.completedFuture(null);
            }
            case "shutdown" -> server.shutdown();
            case "exit" -> {
                server.exit();
                yield CompletableFuture.completedFuture(null);
            }
            
            // Text Document
            case "textDocument/didOpen" -> {
                DidOpenTextDocumentParams p = gson.fromJson(params, DidOpenTextDocumentParams.class);
                server.getTextDocumentService().didOpen(p);
                yield CompletableFuture.completedFuture(null);
            }
            case "textDocument/didChange" -> {
                DidChangeTextDocumentParams p = gson.fromJson(params, DidChangeTextDocumentParams.class);
                server.getTextDocumentService().didChange(p);
                yield CompletableFuture.completedFuture(null);
            }
            case "textDocument/didClose" -> {
                DidCloseTextDocumentParams p = gson.fromJson(params, DidCloseTextDocumentParams.class);
                server.getTextDocumentService().didClose(p);
                yield CompletableFuture.completedFuture(null);
            }
            case "textDocument/didSave" -> {
                DidSaveTextDocumentParams p = gson.fromJson(params, DidSaveTextDocumentParams.class);
                server.getTextDocumentService().didSave(p);
                yield CompletableFuture.completedFuture(null);
            }
            
            // Completion
            case "textDocument/completion" -> {
                CompletionParams p = gson.fromJson(params, CompletionParams.class);
                yield server.getTextDocumentService().completion(p);
            }
            case "completionItem/resolve" -> {
                CompletionItem p = gson.fromJson(params, CompletionItem.class);
                yield server.getTextDocumentService().resolveCompletionItem(p);
            }
            
            // Hover
            case "textDocument/hover" -> {
                HoverParams p = gson.fromJson(params, HoverParams.class);
                yield server.getTextDocumentService().hover(p);
            }
            
            // Definition
            case "textDocument/definition" -> {
                DefinitionParams p = gson.fromJson(params, DefinitionParams.class);
                yield server.getTextDocumentService().definition(p);
            }
            
            // References
            case "textDocument/references" -> {
                ReferenceParams p = gson.fromJson(params, ReferenceParams.class);
                yield server.getTextDocumentService().references(p);
            }
            
            // Document Symbols
            case "textDocument/documentSymbol" -> {
                DocumentSymbolParams p = gson.fromJson(params, DocumentSymbolParams.class);
                yield server.getTextDocumentService().documentSymbol(p);
            }
            
            // Formatting
            case "textDocument/formatting" -> {
                DocumentFormattingParams p = gson.fromJson(params, DocumentFormattingParams.class);
                yield server.getTextDocumentService().formatting(p);
            }
            
            // Semantic Tokens
            case "textDocument/semanticTokens/full" -> {
                SemanticTokensParams p = gson.fromJson(params, SemanticTokensParams.class);
                yield server.getTextDocumentService().semanticTokensFull(p);
            }
            
            // Signature Help
            case "textDocument/signatureHelp" -> {
                SignatureHelpParams p = gson.fromJson(params, SignatureHelpParams.class);
                yield server.getTextDocumentService().signatureHelp(p);
            }
            
            // Code Actions
            case "textDocument/codeAction" -> {
                CodeActionParams p = gson.fromJson(params, CodeActionParams.class);
                yield server.getTextDocumentService().codeAction(p);
            }
            
            // Workspace
            case "workspace/didChangeConfiguration" -> {
                DidChangeConfigurationParams p = gson.fromJson(params, DidChangeConfigurationParams.class);
                server.getWorkspaceService().didChangeConfiguration(p);
                yield CompletableFuture.completedFuture(null);
            }
            case "workspace/didChangeWatchedFiles" -> {
                DidChangeWatchedFilesParams p = gson.fromJson(params, DidChangeWatchedFilesParams.class);
                server.getWorkspaceService().didChangeWatchedFiles(p);
                yield CompletableFuture.completedFuture(null);
            }
            case "workspace/symbol" -> {
                WorkspaceSymbolParams p = gson.fromJson(params, WorkspaceSymbolParams.class);
                yield server.getWorkspaceService().symbol(p);
            }
            case "workspace/executeCommand" -> {
                ExecuteCommandParams p = gson.fromJson(params, ExecuteCommandParams.class);
                yield server.getWorkspaceService().executeCommand(p);
            }
            
            default -> {
                log.warn("Unknown method: {}", method);
                yield CompletableFuture.completedFuture(null);
            }
        };
    }
    
    /**
     * Send a JSON-RPC response.
     */
    private void sendResponse(WebSocketSession session, JsonElement id, Object result, Throwable error) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        
        if (error != null) {
            JsonObject errorObj = new JsonObject();
            errorObj.addProperty("code", -32603); // Internal error
            errorObj.addProperty("message", error.getMessage());
            response.add("error", errorObj);
        } else {
            response.add("result", gson.toJsonTree(result));
        }
        
        try {
            String json = gson.toJson(response);
            log.debug("Sending response: {}", json);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Error sending response", e);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("LSP WebSocket closed: {} ({})", session.getId(), status);
        sessions.remove(session.getId());
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket error: {}", exception.getMessage());
    }
    
    /**
     * Session context holding server and client.
     */
    private record SessionContext(DdslLanguageServer server, WebSocketLanguageClient client) {}
    
    /**
     * Language client implementation that sends notifications via WebSocket.
     */
    private static class WebSocketLanguageClient implements LanguageClient {
        private final WebSocketSession session;
        private final Gson gson;
        
        WebSocketLanguageClient(WebSocketSession session, Gson gson) {
            this.session = session;
            this.gson = gson;
        }
        
        private void sendNotification(String method, Object params) {
            JsonObject notification = new JsonObject();
            notification.addProperty("jsonrpc", "2.0");
            notification.addProperty("method", method);
            notification.add("params", gson.toJsonTree(params));
            
            try {
                String json = gson.toJson(notification);
                log.debug("Sending notification: {}", json);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("Error sending notification", e);
            }
        }
        
        @Override
        public void telemetryEvent(Object object) {
            sendNotification("telemetry/event", object);
        }
        
        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
            sendNotification("textDocument/publishDiagnostics", diagnostics);
        }
        
        @Override
        public void showMessage(MessageParams messageParams) {
            sendNotification("window/showMessage", messageParams);
        }
        
        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
            // Would need to implement request/response
            return CompletableFuture.completedFuture(null);
        }
        
        @Override
        public void logMessage(MessageParams message) {
            sendNotification("window/logMessage", message);
        }
    }
    
    /**
     * Custom type adapter for LSP4J Either type.
     */
    private static class EitherTypeAdapter implements JsonSerializer<Either<?, ?>>, JsonDeserializer<Either<?, ?>> {
        @Override
        public JsonElement serialize(Either<?, ?> src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            if (src.isLeft()) {
                return context.serialize(src.getLeft());
            } else {
                return context.serialize(src.getRight());
            }
        }
        
        @Override
        public Either<?, ?> deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) {
            // This is complex - would need proper type handling
            return Either.forLeft(json);
        }
    }
}
