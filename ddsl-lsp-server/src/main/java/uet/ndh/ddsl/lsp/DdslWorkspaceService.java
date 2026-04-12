package uet.ndh.ddsl.lsp;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import uet.ndh.ddsl.compiler.DdslCompilationService;
import uet.ndh.ddsl.compiler.api.CompileResponse;
import uet.ndh.ddsl.lsp.core.DdslCommandIds;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
    private final DdslCompilationService compilationService;
    
    public DdslWorkspaceService(DdslLanguageServer server) {
        this.server = server;
        this.compilationService = new DdslCompilationService();
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
                case DdslCommandIds.COMPILE -> {
                    server.logMessage(MessageType.Info, "Compiling DDSL specification...");
                    return handleCompileCommand(params);
                }
                case DdslCommandIds.VALIDATE -> {
                    // Trigger validation
                    server.logMessage(MessageType.Info, "Validating DDSL specification...");
                    return "Validation started";
                }
                case DdslCommandIds.GENERATE_CODE -> {
                    // Trigger code generation
                    server.logMessage(MessageType.Info, "Generating code from DDSL...");
                    return "Code generation started";
                }
                case DdslCommandIds.CONVERT_TO_ENTITY -> {
                    server.logMessage(MessageType.Info, "Refactor: Convert ValueObject to Entity");
                    return "Refactor command accepted";
                }
                case DdslCommandIds.EXTRACT_VALUE_OBJECT,
                     DdslCommandIds.EXTRACT_TO_VALUE_OBJECT -> {
                    server.logMessage(MessageType.Info, "Refactor: Extract ValueObject");
                    return "Refactor command accepted";
                }
                case DdslCommandIds.GENERATE_INVARIANTS -> {
                    server.logMessage(MessageType.Info, "Source action: Generate invariants block");
                    return "Source action accepted";
                }
                case DdslCommandIds.GENERATE_OPERATIONS -> {
                    server.logMessage(MessageType.Info, "Source action: Generate operations block");
                    return "Source action accepted";
                }
                default -> {
                    log.warn("Unknown command: {}", command);
                    return null;
                }
            }
        });
    }

    private CompileResponse handleCompileCommand(ExecuteCommandParams params) {
        String uri = extractUri(params.getArguments());
        log.info("Param: {}", params);
        log.info("Parameters for compile command: URI={}, basePackage={}", uri, extractBasePackage(params.getArguments()));
        if (uri == null || uri.isBlank()) {
            return CompileResponse.failure("Missing file URI argument for ddsl.compile");
        }

        String content = resolveDocumentContent(uri);
        log.info("Resolved document content for URI {}: {} characters", uri, content != null ? content.length() : "null");
        if (content == null || content.isBlank()) {
            return CompileResponse.failure("Document content is empty or unavailable for URI: " + uri);
        }

        String basePackage = extractBasePackage(params.getArguments());
        CompileResponse response = compilationService.compile(content, basePackage);

        log.info("Compilation result for URI {}: success={}, artifacts={}, errors={}, diagnostics={}",
                uri, response.success(), response.artifacts().size(), response.errors().size(), response.diagnostics().size());

        if (response.success()) {
            server.showMessage(MessageType.Info,
                    "Compile successful: " + response.artifacts().size() + " artifact(s)");
        } else {
            int errorCount = response.errors().size();
            long diagnosticErrors = response.diagnostics().stream()
                    .filter(d -> "ERROR".equalsIgnoreCase(d.severity()))
                    .count();
            server.showMessage(MessageType.Error,
                    "Compile failed: " + Math.max(errorCount, (int) diagnosticErrors) + " error(s)");
        }

        return response;
    }

    private String extractUri(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }

        Object first = args.getFirst();
        if (first instanceof String s) {
            return s;
        }

        if (first instanceof Map<?, ?> map) {
            Object uri = map.get("uri");
            return uri instanceof String s ? s : null;
        }

        return null;
    }

    private String extractBasePackage(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return "com.example.domain";
        }

        Object first = args.getFirst();
        if (first instanceof Map<?, ?> map) {
            Object basePackage = map.get("basePackage");
            if (basePackage instanceof String s && !s.isBlank()) {
                return s;
            }
        }

        return "com.example.domain";
    }

    private String resolveDocumentContent(String uri) {
        String cached = server.getDdslTextDocumentService().getDocumentContent(uri);
        if (cached != null) {
            return cached;
        }

        try {
            URI parsed = URI.create(uri);
            if (!"file".equalsIgnoreCase(parsed.getScheme())) {
                return null;
            }
            Path path = Path.of(parsed);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return null;
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Unable to load document content for URI {}: {}", uri, e.getMessage());
            return null;
        }
    }
}
