package uet.ndh.ddsl.lsp.core;

import uet.ndh.ddsl.lsp.DdslLanguageServer;

/**
 * Factory for creating language-server instances with transport-specific lifecycle behavior.
 */
public final class DdslLanguageServerFactory {

    private DdslLanguageServerFactory() {
    }

    /**
     * Embedded server variant (WebSocket/Spring): never terminates host JVM on {@code exit}.
     */
    public static DdslLanguageServer createEmbedded() {
        return new DdslLanguageServer(ExitHandler.noOp());
    }

    /**
     * Standalone process variant (stdio/tcp): exits process with LSP-compliant code.
     */
    public static DdslLanguageServer createProcess() {
        return new DdslLanguageServer(shutdownReceived -> System.exit(shutdownReceived ? 0 : 1));
    }
}
