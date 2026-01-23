package uet.ndh.ddsl.compiler.metrics;

import lombok.Data;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks compilation metrics and performance timing.
 */
@Data
public class CompilationMetrics {

    @Getter
    private final Map<String, Duration> timings = new HashMap<>();

    @Getter
    private final Map<String, Long> counters = new HashMap<>();

    @Getter
    private final Map<String, Object> metadata = new HashMap<>();

    private final Instant compilationStart = Instant.now();

    /**
     * Start timing a phase.
     */
    public PhaseTimer startPhase(String phaseName) {
        return new PhaseTimer(phaseName, this);
    }

    /**
     * Record a timing manually.
     */
    public void recordTiming(String phase, Duration duration) {
        timings.put(phase, duration);
    }

    /**
     * Record a counter value.
     */
    public void recordCounter(String name, long value) {
        counters.put(name, value);
    }

    /**
     * Increment a counter.
     */
    public void incrementCounter(String name) {
        counters.merge(name, 1L, Long::sum);
    }

    /**
     * Record metadata.
     */
    public void recordMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Get total compilation time.
     */
    public Duration getTotalTime() {
        return Duration.between(compilationStart, Instant.now());
    }

    /**
     * Get synthetic lines of code generated.
     */
    public long getSyntheticLOC() {
        return counters.getOrDefault("synthetic_loc", 0L);
    }

    /**
     * Get number of files generated.
     */
    public long getFilesGenerated() {
        return counters.getOrDefault("files_generated", 0L);
    }

    /**
     * Get number of validation errors.
     */
    public long getValidationErrors() {
        return counters.getOrDefault("validation_errors", 0L);
    }

    /**
     * Get number of validation warnings.
     */
    public long getValidationWarnings() {
        return counters.getOrDefault("validation_warnings", 0L);
    }

    /**
     * Print detailed metrics report.
     */
    public void printReport() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 COMPILATION METRICS REPORT");
        System.out.println("=".repeat(60));

        // Timing Information
        System.out.println("\n⏱️  TIMING BREAKDOWN:");
        timings.entrySet().stream()
            .sorted(Map.Entry.<String, Duration>comparingByValue().reversed())
            .forEach(entry -> {
                System.out.printf("  %-20s: %,d ms%n",
                    entry.getKey(), entry.getValue().toMillis());
            });

        System.out.printf("  %-20s: %,d ms%n", "TOTAL_TIME", getTotalTime().toMillis());

        // Code Generation Metrics
        System.out.println("\n📈 CODE GENERATION METRICS:");
        System.out.printf("  %-20s: %,d lines%n", "Synthetic LOC", getSyntheticLOC());
        System.out.printf("  %-20s: %,d files%n", "Files Generated", getFilesGenerated());

        // Calculate throughput
        long totalTime = getTotalTime().toMillis();
        if (totalTime > 0) {
            double locPerSecond = (getSyntheticLOC() * 1000.0) / totalTime;
            System.out.printf("  %-20s: %,.1f LOC/sec%n", "Generation Speed", locPerSecond);
        }

        // Validation Metrics
        System.out.println("\n🔍 VALIDATION METRICS:");
        System.out.printf("  %-20s: %,d issues%n", "Errors", getValidationErrors());
        System.out.printf("  %-20s: %,d issues%n", "Warnings", getValidationWarnings());
        System.out.printf("  %-20s: %,d total%n", "Total Issues",
            getValidationErrors() + getValidationWarnings());

        // Domain Model Metrics
        System.out.println("\n🏗️  DOMAIN MODEL METRICS:");
        metadata.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("model_"))
            .forEach(entry -> {
                String displayName = entry.getKey().replace("model_", "")
                    .replace("_", " ")
                    .toUpperCase();
                System.out.printf("  %-20s: %s%n", displayName, entry.getValue());
            });

        // Additional Counters
        if (!counters.isEmpty()) {
            System.out.println("\n📋 ADDITIONAL COUNTERS:");
            counters.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("validation_") &&
                               !entry.getKey().equals("synthetic_loc") &&
                               !entry.getKey().equals("files_generated"))
                .forEach(entry -> {
                    String displayName = entry.getKey().replace("_", " ").toUpperCase();
                    System.out.printf("  %-20s: %,d%n", displayName, entry.getValue());
                });
        }

        System.out.println("=".repeat(60));
    }

    /**
     * Helper class for timing phases automatically.
     */
    public static class PhaseTimer implements AutoCloseable {
        private final String phaseName;
        private final CompilationMetrics metrics;
        private final Instant startTime;

        public PhaseTimer(String phaseName, CompilationMetrics metrics) {
            this.phaseName = phaseName;
            this.metrics = metrics;
            this.startTime = Instant.now();
            System.out.printf("🔄 Starting %s...%n", phaseName);
        }

        @Override
        public void close() {
            Duration duration = Duration.between(startTime, Instant.now());
            metrics.recordTiming(phaseName, duration);
            System.out.printf("✅ Completed %s in %d ms%n", phaseName, duration.toMillis());
        }
    }
}
