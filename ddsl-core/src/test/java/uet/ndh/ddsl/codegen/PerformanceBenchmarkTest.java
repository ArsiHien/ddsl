package uet.ndh.ddsl.codegen;

import org.junit.jupiter.api.*;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.compiler.metrics.CodeAnalyzer;
import uet.ndh.ddsl.compiler.metrics.CompilationMetrics;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and throughput benchmarks for the DDSL compilation pipeline.
 *
 * <p>These tests measure wall-clock time of each compiler phase and assert
 * that they stay within reasonable bounds. They are intentionally generous
 * (seconds, not milliseconds) so they pass in CI but still catch dramatic
 * regressions.</p>
 *
 * <p>For micro-benchmarking, consider adding JMH later.</p>
 */
class PerformanceBenchmarkTest {

    private static final String BASE_PACKAGE = "com.example.perf";

    // ─── Helpers ────────────────────────────────────────────────────────

    private DomainModel parse(String ddsl) throws ParseException {
        var parser = new DdslParser(ddsl, "<perf-test>");
        return parser.parse();
    }

    /**
     * Builds a moderately complex DDSL model with N aggregates, each
     * having 3 fields, 1 nested entity, and 1 value object.
     */
    private String buildLargeModel(int aggregateCount) {
        var sb = new StringBuilder();
        sb.append("BoundedContext PerfTest {\n");
        sb.append("    domain {\n");

        for (int i = 0; i < aggregateCount; i++) {
            sb.append("""
                        Aggregate Agg%d {
                            @identity id%d: UUID
                            name%d: String
                            amount%d: Decimal
                            
                            Entity Child%d {
                                @identity childId%d: UUID
                                label%d: String
                            }
                        }
                    """.formatted(i, i, i, i, i, i, i));
        }

        sb.append("    }\n");

        // Add value objects
        sb.append("    domain {\n");
        for (int i = 0; i < aggregateCount; i++) {
            sb.append("""
                        ValueObject VO%d {
                            field1_%d: String
                            field2_%d: Int
                        }
                    """.formatted(i, i, i));
        }
        sb.append("    }\n");

        // Add events
        sb.append("    events {\n");
        for (int i = 0; i < aggregateCount; i++) {
            sb.append("""
                        DomainEvent Event%d {
                            entityId%d: UUID
                            timestamp%d: DateTime
                        }
                    """.formatted(i, i, i));
        }
        sb.append("    }\n");

        sb.append("}\n");
        return sb.toString();
    }

    // ─── 1. Parser throughput ───────────────────────────────────────────

    @Test
    @DisplayName("Parse a 50-aggregate model within 5 seconds")
    void parsePerformance() throws ParseException {
        String ddsl = buildLargeModel(50);

        long start = System.nanoTime();
        DomainModel model = parse(ddsl);
        long elapsed = System.nanoTime() - start;

        Duration duration = Duration.ofNanos(elapsed);
        System.out.printf("⏱ Parse (50 aggregates): %d ms%n", duration.toMillis());

        assertNotNull(model);
        assertTrue(duration.toSeconds() < 5,
                "Parsing should complete in < 5s, took " + duration.toMillis() + " ms");
    }

    // ─── 2. Code generation throughput ──────────────────────────────────

    @Test
    @DisplayName("Generate code for 50-aggregate model within 5 seconds")
    void codegenPerformance() throws ParseException {
        String ddsl = buildLargeModel(50);
        DomainModel model = parse(ddsl);

        long start = System.nanoTime();
        var poet = new PoetModule(BASE_PACKAGE);
        List<CodeArtifact> artifacts = poet.generateFromModel(model);
        long elapsed = System.nanoTime() - start;

        Duration duration = Duration.ofNanos(elapsed);
        System.out.printf("⏱ CodeGen (50 aggregates): %d ms  → %d artifacts%n",
                duration.toMillis(), artifacts.size());

        assertFalse(artifacts.isEmpty());
        assertTrue(duration.toSeconds() < 5,
                "Code generation should complete in < 5s, took " + duration.toMillis() + " ms");
    }

    // ─── 3. End-to-end throughput ───────────────────────────────────────

    @Test
    @DisplayName("End-to-end compile (parse+gen) for 100 aggregates within 10 seconds")
    void endToEndPerformance() throws ParseException {
        String ddsl = buildLargeModel(100);

        long start = System.nanoTime();
        DomainModel model = parse(ddsl);
        var poet = new PoetModule(BASE_PACKAGE);
        List<CodeArtifact> artifacts = poet.generateFromModel(model);
        long elapsed = System.nanoTime() - start;

        Duration duration = Duration.ofNanos(elapsed);
        long totalLOC = artifacts.stream()
                .mapToLong(a -> a.sourceCode().lines().count())
                .sum();

        System.out.printf("⏱ End-to-end (100 agg): %d ms  → %d artifacts, %,d LOC%n",
                duration.toMillis(), artifacts.size(), totalLOC);

        assertTrue(duration.toSeconds() < 10,
                "End-to-end should complete in < 10s, took " + duration.toMillis() + " ms");
    }

    // ─── 4. Scaling linearity (rough check) ─────────────────────────────

    @Test
    @DisplayName("Codegen time scales roughly linearly (2x input ≤ 4x time)")
    void scalingLinearity() throws ParseException {
        String small = buildLargeModel(20);
        String large = buildLargeModel(40);

        // Warm-up JIT
        generate(small);

        long startSmall = System.nanoTime();
        generate(small);
        long elapsedSmall = System.nanoTime() - startSmall;

        long startLarge = System.nanoTime();
        generate(large);
        long elapsedLarge = System.nanoTime() - startLarge;

        double ratio = (double) elapsedLarge / elapsedSmall;
        System.out.printf("⏱ Scaling: 20 agg=%d ms, 40 agg=%d ms, ratio=%.2fx%n",
                Duration.ofNanos(elapsedSmall).toMillis(),
                Duration.ofNanos(elapsedLarge).toMillis(),
                ratio);

        assertTrue(ratio < 4.0,
                "2x aggregates should take at most 4x time (sub-quadratic). Actual ratio: " + ratio);
    }

