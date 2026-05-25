package uet.ndh.ddsl.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import uet.ndh.ddsl.agent.NlToDslResult;
import uet.ndh.ddsl.mcp.DdslValidationTool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Automated test runner for NL-to-DSL translation pipeline.
 * Tests all cases declared in test-cases/nl-to-dsl-test-cases.json.
 * <p>
 * Requirements:
 * - Qdrant running on localhost:6334
 * - OPENROUTER_API_KEY set in environment or application-local.properties
 * <p>
 * This test is disabled by default since it requires external services.
 * To run: ./gradlew test --tests "uet.ndh.ddsl.agent.NlToDslTestRunner" -Dspring.profiles.active=local
 */
@SpringBootTest
//@Disabled("Requires external services: Qdrant + OpenRouter API key")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class NlToDslTestRunner {

    @Autowired
    private NlToDslService nlToDslService;

    @Autowired
    private DdslValidationTool validationTool;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<TestCase> testCases;
    private final List<TestResult> results = new ArrayList<>();
    private Path runReportDir;

    @BeforeEach
    void setUp() throws IOException {
        loadTestCases();
        initRunReportDir();
    }

    private void loadTestCases() throws IOException {
        ClassPathResource resource = new ClassPathResource("test-cases/nl-to-dsl-test-cases-hard-5.json");
        TestSuite testSuite = objectMapper.readValue(resource.getInputStream(), TestSuite.class);
        this.testCases = testSuite.testCases();
        log.info("Loaded {} test cases", testCases.size());
    }

    private void initRunReportDir() throws IOException {
        if (runReportDir != null) {
            return;
        }
        String runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        runReportDir = Path.of("build", "reports", "nl-to-dsl-runs", runId).toAbsolutePath();
        Files.createDirectories(runReportDir);
        log.info("NL-to-DSL run artifacts will be written to {}", runReportDir);
    }

    @Test
    @Order(1)
    @DisplayName("Run all test cases")
    void runAllTestCases() {
        log.info("Starting test run with {} cases", testCases.size());

        for (TestCase testCase : testCases) {
            TestResult result = runTestCase(testCase);
            results.add(result);
        }

        generateReport();
    }

    private TestResult runTestCase(TestCase testCase) {
        log.info("Running test case: {} ({}) - {}", testCase.id(), testCase.category(), testCase.name());
        long startTime = System.currentTimeMillis();

        try {
            // Run through the pipeline with node-level diagnostics.
            NlToDslDetailedResult detailedResult = nlToDslService.translateWithTrace(testCase.input(), 2);
            NlToDslResult result = detailedResult.result();

            // Validate the output
            boolean syntaxValid = validateSyntax(result.dsl());

            long executionTime = System.currentTimeMillis() - startTime;
            printCaseTrace(testCase, detailedResult, syntaxValid);
            writeCaseArtifacts(testCase, detailedResult, syntaxValid);

            return new TestResult(
                testCase,
                result.success() && syntaxValid,
                result.dsl(),
                result.errors(),
                result.retrieverRetries() + result.synthesizerRetries(),
                executionTime,
                result.retrievalQuality(),
                result.compilerFeedback(),
                detailedResult.trace(),
                detailedResult.nodeTimeMs(),
                detailedResult.nodeIterations()
            );

        } catch (Exception e) {
            log.error("Test case {} failed with exception", testCase.id(), e);
            return new TestResult(
                testCase,
                false,
                "",
                List.of("Exception: " + e.getMessage()),
                0,
                System.currentTimeMillis() - startTime,
                0.0,
                "",
                List.of(),
                Map.of(),
                Map.of()
            );
        }
    }

    private void writeCaseArtifacts(TestCase testCase, NlToDslDetailedResult detailedResult, boolean syntaxValid) throws IOException {
        Path caseDir = runReportDir.resolve(safeName(testCase.id() + "-" + testCase.name()));
        Files.createDirectories(caseDir);

        NlToDslResult result = detailedResult.result();
        Files.writeString(caseDir.resolve("input.txt"), testCase.input(), StandardCharsets.UTF_8);
        Files.writeString(caseDir.resolve("final.ddsl"), defaultString(result.dsl()), StandardCharsets.UTF_8);
        Files.writeString(caseDir.resolve("final-numbered.ddsl"), numbered(defaultString(result.dsl()), errorLines(detailedResult)), StandardCharsets.UTF_8);
        Files.writeString(caseDir.resolve("compiler-feedback.txt"), defaultString(result.compilerFeedback()), StandardCharsets.UTF_8);
        Files.writeString(caseDir.resolve("trace.json"), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detailedResult.trace()), StandardCharsets.UTF_8);
        Files.writeString(caseDir.resolve("synthesis-artifacts.json"), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detailedResult.synthesisArtifacts()), StandardCharsets.UTF_8);
        Files.writeString(caseDir.resolve("judge-artifacts.json"), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detailedResult.judgeArtifacts()), StandardCharsets.UTF_8);

        for (int i = 0; i < detailedResult.synthesisArtifacts().size(); i++) {
            Map<String, Object> artifact = detailedResult.synthesisArtifacts().get(i);
            String chunk = safeName(String.valueOf(artifact.getOrDefault("chunkId", "unknown")));
            String mode = safeName(String.valueOf(artifact.getOrDefault("mode", "unknown")));
            String attempt = String.valueOf(artifact.getOrDefault("attempt", i));
            Files.writeString(
                    caseDir.resolve(String.format("llm-%02d-%s-%s-attempt-%s.ddsl", i + 1, chunk, mode, attempt)),
                    defaultString(String.valueOf(artifact.getOrDefault("code", ""))),
                    StandardCharsets.UTF_8
            );
        }

        for (int i = 0; i < detailedResult.judgeArtifacts().size(); i++) {
            Map<String, Object> artifact = detailedResult.judgeArtifacts().get(i);
            String chunk = safeName(String.valueOf(artifact.getOrDefault("chunkId", "unknown")));
            Files.writeString(
                    caseDir.resolve(String.format("judge-%02d-%s-compiler-output.json", i + 1, chunk)),
                    defaultString(String.valueOf(artifact.getOrDefault("compilerOutput", ""))),
                    StandardCharsets.UTF_8
            );
            Files.writeString(
                    caseDir.resolve(String.format("judge-%02d-%s-merged.ddsl", i + 1, chunk)),
                    defaultString(String.valueOf(artifact.getOrDefault("dsl", ""))),
                    StandardCharsets.UTF_8
            );
        }

        Files.writeString(caseDir.resolve("README.md"), caseMarkdown(testCase, detailedResult, syntaxValid), StandardCharsets.UTF_8);
        System.out.printf("Artifacts written to: %s%n", caseDir);
    }

    private String caseMarkdown(TestCase testCase, NlToDslDetailedResult detailedResult, boolean syntaxValid) {
        NlToDslResult result = detailedResult.result();
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(testCase.id()).append(" - ").append(testCase.name()).append("\n\n");
        sb.append("- Category: `").append(testCase.category()).append("`\n");
        sb.append("- Success: `").append(result.success()).append("`\n");
        sb.append("- Syntax valid: `").append(syntaxValid).append("`\n");
        sb.append("- Total time: `").append(detailedResult.totalTimeMs()).append(" ms`\n");
        sb.append("- Steps: `").append(detailedResult.trace().size()).append("`\n");
        sb.append("- Synthesis artifacts: `").append(detailedResult.synthesisArtifacts().size()).append("`\n");
        sb.append("- Judge artifacts: `").append(detailedResult.judgeArtifacts().size()).append("`\n\n");

        sb.append("## Node Timings\n\n");
        detailedResult.nodeIterations().forEach((node, iterations) -> {
            long nodeMs = detailedResult.nodeTimeMs().getOrDefault(node, 0L);
            sb.append("- `").append(node).append("`: iterations=`").append(iterations)
                    .append("`, totalMs=`").append(nodeMs).append("`\n");
        });

        sb.append("\n## Files\n\n");
        sb.append("- `input.txt`\n");
        sb.append("- `final.ddsl`\n");
        sb.append("- `final-numbered.ddsl`\n");
        sb.append("- `compiler-feedback.txt`\n");
        sb.append("- `trace.json`\n");
        sb.append("- `synthesis-artifacts.json`\n");
        sb.append("- `judge-artifacts.json`\n");
        sb.append("- `llm-*.ddsl`: raw LLM output per Synthesizer call\n");
        sb.append("- `judge-*-compiler-output.json`: raw compiler/MCP output per Judge call\n");
        sb.append("- `judge-*-merged.ddsl`: merged DSL sent to Judge\n");
        return sb.toString();
    }

    private List<Integer> errorLines(NlToDslDetailedResult detailedResult) {
        return detailedResult.trace().stream()
                .flatMap(step -> step.structuredErrors() == null
                        ? java.util.stream.Stream.<Map<String, Object>>empty()
                        : step.structuredErrors().stream())
                .map(error -> error.get("line_number"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .filter(line -> line > 0)
                .distinct()
                .sorted()
                .toList();
    }

    private String numbered(String dsl, List<Integer> errorLines) {
        Set<Integer> highlighted = new HashSet<>(errorLines);
        StringBuilder sb = new StringBuilder();
        List<String> lines = dsl.lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String marker = highlighted.contains(lineNumber) ? ">>" : "  ";
            sb.append(String.format("%s %4d | %s%n", marker, lineNumber, lines.get(i)));
        }
        return sb.toString();
    }

    private String safeName(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-|-$", "");
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private void printCaseTrace(TestCase testCase, NlToDslDetailedResult detailedResult, boolean syntaxValid) {
        NlToDslResult result = detailedResult.result();
        System.out.println();
        System.out.println("-".repeat(120));
        System.out.printf("TRACE %s (%s) - %s%n", testCase.id(), testCase.category(), testCase.name());
        System.out.printf("success=%s | syntaxValid=%s | totalTime=%d ms | totalSteps=%d | retrieverRetries=%d | synthesizerRetries=%d | finalQuality=%.2f | dslChars=%d%n",
                result.success(),
                syntaxValid,
                detailedResult.totalTimeMs(),
                detailedResult.trace().size(),
                result.retrieverRetries(),
                result.synthesizerRetries(),
                result.retrievalQuality(),
                result.dsl() != null ? result.dsl().length() : 0);

        System.out.println("Stream emission timing (delta since previous emitted node; LLM latency may appear on the next emitted node):");
        detailedResult.nodeIterations().forEach((node, iterations) -> {
            long nodeMs = detailedResult.nodeTimeMs().getOrDefault(node, 0L);
            double avgMs = iterations > 0 ? nodeMs / (double) iterations : 0.0;
            System.out.printf("  %-18s emissions=%2d | totalDelta=%6d ms | avgDelta=%7.1f ms%n",
                    node, iterations, nodeMs, avgMs);
        });

        System.out.println("Step timeline:");
        System.out.printf("  %-4s %-18s %-8s %-12s %-15s %-11s %-7s %-7s %-8s %-9s %-10s %-24s%n",
                "#", "node", "sincePrev", "phase", "route", "chunk", "mode", "R/S", "quality", "dslChars", "errors", "plan");
        for (NlToDslTraceStep step : detailedResult.trace()) {
            System.out.printf("  %-4d %-18s %6dms %-12s %-15s %-11s %-7s %d/%d     %-8.2f %-9d %-10d %-24s%n",
                    step.index(),
                    step.node(),
                    step.deltaMs(),
                    abbreviate(step.phase(), 12),
                    abbreviate(step.route(), 15),
                    abbreviate(step.chunkId(), 11),
                    abbreviate(step.synthesisMode(), 7),
                    step.retrieverRetries(),
                    step.synthesizerRetries(),
                    step.retrievalQuality(),
                    step.currentDslChars(),
                    step.errorCount(),
                    formatPlan(step.planStatuses()));
//            if (step.structuredErrors() != null && !step.structuredErrors().isEmpty()) {
//                step.structuredErrors().stream().limit(3).forEach(error ->
//                        System.out.printf("       verdict=%s retry=%s source=%s line=%s type=%s msg=%s%n",
//                                error.get("verdict"),
//                                error.get("retry_allowed"),
//                                error.get("source_chunk"),
//                                error.get("line_number"),
//                                error.get("error_type"),
//                                abbreviate(String.valueOf(error.get("error_message")), 72)));
//            }
        }
        if (!result.errors().isEmpty()) {
            System.out.printf("Final errors: %s%n", String.join(" | ", result.errors()));
        }
        if (!result.success() || !syntaxValid) {
            printFailureDebug(result, detailedResult);
        }
        System.out.println("-".repeat(120));
    }

    private void printFailureDebug(NlToDslResult result, NlToDslDetailedResult detailedResult) {
        if (result.compilerFeedback() != null && !result.compilerFeedback().isBlank()) {
            System.out.println();
            System.out.println("Compiler feedback:");
            System.out.println(result.compilerFeedback());
        }

        List<Integer> errorLines = detailedResult.trace().stream()
                .flatMap(step -> step.structuredErrors() == null
                        ? java.util.stream.Stream.<Map<String, Object>>empty()
                        : step.structuredErrors().stream())
                .map(error -> error.get("line_number"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .filter(line -> line > 0)
                .distinct()
                .sorted()
                .toList();

        String dsl = result.dsl();
        if (dsl == null || dsl.isBlank()) {
            System.out.println();
            System.out.println("Generated DSL is empty.");
            return;
        }

        System.out.println();
        System.out.println("Generated DSL with line numbers:");
        printNumberedDsl(dsl, errorLines);
    }

    private void printNumberedDsl(String dsl, List<Integer> errorLines) {
        List<String> lines = dsl.lines().toList();
        Set<Integer> highlighted = new HashSet<>(errorLines);
        if (highlighted.isEmpty() || lines.size() <= 140) {
            for (int i = 0; i < lines.size(); i++) {
                int lineNumber = i + 1;
                String marker = highlighted.contains(lineNumber) ? ">>" : "  ";
                System.out.printf("%s %4d | %s%n", marker, lineNumber, lines.get(i));
            }
            return;
        }

        Set<Integer> printed = new TreeSet<>();
        for (int errorLine : highlighted) {
            int from = Math.max(1, errorLine - 6);
            int to = Math.min(lines.size(), errorLine + 10);
            for (int line = from; line <= to; line++) {
                printed.add(line);
            }
        }

        int previous = -1;
        for (int lineNumber : printed) {
            if (previous != -1 && lineNumber > previous + 1) {
                System.out.println("       | ...");
            }
            String marker = highlighted.contains(lineNumber) ? ">>" : "  ";
            System.out.printf("%s %4d | %s%n", marker, lineNumber, lines.get(lineNumber - 1));
            previous = lineNumber;
        }
    }

    private String formatPlan(Map<String, String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        statuses.forEach((chunk, status) -> {
            if (!sb.isEmpty()) {
                sb.append(",");
            }
            sb.append(shortChunk(chunk)).append("=").append(shortStatus(status));
        });
        return abbreviate(sb.toString(), 24);
    }

    private String shortChunk(String chunk) {
        return switch (chunk) {
            case "UbiquitousLanguage" -> "UL";
            case "DomainTypes" -> "DT";
            case "DomainModel" -> "DM";
            case "StateMachines" -> "SM";
            case "DomainServices" -> "DS";
            case "DomainEvents" -> "EV";
            case "EventHandlers" -> "EH";
            case "Factories" -> "FA";
            case "Repositories" -> "RE";
            case "Specifications" -> "SP";
            case "UseCases" -> "UC";
            default -> abbreviate(chunk, 3);
        };
    }

    private String shortStatus(String status) {
        return switch (status) {
            case "PENDING" -> "PEND";
            case "RUNNING" -> "RUN";
            case "GENERATED" -> "GEN";
            case "APPROVED" -> "OK";
            case "REPAIRING" -> "FIX";
            case "FAILED" -> "FAIL";
            default -> abbreviate(status, 4);
        };
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "~";
    }

    private boolean validateSyntax(String dsl) {
        if (dsl == null || dsl.isBlank()) {
            return false;
        }
        try {
            String validationResult = validationTool.validateDSL(dsl);
            return validationResult.contains("\"valid\":true");
        } catch (Exception e) {
            log.warn("Validation failed: {}", e.getMessage());
            return false;
        }
    }

    private void generateReport() {
        int total = results.size();
        int passed = (int) results.stream().filter(TestResult::success).count();
        int failed = total - passed;

        long avgExecutionTime = results.stream()
            .mapToLong(TestResult::executionTimeMs)
            .sum() / total;

        double avgRetries = results.stream()
            .mapToInt(TestResult::totalRetries)
            .average()
            .orElse(0.0);

        double avgRetrievalQuality = results.stream()
            .mapToDouble(TestResult::retrievalQuality)
            .average()
            .orElse(0.0);

        double avgSteps = results.stream()
            .mapToInt(r -> r.trace().size())
            .average()
            .orElse(0.0);

        Map<String, LongSummaryStatistics> nodeTimeStats = new TreeMap<>();
        Map<String, IntSummaryStatistics> nodeIterationStats = new TreeMap<>();
        for (TestResult result : results) {
            result.nodeTimeMs().forEach((node, timeMs) ->
                    nodeTimeStats.computeIfAbsent(node, ignored -> new LongSummaryStatistics()).accept(timeMs));
            result.nodeIterations().forEach((node, iterations) ->
                    nodeIterationStats.computeIfAbsent(node, ignored -> new IntSummaryStatistics()).accept(iterations));
        }

        // Category breakdown
        Map<String, CategoryStats> categoryStats = new HashMap<>();
        for (TestResult result : results) {
            String category = result.testCase().category();
            categoryStats.computeIfAbsent(category, k -> new CategoryStats(category, 0, 0));
            categoryStats.get(category).total++;
            if (result.success()) {
                categoryStats.get(category).passed++;
            }
        }

        // Print report
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DDSL NL-to-DSL Test Report");
        System.out.println("=".repeat(80));
        System.out.printf("Total Tests:    %d%n", total);
        System.out.printf("Passed:         %d (%.1f%%)%n", passed, (passed * 100.0 / total));
        System.out.printf("Failed:         %d (%.1f%%)%n", failed, (failed * 100.0 / total));
        System.out.printf("Avg Time:       %d ms%n", avgExecutionTime);
        System.out.printf("Avg Retries:    %.2f%n", avgRetries);
        System.out.printf("Avg Steps:      %.2f%n", avgSteps);
        System.out.printf("Avg Retrieval:  %.2f%n", avgRetrievalQuality);
        System.out.println("-".repeat(80));

        System.out.println("\nStream Emission Timing Summary:");
        nodeTimeStats.forEach((node, stats) -> {
            IntSummaryStatistics iterStats = nodeIterationStats.getOrDefault(node, new IntSummaryStatistics());
            System.out.printf("  %-18s avgDelta=%7.1f ms | min=%6d | max=%6d | avgEmissions=%.2f%n",
                    node,
                    stats.getAverage(),
                    stats.getMin(),
                    stats.getMax(),
                    iterStats.getAverage());
        });

        System.out.println("\nCategory Breakdown:");
        categoryStats.values().stream()
            .sorted(Comparator.comparing(c -> c.category))
            .forEach(cat -> {
                double rate = cat.passed * 100.0 / cat.total;
                System.out.printf("  %s: %d/%d (%.1f%%)%n", cat.category, cat.passed, cat.total, rate);
            });

        System.out.println("\nFailed Tests:");
        results.stream()
            .filter(r -> !r.success())
            .forEach(r -> {
                System.out.printf("  %s (%s): %s%n", r.testCase().id(), r.testCase().category(), r.testCase().name());
                System.out.printf("    Steps: %d | Node iterations: %s%n", r.trace().size(), r.nodeIterations());
                if (!r.errors().isEmpty()) {
                    System.out.printf("    Errors: %s%n", String.join(", ", r.errors()));
                }
                if (r.compilerFeedback() != null && !r.compilerFeedback().isBlank()) {
                    System.out.printf("    Compiler Feedback: %s%n", r.compilerFeedback());
                }
            });

        System.out.println("=".repeat(80) + "\n");

        // Assert overall success rate
        double successRate = passed * 100.0 / total;
        Assertions.assertTrue(successRate >= 70.0,
            String.format("Success rate %.1f%% is below threshold of 70%%", successRate));
    }

    // Record classes
    public record TestCase(String id, String category, String name, String input, String expectedOutput) {}
    public record TestSuite(String version, int totalCases, Map<String, Integer> categories, List<TestCase> testCases) {}
    public record TestResult(
            TestCase testCase,
            boolean success,
            String actualOutput,
            List<String> errors,
            int totalRetries,
            long executionTimeMs,
            double retrievalQuality,
            String compilerFeedback,
            List<NlToDslTraceStep> trace,
            Map<String, Long> nodeTimeMs,
            Map<String, Integer> nodeIterations
    ) {}
    
    // Mutable class for category statistics
    public static class CategoryStats {
        public final String category;
        public int passed;
        public int total;
        
        public CategoryStats(String category, int passed, int total) {
            this.category = category;
            this.passed = passed;
            this.total = total;
        }
    }
}
