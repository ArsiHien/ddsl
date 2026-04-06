package uet.ndh.ddsl.codegen;

import org.junit.jupiter.api.*;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.parser.DdslParser;
import uet.ndh.ddsl.parser.ParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end pipeline test for the Hotel Booking DDSL sample.
 *
 * <p>Goes from <b>zero to complete files on disk</b>:
 * reads {@code samples/hotel-booking.ddsl}, parses it, generates every Java
 * artifact via JavaPoet, writes the full package/file tree to a temp directory,
 * verifies every file compiles-ready, and reports detailed per-phase
 * wall-clock timing + file-system statistics.</p>
 *
 * <h3>Pipeline Phases</h3>
 * <ol>
 *   <li>Load DDSL source</li>
 *   <li>Lex + Parse → AST</li>
 *   <li>Type registration + Code generation → CodeArtifacts</li>
 *   <li>Write artifacts to disk (packages + .java files)</li>
 *   <li>Verify file tree (existence, content, package declarations)</li>
 * </ol>
 */
class HotelBookingPipelineTest {

    private static final String BASE_PACKAGE = "com.hotel.booking";
    private static final String DDSL_FILE = "samples/hotel-booking.ddsl";

        /** Generated files are written here and preserved across test runs for inspection. */
    private static final String OUTPUT_DIR = "build/generated-test/hotel-booking";

    private String ddslSource;
    private Path outputDir;

    @BeforeEach
    void setUp() throws IOException {
        // Load the DDSL source
        Path path = Path.of(DDSL_FILE);
        assertTrue(Files.exists(path), "Sample file must exist: " + DDSL_FILE);
        ddslSource = Files.readString(path);

        // Use a persistent project-relative directory and preserve generated output across runs
        outputDir = Path.of(OUTPUT_DIR).toAbsolutePath();
        Files.createDirectories(outputDir);
    }

    // ─── Zero-to-complete-files pipeline ────────────────────────────────

