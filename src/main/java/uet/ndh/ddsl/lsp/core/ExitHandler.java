package uet.ndh.ddsl.lsp.core;

/**
 * Strategy interface for handling LSP exit behavior.
 *
 * <p>Different transports can control shutdown semantics differently:
 * embedded server transports usually should not terminate the JVM,
 * while stdio process mode should exit with an explicit code.
 */
@FunctionalInterface
public interface ExitHandler {

    /**
     * Handle process/server exit.
     *
     * @param shutdownReceived true when a graceful shutdown request was received before exit
     */
    void exit(boolean shutdownReceived);

    static ExitHandler noOp() {
        return shutdownReceived -> {
            // Intentionally no-op for embedded transports.
        };
    }
}
