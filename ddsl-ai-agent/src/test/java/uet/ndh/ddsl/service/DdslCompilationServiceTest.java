package uet.ndh.ddsl.service;

import org.junit.jupiter.api.*;
import uet.ndh.ddsl.controller.dto.CompileResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DdslCompilationService} — the orchestrator of
 * Parse → CodeGen → Response.
 *
 * No Spring context needed; the service is a plain POJO.
 */
class DdslCompilationServiceTest {

    private final DdslCompilationService service = new DdslCompilationService();
    private static final String PKG = "com.test.domain";

    // ─── Happy path ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Successful compilation returns artifacts")
    void successfulCompile() {
        String ddsl = """
                BoundedContext Store {
                    domain {
                        Aggregate Product {
                            @identity productId: UUID
                            name: String
                            price: Decimal
                        }
                        ValueObject Category {
                            name: String
                        }
                    }
                }
                """;

        CompileResponse resp = service.compile(ddsl, PKG);

        assertTrue(resp.success(), "Should succeed");
        assertFalse(resp.artifacts().isEmpty(), "Should produce artifacts");
        assertTrue(resp.errors().isEmpty(), "No errors expected");

        // Verify artifact structure
        for (CompileResponse.Artifact a : resp.artifacts()) {
            assertNotNull(a.fileName());
            assertNotNull(a.packageName());
            assertNotNull(a.type());
            assertNotNull(a.sourceCode());
            assertFalse(a.sourceCode().isEmpty());
        }
    }

    // ─── Parse errors ───────────────────────────────────────────────────

    @Test
    @DisplayName("Invalid DDSL returns failure with error messages")
    void parseFailure() {
        String ddsl = "this is not valid DDSL syntax {{{";

        CompileResponse resp = service.compile(ddsl, PKG);

        assertFalse(resp.success());
        assertTrue(resp.artifacts().isEmpty());
        assertFalse(resp.errors().isEmpty(),
                "Should contain parse error messages");
    }

    // ─── Edge: empty input ──────────────────────────────────────────────

    @Test
    @DisplayName("Empty DDSL input returns failure")
    void emptyInput() {
        CompileResponse resp = service.compile("", PKG);
        assertFalse(resp.success());
    }

    // ─── Artifact naming matches expectations ───────────────────────────

    @Test
    @DisplayName("Artifact file names end with .java")
    void artifactFileNames() {
        String ddsl = """
                BoundedContext Demo {
                    domain {
                        Aggregate Order {
                            @identity orderId: UUID
                        }
                    }
                }
                """;

        CompileResponse resp = service.compile(ddsl, PKG);
        assertTrue(resp.success());

        for (CompileResponse.Artifact a : resp.artifacts()) {
            assertTrue(a.fileName().endsWith(".java"),
                    "File " + a.fileName() + " should end with .java");
        }
    }

    // ─── Base package propagation ───────────────────────────────────────

    @Test
    @DisplayName("Artifacts use the specified base package")
    void basePackagePropagation() {
        String custom = "org.acme.myapp";
        String ddsl = """
                BoundedContext X {
                    domain {
                        Aggregate Y {
                            @identity yId: UUID
                        }
                    }
                }
                """;

        CompileResponse resp = service.compile(ddsl, custom);
        assertTrue(resp.success());

        for (CompileResponse.Artifact a : resp.artifacts()) {
            assertTrue(a.packageName().startsWith(custom),
                    a.packageName() + " should start with " + custom);
        }
    }

    // ─── Performance: compile should be fast ────────────────────────────

    @Test
    @DisplayName("Compilation of a simple model completes in < 3 seconds")
    void compilePerformance() {
        String ddsl = """
                BoundedContext Quick {
                    domain {
                        Aggregate Fast {
                            @identity id: UUID
                            data: String
                        }
                    }
                }
                """;

        long start = System.nanoTime();
        CompileResponse resp = service.compile(ddsl, PKG);
        long elapsed = (System.nanoTime() - start) / 1_000_000;

        assertTrue(resp.success());
        assertTrue(elapsed < 3000,
                "Compile should take < 3s, took " + elapsed + " ms");
    }
}
