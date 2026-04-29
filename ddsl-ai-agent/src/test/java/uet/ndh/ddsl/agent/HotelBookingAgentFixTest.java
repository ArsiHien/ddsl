package uet.ndh.ddsl.agent;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.ProjectWriter;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.parser.DdslParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM Agent Syntax-Fix Pipeline Test for Hotel Booking DDSL.
 *
 * <p>Feeds {@code samples/hotel-booking-broken.ddsl} through the real
 * {@link NlToDslService} (Gemini + Qdrant RAG + self-healing loop),
 * measures agent wall-clock time, verifies the fixed output parses
 * without errors, generates code, and writes complete Java files to disk.</p>
 *
 * <h3>Pipeline</h3>
 * <pre>
 *   broken.ddsl → NlToDslService.translate() → fixed DSL
 *                                                 ↓
 *                              DdslParser.parse() → no errors?
 *                                                 ↓
 *                              PoetModule.generateFromModel() → artifacts
 *                                                 ↓
 *                              ProjectWriter.writeAll() → files on disk
 * </pre>
 *
 * <p>Requires live Gemini API + Qdrant. Run with {@code @Tag("live")}.</p>
 */
@SpringBootTest
@ActiveProfiles("local")
@Tag("live")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HotelBookingAgentFixTest {

    private static final String BASE_PACKAGE = "com.hotel.booking";
    private static final String BROKEN_FILE = "../samples/hotel-booking-broken.ddsl";
    private static final String CORRECT_FILE = "../samples/hotel-booking.ddsl";
    private static final String OUTPUT_DIR = "build/generated-test/hotel-booking-agent-fixed";

    @Autowired
    private NlToDslService nlToDslService;

    // Shared across ordered tests
    private static NlToDslResult agentResult;
    private static long agentDurationMs;

    // ═════════════════════════════════════════════════════════════════════
    //  Test 1: Agent fixes the broken DDSL
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Agent fixes broken hotel-booking DDSL (measures time, checks parse = 0 errors)")
    void agentFixesBrokenDdsl() throws IOException {
        String brokenSource = Files.readString(Path.of(BROKEN_FILE));
        long brokenLines = brokenSource.lines().count();

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  Hotel Booking Agent Fix — Starting                              ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Input:  %-57s ║%n", BROKEN_FILE);
        System.out.printf("║  Size:   %,d bytes, %d lines %34s ║%n",
                brokenSource.length(), brokenLines, "");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.println("║  Calling NlToDslService.translate() ...                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");

        // ── Call the agent ───────────────────────────────────────────────
        long t0 = System.nanoTime();
        agentResult = nlToDslService.translate(brokenSource, 5);
        long t1 = System.nanoTime();
        agentDurationMs = (t1 - t0) / 1_000_000;

        // ── Report ──────────────────────────────────────────────────────
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  Hotel Booking Agent Fix — Result                                ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Success:       %-49s ║%n", agentResult.success() ? "✅ YES" : "❌ NO");
        System.out.printf("║  Retries:       %-49d ║%n",
                agentResult.retrieverRetries() + agentResult.synthesizerRetries());
        System.out.printf("║  Agent time:    %,d ms %44s ║%n", agentDurationMs, "");
        System.out.printf("║  Output size:   %,d chars %40s ║%n",
                agentResult.dsl().length(), "");

        if (!agentResult.errors().isEmpty()) {
            System.out.println("╠════════════════════════════════════════════════════════════════════╣");
            System.out.println("║  Remaining errors:                                               ║");
            for (int i = 0; i < agentResult.errors().size(); i++) {
                System.out.printf("║  %2d. %-61s ║%n", i + 1,
                        truncate(agentResult.errors().get(i), 61));
            }
        }
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");

        // ── Save the fixed DSL to disk ──────────────────────────────────
        if (agentResult.success()) {
            Path fixedDslPath = Path.of("build/generated-test/hotel-booking-fixed.ddsl");
            Files.createDirectories(fixedDslPath.getParent());
            Files.writeString(fixedDslPath, agentResult.dsl(), StandardCharsets.UTF_8);
            System.out.println("📄 Fixed DSL saved to: " + fixedDslPath.toAbsolutePath());
        }

        // ── Assert ──────────────────────────────────────────────────────
        assertTrue(agentResult.success(),
                "Agent should fix the broken DDSL. Errors:\n"
                        + String.join("\n", agentResult.errors()));
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Test 2: Parse the fixed DSL → generate code → write files
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("Fixed DSL → parse → codegen → write files to disk")
    void fixedDslToCompleteFiles() throws Exception {
        assertNotNull(agentResult, "Test 1 must run first");
        assertTrue(agentResult.success(), "Test 1 must have succeeded");

        Map<String, Long> timings = new LinkedHashMap<>();
        long t0, t1;

        // ── Parse ───────────────────────────────────────────────────────
        t0 = System.nanoTime();
        var parser = new DdslParser(agentResult.dsl(), "hotel-booking-fixed.ddsl");
        var model = parser.parse();
        t1 = System.nanoTime();
        timings.put("Parse", t1 - t0);

        assertNotNull(model);
        assertFalse(model.boundedContexts().isEmpty(), "Should have ≥1 bounded context");

        // ── Codegen ─────────────────────────────────────────────────────
        t0 = System.nanoTime();
        var poet = new PoetModule(BASE_PACKAGE);
        List<CodeArtifact> artifacts = poet.generateFromModel(model);
        t1 = System.nanoTime();
        timings.put("Codegen", t1 - t0);

        assertFalse(artifacts.isEmpty(), "Should produce artifacts");

        // ── Write to disk ───────────────────────────────────────────────
        Path outputDir = Path.of(OUTPUT_DIR).toAbsolutePath();
        cleanDir(outputDir);
        Files.createDirectories(outputDir);

        t0 = System.nanoTime();
        var writer = new ProjectWriter(ProjectWriter.WriterConfig.defaults());
        var writeResult = writer.writeAll(artifacts, outputDir);
        t1 = System.nanoTime();
        timings.put("Write to disk", t1 - t0);

        assertEquals(0, writeResult.filesFailed(), "No writes should fail");

        // ── File-system stats ───────────────────────────────────────────
        long totalFilesOnDisk;
        long totalBytesOnDisk;
        try (Stream<Path> walk = Files.walk(outputDir)) {
            var files = walk.filter(Files::isRegularFile).toList();
            totalFilesOnDisk = files.size();
            totalBytesOnDisk = files.stream()
                    .mapToLong(p -> { try { return Files.size(p); } catch (IOException e) { return 0; } })
                    .sum();
        }

        int totalLines = artifacts.stream()
                .mapToInt(a -> (int) a.sourceCode().lines().count())
                .sum();

        long pipelineNanos = timings.values().stream().mapToLong(Long::longValue).sum();

        // ── Report ──────────────────────────────────────────────────────
        Map<CodeArtifact.ArtifactType, List<CodeArtifact>> byType = artifacts.stream()
                .collect(Collectors.groupingBy(CodeArtifact::artifactType));

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  Hotel Booking Agent Fix — Full Pipeline Performance             ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Agent (NlToDslService):              %,10d ms              ║%n", agentDurationMs);
        for (var entry : timings.entrySet()) {
            System.out.printf("║  %-38s %10.3f ms              ║%n",
                    entry.getKey(), entry.getValue() / 1_000_000.0);
        }
        System.out.printf("║  %-38s %10.3f ms              ║%n",
                "Pipeline (parse+codegen+write)", pipelineNanos / 1_000_000.0);
        System.out.printf("║  %-38s %,10d ms              ║%n",
                "TOTAL (agent + pipeline)", agentDurationMs + (pipelineNanos / 1_000_000));
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Agent retries:        %-43d ║%n",
                agentResult.retrieverRetries() + agentResult.synthesizerRetries());
        System.out.printf("║  Artifacts generated:  %-43d ║%n", artifacts.size());
        System.out.printf("║  Files on disk:        %-43d ║%n", totalFilesOnDisk);
        System.out.printf("║  Total lines of code:  %-43d ║%n", totalLines);
        System.out.printf("║  Total bytes on disk:  %,-43d ║%n", totalBytesOnDisk);
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");

        // Artifact type breakdown
        for (var entry : byType.entrySet()) {
            System.out.printf("║  %-22s %3d artifact(s)                         ║%n",
                    entry.getKey(), entry.getValue().size());
            for (CodeArtifact a : entry.getValue()) {
                System.out.printf("║    📄 %-60s ║%n", a.relativePath());
            }
        }
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");

        // File listing
        System.out.println();
        System.out.println("┌─── Files on Disk ───────────────────────────────────────────────┐");
        try (Stream<Path> walk = Files.walk(outputDir)) {
            walk.filter(Files::isRegularFile).sorted().forEach(p -> {
                String rel = outputDir.relativize(p).toString();
                try {
                    System.out.printf("│  %-56s %6d B │%n", rel, Files.size(p));
                } catch (IOException e) {
                    System.out.printf("│  %-56s   ERR   │%n", rel);
                }
            });
        }
        System.out.println("└──────────────────────────────────────────────────────────────────┘");

        // ── Assertions ──────────────────────────────────────────────────
        assertTrue(artifacts.size() >= 10,
                "Expected ≥10 artifacts, got " + artifacts.size());
        assertEquals(artifacts.size(), totalFilesOnDisk,
                "Files on disk must match artifact count");

        System.out.println();
        System.out.println("✅ Full pipeline complete: broken DDSL → agent fix → " +
                artifacts.size() + " Java files on disk in " + OUTPUT_DIR);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Test 3: Compare agent output with reference (correct) DDSL
    // ═════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("Compare agent-fixed output with reference hotel-booking.ddsl")
    void compareWithReference() throws Exception {
        assertNotNull(agentResult, "Test 1 must run first");
        assertTrue(agentResult.success(), "Test 1 must have succeeded");

        // Generate from fixed DSL
        var fixedParser = new DdslParser(agentResult.dsl(), "hotel-booking-fixed.ddsl");
        var fixedModel = fixedParser.parse();
        var fixedPoet = new PoetModule(BASE_PACKAGE);
        List<CodeArtifact> fixedArtifacts = fixedPoet.generateFromModel(fixedModel);

        // Generate from reference DSL
        String refSource = Files.readString(Path.of(CORRECT_FILE));
        var refParser = new DdslParser(refSource, "hotel-booking.ddsl");
        var refModel = refParser.parse();
        var refPoet = new PoetModule(BASE_PACKAGE);
        List<CodeArtifact> refArtifacts = refPoet.generateFromModel(refModel);

        // Compare artifact names
        Set<String> refNames = refArtifacts.stream()
                .map(CodeArtifact::fullyQualifiedName).collect(Collectors.toSet());
        Set<String> fixedNames = fixedArtifacts.stream()
                .map(CodeArtifact::fullyQualifiedName).collect(Collectors.toSet());

        Set<String> missing = new TreeSet<>(refNames);
        missing.removeAll(fixedNames);
        Set<String> extra = new TreeSet<>(fixedNames);
        extra.removeAll(refNames);

        // Compare AST structure (HotelBooking context)
        var refCtx = refModel.boundedContexts().stream()
                .filter(c -> c.name().equals("HotelBooking")).findFirst().orElse(null);
        var fixedCtx = fixedModel.boundedContexts().stream()
                .filter(c -> c.name().equals("HotelBooking")).findFirst().orElse(null);

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  Comparison: Agent-Fixed vs Reference                            ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Reference artifacts:  %-43d ║%n", refArtifacts.size());
        System.out.printf("║  Fixed artifacts:      %-43d ║%n", fixedArtifacts.size());
        System.out.printf("║  Missing from fixed:   %-43d ║%n", missing.size());
        System.out.printf("║  Extra in fixed:       %-43d ║%n", extra.size());

        if (refCtx != null && fixedCtx != null) {
            System.out.println("╠════════════════════════════════════════════════════════════════════╣");
            System.out.println("║  HotelBooking Context        Reference    Fixed                   ║");
            System.out.printf("║    Aggregates:                %5d       %5d                    ║%n",
                    refCtx.aggregates().size(), fixedCtx.aggregates().size());
            System.out.printf("║    Value Objects:             %5d       %5d                    ║%n",
                    refCtx.valueObjects().size(), fixedCtx.valueObjects().size());
            System.out.printf("║    Domain Events:             %5d       %5d                    ║%n",
                    refCtx.domainEvents().size(), fixedCtx.domainEvents().size());
            System.out.printf("║    Repositories:              %5d       %5d                    ║%n",
                    refCtx.repositories().size(), fixedCtx.repositories().size());
            System.out.printf("║    Domain Services:           %5d       %5d                    ║%n",
                    refCtx.domainServices().size(), fixedCtx.domainServices().size());
            System.out.printf("║    Specifications:            %5d       %5d                    ║%n",
                    refCtx.specifications().size(), fixedCtx.specifications().size());
            System.out.printf("║    Factories:                 %5d       %5d                    ║%n",
                    refCtx.factories().size(), fixedCtx.factories().size());
        }
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");

        if (!missing.isEmpty()) {
            System.out.println("\n⚠ Missing from agent output:");
            missing.forEach(n -> System.out.println("  ✗ " + n));
        }
        if (!extra.isEmpty()) {
            System.out.println("\nℹ Extra in agent output:");
            extra.forEach(n -> System.out.println("  + " + n));
        }

        // ── Assertions ──────────────────────────────────────────────────
        assertNotNull(fixedCtx, "Fixed model must contain HotelBooking context");
        assertTrue(missing.isEmpty(),
                "Agent output is missing artifacts:\n" + String.join("\n", missing));

        System.out.println("\n✅ Agent output matches reference.");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Utilities
    // ═════════════════════════════════════════════════════════════════════

    private static void cleanDir(Path dir) throws IOException {
        if (dir != null && Files.exists(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