    @Test
    @DisplayName("Hotel Booking: zero → complete files on disk with full performance metrics")
    void zeroToCompleteFiles() throws ParseException, IOException {
        Map<String, Long> timings = new LinkedHashMap<>();
        long t0, t1;

        // ════════════════════════════════════════════════════════════════
        // Phase 1: Load DDSL source
        // ════════════════════════════════════════════════════════════════
        t0 = System.nanoTime();
        byte[] rawBytes = ddslSource.getBytes(StandardCharsets.UTF_8);
        t1 = System.nanoTime();
        timings.put("1. Load source", t1 - t0);

        // ════════════════════════════════════════════════════════════════
        // Phase 2: Lexing + Parsing → AST
        // ════════════════════════════════════════════════════════════════
        t0 = System.nanoTime();
        var parser = new DdslParser(ddslSource, "hotel-booking.ddsl");
        DomainModel model = parser.parse();
        t1 = System.nanoTime();
        timings.put("2. Parse (lex + parse)", t1 - t0);

        assertNotNull(model);
        assertFalse(model.boundedContexts().isEmpty(), "Should parse at least 1 bounded context");

        // ════════════════════════════════════════════════════════════════
        // Phase 3: Type registration + Code Generation
        // ════════════════════════════════════════════════════════════════
        t0 = System.nanoTime();
        var poet = new PoetModule(BASE_PACKAGE);
        List<CodeArtifact> artifacts = poet.generateFromModel(model);
        t1 = System.nanoTime();
        timings.put("3. Code generation", t1 - t0);

        assertFalse(artifacts.isEmpty(), "Should produce artifacts");

        // ════════════════════════════════════════════════════════════════
        // Phase 4: Write all files to disk (packages + .java files)
        // ════════════════════════════════════════════════════════════════
        t0 = System.nanoTime();
        var writer = new ProjectWriter(ProjectWriter.WriterConfig.defaults());
        ProjectWriter.WriteResult writeResult = writer.writeAll(artifacts, outputDir);
        t1 = System.nanoTime();
        timings.put("4. Write to disk", t1 - t0);

        assertEquals(0, writeResult.filesFailed(), "No file writes should fail");
        assertEquals(artifacts.size(), writeResult.filesWritten(),
                "All artifacts should be written");

        // ════════════════════════════════════════════════════════════════
        // Phase 5: Verify file tree — every file exists & has content
        // ════════════════════════════════════════════════════════════════
        t0 = System.nanoTime();
        int verifiedCount = 0;
        List<String> verificationErrors = new ArrayList<>();

        for (CodeArtifact artifact : artifacts) {
            Path filePath = outputDir.resolve(artifact.relativePath());

            // File must exist
            if (!Files.exists(filePath)) {
                verificationErrors.add("MISSING: " + artifact.relativePath());
                continue;
            }

            // File content must match generated source
            String diskContent = Files.readString(filePath, StandardCharsets.UTF_8);
            if (!diskContent.equals(artifact.sourceCode())) {
                verificationErrors.add("CONTENT MISMATCH: " + artifact.relativePath());
                continue;
            }

            // Must contain package declaration
            if (!diskContent.contains("package ")) {
                verificationErrors.add("NO PACKAGE DECL: " + artifact.relativePath());
                continue;
            }

            // Must contain class/record/interface/enum
            if (!diskContent.contains("class ") && !diskContent.contains("record ")
                    && !diskContent.contains("interface ") && !diskContent.contains("enum ")) {
                verificationErrors.add("NO TYPE DECL: " + artifact.relativePath());
                continue;
            }

            verifiedCount++;
        }
        t1 = System.nanoTime();
        timings.put("5. Verify file tree", t1 - t0);

        // ════════════════════════════════════════════════════════════════
        // Phase 6: File-system statistics
        // ════════════════════════════════════════════════════════════════
        t0 = System.nanoTime();
        long totalFilesOnDisk;
        long totalDirsOnDisk;
        long totalBytesOnDisk;
        Set<String> packageDirs = new TreeSet<>();

        try (Stream<Path> walk = Files.walk(outputDir)) {
            List<Path> allPaths = walk.toList();
            totalFilesOnDisk = allPaths.stream().filter(Files::isRegularFile).count();
            totalDirsOnDisk = allPaths.stream().filter(Files::isDirectory).count() - 1; // exclude root
            totalBytesOnDisk = allPaths.stream()
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0; }
                    })
                    .sum();
        }

        // Collect package directories
        try (Stream<Path> walk = Files.walk(outputDir)) {
            walk.filter(Files::isDirectory)
                .filter(p -> !p.equals(outputDir))
                .forEach(p -> {
                    String rel = outputDir.relativize(p).toString().replace('/', '.');
                    packageDirs.add(rel);
                });
        }

        int totalLines = 0;
        int totalChars = 0;
        for (CodeArtifact a : artifacts) {
            totalLines += a.sourceCode().lines().count();
            totalChars += a.sourceCode().length();
        }
        t1 = System.nanoTime();
        timings.put("6. Collect statistics", t1 - t0);

        long totalNanos = timings.values().stream().mapToLong(Long::longValue).sum();
        timings.put("   TOTAL", totalNanos);

        // ════════════════════════════════════════════════════════════════
        //  Performance Report
        // ════════════════════════════════════════════════════════════════
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║    Hotel Booking — Zero to Complete Files — Performance Report    ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        for (var entry : timings.entrySet()) {
            System.out.printf("║  %-46s %10.3f ms  ║%n",
                    entry.getKey(), entry.getValue() / 1_000_000.0);
        }
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  DDSL source size:       %6d bytes  (%3d lines)               ║%n",
                rawBytes.length, ddslSource.lines().count());
        System.out.printf("║  Artifacts generated:    %6d                                   ║%n",
                artifacts.size());
        System.out.printf("║  Files on disk:          %6d                                   ║%n",
                totalFilesOnDisk);
        System.out.printf("║  Directories created:    %6d                                   ║%n",
                totalDirsOnDisk);
        System.out.printf("║  Total bytes on disk:    %6d                                   ║%n",
                totalBytesOnDisk);
        System.out.printf("║  Total lines of code:    %6d                                   ║%n",
                totalLines);
        System.out.printf("║  Total characters:       %6d                                   ║%n",
                totalChars);
        System.out.printf("║  Files verified OK:      %6d / %-6d                            ║%n",
                verifiedCount, artifacts.size());
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");

        // ── Package tree ────────────────────────────────────────────────
        System.out.println();
        System.out.println("┌─── Package Tree ────────────────────────────────────────────────┐");
        for (String pkg : packageDirs) {
            System.out.printf("│  📦 %-62s │%n", pkg);
        }
        System.out.println("└──────────────────────────────────────────────────────────────────┘");

        // ── Artifact type breakdown ─────────────────────────────────────
        Map<CodeArtifact.ArtifactType, List<CodeArtifact>> byType = artifacts.stream()
                .collect(Collectors.groupingBy(CodeArtifact::artifactType));

        System.out.println();
        System.out.println("┌─── Artifact Type Breakdown ─────────────────────────────────────┐");
        for (var entry : byType.entrySet()) {
            System.out.printf("│  %-22s  %3d artifact(s)                        │%n",
                    entry.getKey(), entry.getValue().size());
            for (CodeArtifact a : entry.getValue()) {
                System.out.printf("│    📄 %-60s │%n", a.relativePath());
            }
        }
        System.out.println("└──────────────────────────────────────────────────────────────────┘");

        // ── Directory listing on disk ───────────────────────────────────
        System.out.println();
        System.out.println("┌─── Files on Disk ───────────────────────────────────────────────┐");
        try (Stream<Path> walk = Files.walk(outputDir)) {
            walk.filter(Files::isRegularFile)
                .sorted()
                .forEach(p -> {
                    String rel = outputDir.relativize(p).toString();
                    try {
                        long size = Files.size(p);
                        System.out.printf("│  %-56s %6d B │%n", rel, size);
                    } catch (IOException e) {
                        System.out.printf("│  %-56s   ERR   │%n", rel);
                    }
                });
        }
        System.out.println("└──────────────────────────────────────────────────────────────────┘");

        // ── Print every generated file content ──────────────────────────
        for (CodeArtifact artifact : artifacts) {
            System.out.println();
            System.out.println("━".repeat(72));
            System.out.printf("  📄 %s  [%s]%n", artifact.relativePath(), artifact.artifactType());
            System.out.println("━".repeat(72));
            System.out.println(artifact.sourceCode());
        }

        // ── Verification errors ─────────────────────────────────────────
        if (!verificationErrors.isEmpty()) {
            System.out.println();
            System.out.println("⚠ Verification errors:");
            verificationErrors.forEach(e -> System.out.println("  ✗ " + e));
        }

        // ════════════════════════════════════════════════════════════════
        //  Assertions
        // ════════════════════════════════════════════════════════════════

        // All files verified
        assertTrue(verificationErrors.isEmpty(),
                "File verification errors:\n" + String.join("\n", verificationErrors));
        assertEquals(artifacts.size(), verifiedCount,
                "Every artifact must be verified on disk");

        // Minimum artifact count
        assertTrue(artifacts.size() >= 10,
                "Expected ≥10 artifacts, got " + artifacts.size());

        // Key artifact types present
        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.AGGREGATE_ROOT),
                "Should contain aggregate roots");
        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.DOMAIN_EVENT),
                "Should contain domain events");
        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.VALUE_OBJECT),
                "Should contain value objects");
        assertTrue(byType.containsKey(CodeArtifact.ArtifactType.REPOSITORY),
                "Should contain repositories");

        // Files on disk must match artifacts
        assertEquals(artifacts.size(), totalFilesOnDisk,
                "Files on disk must match artifact count");

        // At least some packages were created
        assertFalse(packageDirs.isEmpty(), "Should create package directories");

        // Total pipeline should complete within 5 seconds
        assertTrue(totalNanos < 5_000_000_000L,
                "Total pipeline exceeded 5 s: " + (totalNanos / 1_000_000.0) + " ms");
    }

    // ─── AST inspection ─────────────────────────────────────────────────

    @Test
    @DisplayName("Hotel Booking: parsed AST has correct structure")
    void astStructure() throws ParseException {
        var parser = new DdslParser(ddslSource, "hotel-booking.ddsl");
        DomainModel model = parser.parse();

        var ctx = model.boundedContexts().getFirst();
        assertEquals("HotelBooking", ctx.name());

        // Aggregates
        assertEquals(2, ctx.aggregates().size(), "2 aggregates: Reservation, Room");
        var aggNames = ctx.aggregates().stream().map(a -> a.name()).toList();
        assertTrue(aggNames.contains("Reservation"));
        assertTrue(aggNames.contains("Room"));

        // Value Objects
        assertTrue(ctx.valueObjects().size() >= 4,
                "At least 4 VOs: GuestProfile, Money, DateRange, ContactInfo");

        // Domain Events
        assertTrue(ctx.domainEvents().size() >= 6,
                "At least 6 domain events");

        // Repositories
        assertEquals(2, ctx.repositories().size(), "2 repositories");

        // Domain Services
        assertEquals(2, ctx.domainServices().size(), "2 domain services");

        // Specifications
        assertTrue(ctx.specifications().size() >= 3, "At least 3 specifications");

        // Factories
        assertTrue(ctx.factories().size() >= 1, "At least 1 factory");

        // Behaviors on Reservation aggregate
        var reservation = ctx.aggregates().stream()
                .filter(a -> a.name().equals("Reservation"))
                .findFirst().orElseThrow();
        assertTrue(reservation.behaviors().size() >= 5,
                "Reservation should have ≥5 behaviors (place, confirm, cancel, check-in, check-out)");

        // Invariants on Reservation aggregate
        assertTrue(reservation.invariants().size() >= 3,
                "Reservation should have ≥3 invariants");

        // Child entities
//        assertTrue(reservation.childEntities().size() >= 1,
//                "Reservation should have ≥1 child entity (RoomAssignment)");

        System.out.println("✓ AST structure validated");
        System.out.printf("  Aggregates:       %d%n", ctx.aggregates().size());
        System.out.printf("  Value Objects:    %d%n", ctx.valueObjects().size());
        System.out.printf("  Domain Events:    %d%n", ctx.domainEvents().size());
        System.out.printf("  Domain Services:  %d%n", ctx.domainServices().size());
        System.out.printf("  Repositories:     %d%n", ctx.repositories().size());
        System.out.printf("  Specifications:   %d%n", ctx.specifications().size());
        System.out.printf("  Factories:        %d%n", ctx.factories().size());
        System.out.printf("  Behaviors:        %d (Reservation)%n", reservation.behaviors().size());
        System.out.printf("  Invariants:       %d (Reservation)%n", reservation.invariants().size());
//        System.out.printf("  Child Entities:   %d (Reservation)%n", reservation.childEntities().size());
    }
}