    // ─── 5. CompilationMetrics integration ──────────────────────────────

    @Test
    @DisplayName("CompilationMetrics correctly tracks phase timings")
    void metricsTracking() throws ParseException {
        CompilationMetrics metrics = new CompilationMetrics();

        // Simulate phases
        try (var timer = metrics.startPhase("parse")) {
            parse(buildLargeModel(10));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        String ddsl = buildLargeModel(10);
        DomainModel model = parse(ddsl);
        List<CodeArtifact> artifacts;

        try (var timer = metrics.startPhase("codegen")) {
            var poet = new PoetModule(BASE_PACKAGE);
            artifacts = poet.generateFromModel(model);
        }

        // Analyze domain model
        CodeAnalyzer.analyzeDomainModel(model, metrics);
        CodeAnalyzer.analyzeCodeArtifacts(artifacts, metrics);

        // Verify metrics were recorded
        assertTrue(metrics.getTimings().containsKey("parse"),
                "Should record parse timing");
        assertTrue(metrics.getTimings().containsKey("codegen"),
                "Should record codegen timing");
        assertTrue(metrics.getTimings().get("parse").toMillis() >= 0);
        assertTrue(metrics.getTimings().get("codegen").toMillis() >= 0);

        // Domain model metrics
        assertNotNull(metrics.getMetadata().get("model_total_aggregates"));

        // Print the report for visual inspection
        metrics.printReport();
    }

    // ─── 6. CodeAnalyzer domain complexity ──────────────────────────────

    @Test
    @DisplayName("CodeAnalyzer produces correct domain model counts")
    void codeAnalyzerDomainCounts() throws ParseException {
        String ddsl = """
                BoundedContext Shop {
                    domain {
                        Aggregate Order {
                            @identity orderId: UUID
                            
                            Entity OrderItem {
                                @identity itemId: UUID
                            }
                        }
                        Aggregate Product {
                            @identity productId: UUID
                        }
                        ValueObject Money {
                            amount: Decimal
                            currency: String
                        }
                    }
                    events {
                        DomainEvent OrderPlaced {
                            orderId: UUID
                        }
                    }
                    repositories {
                        Repository OrderRepo for Order {
                            findById(id: UUID): Order?
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        CompilationMetrics metrics = new CompilationMetrics();
        CodeAnalyzer.analyzeDomainModel(model, metrics);

        assertEquals(2L, metrics.getCounters().get("aggregates_in_Shop"),
                "Should count 2 aggregates in Shop");
        assertEquals(1L, (long) (int) metrics.getMetadata().get("model_bounded_contexts"),
                "Should count 1 bounded context");
    }

    // ─── 7. CodeAnalyzer LOC counting ───────────────────────────────────

    @Test
    @DisplayName("CodeAnalyzer correctly counts synthetic LOC")
    void codeAnalyzerLOC() throws ParseException {
        String ddsl = """
                BoundedContext Demo {
                    domain {
                        Aggregate Foo {
                            @identity fooId: UUID
                            bar: String
                        }
                    }
                }
                """;

        DomainModel model = parse(ddsl);
        var poet = new PoetModule(BASE_PACKAGE);
        List<CodeArtifact> artifacts = poet.generateFromModel(model);

        CompilationMetrics metrics = new CompilationMetrics();
        CodeAnalyzer.analyzeCodeArtifacts(artifacts, metrics);

        assertTrue(metrics.getSyntheticLOC() > 0,
                "Should count some lines of generated code");
        assertTrue(metrics.getFilesGenerated() > 0,
                "Should count generated files");
    }

    // ─── 8. Memory pressure (smoke test) ────────────────────────────────

    @Test
    @DisplayName("Compiling a large model does not OOM (200 aggregates)")
    void memoryPressure() {
        String ddsl = buildLargeModel(200);

        assertDoesNotThrow(() -> {
            DomainModel model = parse(ddsl);
            var poet = new PoetModule(BASE_PACKAGE);
            List<CodeArtifact> artifacts = poet.generateFromModel(model);
            assertTrue(artifacts.size() > 200,
                    "Should generate many artifacts for 200 aggregates");
        }, "Should handle 200 aggregates without OOM");
    }

    // ─── 9. Repeated compilation stability ──────────────────────────────

    @RepeatedTest(5)
    @DisplayName("Repeated compilation produces identical output")
    void deterministicOutput() throws ParseException {
        String ddsl = """
                BoundedContext Stable {
                    domain {
                        Aggregate Widget {
                            @identity widgetId: UUID
                            label: String
                        }
                    }
                }
                """;

        List<CodeArtifact> first  = generate(ddsl);
        List<CodeArtifact> second = generate(ddsl);

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).typeName(), second.get(i).typeName());
            assertEquals(first.get(i).sourceCode(), second.get(i).sourceCode(),
                    "Artifact " + first.get(i).typeName() + " should be deterministic");
        }
    }

    // ─── Internal ───────────────────────────────────────────────────────

    private List<CodeArtifact> generate(String ddsl) throws ParseException {
        DomainModel model = parse(ddsl);
        var poet = new PoetModule(BASE_PACKAGE);
        return poet.generateFromModel(model);
    }
}
