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
import java.util.*;

/**
 * Mini test suite for quick validation of NL-to-DSL translation pipeline.
 * Runs only 3 test cases (1 easy, 1 medium, 1 hard) instead of 100.
 * <p>
 * Requirements:
 * - Qdrant running on localhost:6334
 * - OPENROUTER_API_KEY set in environment or application-local.properties
 * <p>
 * To run: ./gradlew test --tests "uet.ndh.ddsl.agent.NlToDslMiniTestRunner" -Dspring.profiles.active=local
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class NlToDslMiniTestRunner {

    @Autowired
    private NlToDslService nlToDslService;

    @Autowired
    private DdslValidationTool validationTool;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<TestCase> testCases;
    private final List<TestResult> results = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        loadTestCases();
    }

    private void loadTestCases() throws IOException {
        ClassPathResource resource = new ClassPathResource("test-cases/nl-to-dsl-mini-test-cases.json");
        TestSuite testSuite = objectMapper.readValue(resource.getInputStream(), TestSuite.class);
        this.testCases = testSuite.testCases();
        log.info("Loaded {} test cases", testCases.size());
    }

    @Test
    @Order(1)
    @DisplayName("Run mini test suite (3 cases)")
    void runAllTestCases() {
        log.info("Starting mini test run with {} cases", testCases.size());

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
            // Run through the pipeline
            NlToDslResult result = nlToDslService.translate(testCase.input(), 2);

            // Validate the output
            boolean syntaxValid = validateSyntax(result.dsl());

            long executionTime = System.currentTimeMillis() - startTime;

            return new TestResult(
                testCase,
                result.success() && syntaxValid,
                result.dsl(),
                result.errors(),
                result.retrieverRetries() + result.synthesizerRetries(),
                executionTime,
                result.retrievalQuality(),
                result.compilerFeedback()
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
                ""
            );
        }
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
        System.out.println("DDSL NL-to-DSL Mini Test Report (3 Cases)");
        System.out.println("=".repeat(80));
        System.out.printf("Total Tests:    %d%n", total);
        System.out.printf("Passed:         %d (%.1f%%)%n", passed, (passed * 100.0 / total));
        System.out.printf("Failed:         %d (%.1f%%)%n", failed, (failed * 100.0 / total));
        System.out.printf("Avg Time:       %d ms%n", avgExecutionTime);
        System.out.printf("Avg Retries:    %.2f%n", avgRetries);
        System.out.printf("Avg Retrieval:  %.2f%n", avgRetrievalQuality);
        System.out.println("-".repeat(80));

        System.out.println("\nTest Case Details:");
        results.forEach(r -> {
            String status = r.success() ? "✓ PASS" : "✗ FAIL";
            System.out.printf("%n  %s %s (%s): %s%n", status, r.testCase().id(), r.testCase().category(), r.testCase().name());
            System.out.printf("     Time: %d ms | Retries: %d | Quality: %.2f%n", 
                r.executionTimeMs(), r.totalRetries(), r.retrievalQuality());
            if (!r.success() && !r.errors().isEmpty()) {
                System.out.printf("     Errors: %s%n", String.join(", ", r.errors()));
            }
        });

        if (failed > 0) {
            System.out.println("\nFailed Test Details:");
            results.stream()
                .filter(r -> !r.success())
                .forEach(r -> {
                    System.out.printf("%n  %s (%s): %s%n", r.testCase().id(), r.testCase().category(), r.testCase().name());
                    System.out.printf("    Input: %s%n", r.testCase().input());
                    System.out.printf("    Output: %s%n", r.actualOutput());
                    if (!r.errors().isEmpty()) {
                        System.out.printf("    Errors: %s%n", String.join(", ", r.errors()));
                    }
                    if (r.compilerFeedback() != null && !r.compilerFeedback().isBlank()) {
                        System.out.printf("    Compiler: %s%n", r.compilerFeedback());
                    }
                });
        }

        System.out.println("=".repeat(80) + "\n");

        // Assert overall success rate (expect at least 2 out of 3 to pass)
        double successRate = passed * 100.0 / total;
        Assertions.assertTrue(successRate >= 66.0,
            String.format("Success rate %.1f%% is below threshold of 66%% (2/3 tests)", successRate));
    }

    // Record classes
    public record TestCase(String id, String category, String name, String input, String expectedOutput) {}
    public record TestSuite(String version, int totalCases, Map<String, Integer> categories, List<TestCase> testCases) {}
    public record TestResult(TestCase testCase, boolean success, String actualOutput, List<String> errors, int totalRetries, long executionTimeMs, double retrievalQuality, String compilerFeedback) {}
    
    // Mutable class for category statistics
    public static class CategoryStats {
        public final String category;
        public int passed;
        public int total;
        
        public CategoryStats(String category, int passed, int total) {
            this.category = category;
            this.passed = passed;
            this.total = passed;
            this.total = total;
        }
    }
}
