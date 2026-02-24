package uet.ndh.ddsl.lsp;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Workspace Service for DDSL Language Server.
 * 
 * Handles workspace-level operations:
 * - Configuration changes
 * - Watched file changes
 * - Workspace symbols
 * - Execute commands
 */
@Slf4j
public class DdslWorkspaceService implements WorkspaceService {

    private final DdslLanguageServer server;
    
    public DdslWorkspaceService(DdslLanguageServer server) {
        this.server = server;
    }
    
    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        log.info("Configuration changed: {}", params.getSettings());
        // Handle configuration changes (e.g., formatting options, validation rules)
    }
    
    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        log.info("Watched files changed: {}", params.getChanges().size());
        
        for (FileEvent event : params.getChanges()) {
            String uri = event.getUri();
            FileChangeType type = event.getType();
            
            switch (type) {
                case Created -> log.debug("File created: {}", uri);
                case Changed -> log.debug("File changed: {}", uri);
                case Deleted -> log.debug("File deleted: {}", uri);
            }
        }
    }
    
    @Override
    public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
        String query = params.getQuery();
        log.debug("Workspace symbol search: {}", query);
        
        // TODO: Implement workspace-wide symbol search
        // This would search across all DDSL files in the workspace
        return CompletableFuture.completedFuture(Either.forLeft(List.of()));
    }
    
    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        String command = params.getCommand();
        log.info("Execute command: {}", command);
        
        return CompletableFuture.supplyAsync(() -> {
            switch (command) {
                case "ddsl.compile" -> {
                    // Trigger compilation
                    server.logMessage(MessageType.Info, "Compiling DDSL specification...");
                    return "Compilation started";
                }
                case "ddsl.validate" -> {
                    // Trigger validation
                    server.logMessage(MessageType.Info, "Validating DDSL specification...");
                    return "Validation started";
                }
                case "ddsl.generateCode" -> {
                    // Trigger code generation
                    server.logMessage(MessageType.Info, "Generating code from DDSL...");
                    return "Code generation started";
                }
                default -> {
                    log.warn("Unknown command: {}", command);
                    return null;
                }
            }
        });
    }
}
