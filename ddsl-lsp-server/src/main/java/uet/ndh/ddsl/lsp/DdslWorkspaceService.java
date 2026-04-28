package uet.ndh.ddsl.lsp;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.ProjectWriter;
import uet.ndh.ddsl.lsp.diagram.DdslDiagramService;
import uet.ndh.ddsl.lsp.diagram.DiagramResponse;
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
    private final DdslDiagramService diagramService;
    private final ProjectWriter projectWriter;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();
    
    public DdslWorkspaceService(DdslLanguageServer server) {
        this.server = server;
        this.compilationService = new DdslCompilationService();
        this.diagramService = new DdslDiagramService();
        this.projectWriter = new ProjectWriter();
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
                case DdslCommandIds.GENERATE_COMPONENT_DIAGRAM -> {
                    server.logMessage(MessageType.Info, "Generating component diagram model...");
                    return handleGenerateComponentDiagramCommand(params);
                }
                case DdslCommandIds.GENERATE_EVENT_FLOW_DIAGRAM -> {
                    server.logMessage(MessageType.Info, "Generating event-flow diagram model...");
                    return handleGenerateEventFlowDiagramCommand(params);
                }
                default -> {
                    log.warn("Unknown command: {}", command);
                    return null;
                }
            }
        });
    }

    private DiagramResponse handleGenerateComponentDiagramCommand(ExecuteCommandParams params) {
        log.info("Param for component diagram command: {}", params);
        String uri = extractUri(params.getArguments());
        log.info("Parameters for component diagram command: URI={}", uri);
        if (uri == null || uri.isBlank()) {
            log.warn("Component diagram generation failed: missing URI argument");
            return DiagramResponse.failure("component", "Missing file URI argument for ddsl.generateComponentDiagram");
        }

        String content = resolveDocumentContent(uri);
        log.info("Resolved document content for component diagram URI {}: {} characters",
                uri, content != null ? content.length() : "null");
        if (content == null || content.isBlank()) {
            log.warn("Component diagram generation failed: document content empty for URI {}", uri);
            return DiagramResponse.failure("component", "Document content is empty or unavailable for URI: " + uri);
        }

        DiagramResponse response = diagramService.generateComponentDiagram(content, uri);
        log.info("Component diagram result for URI {}: success={}, errors={}",
                uri, response.success(), response.errors().size());
        log.info("Component diagram result type for URI {}: {}", uri, response.diagramType());
        log.info("Component diagram response payload for URI {}: {}", uri, toLogPayload(response));
        if (response.success()) {
            server.showMessage(MessageType.Info, "Component diagram model generated successfully");
        } else {
            log.warn("Component diagram generation errors for URI {}: {}", uri, response.errors());
            server.showMessage(MessageType.Error, "Component diagram generation failed");
        }
        return response;
    }

    private DiagramResponse handleGenerateEventFlowDiagramCommand(ExecuteCommandParams params) {
        log.info("Param for event-flow diagram command: {}", params);
        String uri = extractUri(params.getArguments());
        log.info("Parameters for event-flow diagram command: URI={}", uri);
        if (uri == null || uri.isBlank()) {
            log.warn("Event-flow diagram generation failed: missing URI argument");
            return DiagramResponse.failure("eventFlow", "Missing file URI argument for ddsl.generateEventFlowDiagram");
        }

        String content = resolveDocumentContent(uri);
        log.info("Resolved document content for event-flow diagram URI {}: {} characters",
                uri, content != null ? content.length() : "null");
        if (content == null || content.isBlank()) {
            log.warn("Event-flow diagram generation failed: document content empty for URI {}", uri);
            return DiagramResponse.failure("eventFlow", "Document content is empty or unavailable for URI: " + uri);
        }

        DiagramResponse response = diagramService.generateEventFlowDiagram(content, uri);
        log.info("Event-flow diagram result for URI {}: success={}, errors={}",
                uri, response.success(), response.errors().size());
        log.info("Event-flow diagram result type for URI {}: {}", uri, response.diagramType());
        log.info("Event-flow diagram response payload for URI {}: {}", uri, toLogPayload(response));
        if (response.success()) {
            server.showMessage(MessageType.Info, "Event-flow diagram model generated successfully");
        } else {
            log.warn("Event-flow diagram generation errors for URI {}: {}", uri, response.errors());
            server.showMessage(MessageType.Error, "Event-flow diagram generation failed");
        }
        return response;
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
            String writtenTarget = persistCompileArtifacts(uri, response, params.getArguments());
            if (writtenTarget != null) {
                server.showMessage(MessageType.Info,
                        "Compile successful: " + response.artifacts().size() + " artifact(s), written to " + writtenTarget);
            } else {
                server.showMessage(MessageType.Info,
                        "Compile successful: " + response.artifacts().size() + " artifact(s)");
            }
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

    private String persistCompileArtifacts(String sourceUri, CompileResponse response, List<Object> args) {
        if (response == null || !response.success() || response.artifacts().isEmpty()) {
            return null;
        }

        Path outputDir = resolveOutputDir(sourceUri, args);
        if (outputDir == null) {
            return null;
        }

        try {
            List<CodeArtifact> artifacts = response.artifacts().stream()
                    .map(this::toCodeArtifact)
                    .toList();

            var writeResult = projectWriter.writeAll(artifacts, outputDir);
            if (!writeResult.isSuccess()) {
                log.warn("Generated sources write completed with failures: {} failed", writeResult.filesFailed());
            }
            return outputDir.toAbsolutePath().toString();
        } catch (Exception e) {
            log.warn("Failed to persist generated artifacts: {}", e.getMessage());
            return null;
        }
    }

    private Path resolveOutputDir(String sourceUri, List<Object> args) {
        String configuredOutput = extractOutputDir(args);
        if (configuredOutput != null && !configuredOutput.isBlank()) {
            return Path.of(configuredOutput);
        }

        try {
            URI parsed = URI.create(sourceUri);
            if (!"file".equalsIgnoreCase(parsed.getScheme())) {
                return null;
            }
            Path sourcePath = Path.of(parsed);
            Path sourceParent = sourcePath.getParent();
            if (sourceParent == null) {
                return null;
            }
            return sourceParent.resolve("target/generated-sources/ddsl");
        } catch (Exception e) {
            log.debug("resolveOutputDir failed: {}", e.getMessage());
            return null;
        }
    }

    private CodeArtifact toCodeArtifact(CompileResponse.Artifact artifact) {
        return new CodeArtifact(
                stripJavaSuffix(artifact.fileName()),
                artifact.packageName(),
                artifact.sourceCode() != null ? artifact.sourceCode() : "",
                toArtifactType(artifact.type())
        );
    }

    private String stripJavaSuffix(String fileName) {
        if (fileName == null) {
            return "GeneratedType";
        }
        return fileName.endsWith(".java")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;
    }

    private CodeArtifact.ArtifactType toArtifactType(String type) {
        if (type == null || type.isBlank()) {
            return CodeArtifact.ArtifactType.CLASS;
        }
        try {
            return CodeArtifact.ArtifactType.valueOf(type);
        } catch (IllegalArgumentException ignored) {
            return CodeArtifact.ArtifactType.CLASS;
        }
    }

    private String extractUri(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }

        for (Object arg : args) {
            String uri = extractUriFromArgument(arg);
            if (uri != null && !uri.isBlank()) {
                return uri;
            }
        }

        return null;
    }

    private String toLogPayload(Object payload) {
        String json;
        try {
            json = gson.toJson(payload);
        } catch (Exception e) {
            return "<failed-to-serialize-payload: " + e.getClass().getSimpleName() + ">";
        }

        if (json == null) {
            return "null";
        }

        int maxLength = 12000;
        if (json.length() <= maxLength) {
            return json;
        }

        return json.substring(0, maxLength) + "... (truncated, length=" + json.length() + ")";
    }

    private String extractBasePackage(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return "com.example.domain";
        }

        for (Object arg : args) {
            if (arg instanceof Map<?, ?> map) {
                Object basePackage = map.get("basePackage");
                if (basePackage instanceof String s && !s.isBlank()) {
                    return s;
                }
            }

            try {
                var json = gson.toJsonTree(arg);
                if (json != null && json.isJsonObject()) {
                    var object = json.getAsJsonObject();
                    var basePackageEl = object.get("basePackage");
                    if (basePackageEl != null && !basePackageEl.isJsonNull()) {
                        String value = basePackageEl.getAsString();
                        if (!value.isBlank()) {
                            return value;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Try next argument
            }
        }

        return "com.example.domain";
    }

    private String extractOutputDir(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof Map<?, ?> map) {
                Object outputDir = map.get("outputDir");
                if (outputDir instanceof String s && !s.isBlank()) {
                    return s;
                }
            }

            try {
                var json = gson.toJsonTree(arg);
                if (json != null && json.isJsonObject()) {
                    var object = json.getAsJsonObject();
                    var outputDirEl = object.get("outputDir");
                    if (outputDirEl != null && !outputDirEl.isJsonNull()) {
                        String value = outputDirEl.getAsString();
                        if (!value.isBlank()) {
                            return value;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Try next argument
            }
        }

        return null;
    }

    private String extractUriFromArgument(Object arg) {
        if (arg == null) {
            return null;
        }

        if (arg instanceof String s) {
            return looksLikeUri(s) ? s : null;
        }

        if (arg instanceof Map<?, ?> map) {
            String uri = firstNonBlank(
                    map.get("uri"),
                    map.get("documentUri"),
                    map.get("textDocumentUri"),
                    map.get("external")
            );
            if (uri != null && looksLikeUri(uri)) {
                return uri;
            }
        }

        try {
            var json = gson.toJsonTree(arg);
            if (json == null || json.isJsonNull()) {
                return null;
            }

            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                String value = json.getAsString();
                return looksLikeUri(value) ? value : null;
            }

            if (json.isJsonObject()) {
                var object = json.getAsJsonObject();
                String uri = firstNonBlank(
                        jsonString(object, "uri"),
                        jsonString(object, "documentUri"),
                        jsonString(object, "textDocumentUri"),
                        jsonString(object, "external")
                );
                return looksLikeUri(uri) ? uri : null;
            }
        } catch (Exception e) {
            log.debug("extractUriFromArgument failed: {}", e.getMessage());
        }

        return null;
    }

    private boolean looksLikeUri(String value) {
        return value != null && value.contains("://");
    }

    private String jsonString(com.google.gson.JsonObject object, String key) {
        var element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(Object... candidates) {
        for (Object candidate : candidates) {
            if (candidate instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
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
