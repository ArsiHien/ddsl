package uet.ndh.ddsl.lsp.stdio;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import uet.ndh.ddsl.lsp.DdslLanguageServer;
import uet.ndh.ddsl.lsp.core.DdslLanguageServerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Standalone STDIO launcher for DDSL Language Server.
 *
 * <p>This entry point is intended for native editor integrations such as
 * VS Code LanguageClient using stdio transport.
 */
@Slf4j
public final class DdslStdioLanguageServerMain {

    private DdslStdioLanguageServerMain() {
    }

    static void main() {
        DdslLanguageServer server = DdslLanguageServerFactory.createProcess();

        Launcher<LanguageClient> launcher = Launcher.createLauncher(
            server,
            LanguageClient.class,
            System.in,
            System.out
        );

        server.connect(launcher.getRemoteProxy());
        log.info("DDSL Language Server (STDIO) started");

        try {
            launcher.startListening().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("DDSL Language Server (STDIO) interrupted");
        } catch (ExecutionException e) {
            log.error("DDSL Language Server (STDIO) terminated with error", e);
        }
    }
}
