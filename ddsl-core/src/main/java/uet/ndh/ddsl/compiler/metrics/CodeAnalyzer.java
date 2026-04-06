package uet.ndh.ddsl.compiler.metrics;

import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.ast.model.BoundedContextDecl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Analyzes generated code and domain models for metrics.
 */
public class CodeAnalyzer {

    /**
     * Analyze domain model structure and record metrics.
     */
    public static void analyzeDomainModel(DomainModel model, CompilationMetrics metrics) {
        metrics.recordMetadata("model_name", model.name());

        // Count bounded contexts
        int boundedContexts = model.boundedContexts().size();
        metrics.recordCounter("bounded_contexts", boundedContexts);
        metrics.recordMetadata("model_bounded_contexts", boundedContexts);

        // Analyze each bounded context
        model.boundedContexts().forEach(context -> {
            metrics.incrementCounter("entities_total");
            metrics.recordCounter("entities_in_" + context.name(),
                context.aggregates().stream()
                    .mapToInt(agg -> 1 + agg.entities().size()) // root + internal entities
                    .sum());

            metrics.recordCounter("value_objects_in_" + context.name(),
                context.valueObjects().size());
            metrics.recordCounter("aggregates_in_" + context.name(),
                context.aggregates().size());
            metrics.recordCounter("domain_services_in_" + context.name(),
                context.domainServices().size());
            metrics.recordCounter("repositories_in_" + context.name(),
                context.repositories().size());
            metrics.recordCounter("application_services_in_" + context.name(),
                context.applicationServices().size());
            metrics.recordCounter("domain_events_in_" + context.name(),
                context.domainEvents().size());
        });

        // Total counts across all contexts
        long totalEntities = model.boundedContexts().stream()
            .mapToLong(ctx -> ctx.aggregates().stream()
                .mapToLong(agg -> 1L + agg.entities().size())
                .sum())
            .sum();

        long totalValueObjects = model.boundedContexts().stream()
            .mapToLong(ctx -> ctx.valueObjects().size())
            .sum();

        long totalAggregates = model.boundedContexts().stream()
            .mapToLong(ctx -> ctx.aggregates().size())
            .sum();

        long totalRepositories = model.boundedContexts().stream()
            .mapToLong(ctx -> ctx.repositories().size())
            .sum();

        long totalApplicationServices = model.boundedContexts().stream()
            .mapToLong(ctx -> ctx.applicationServices().size())
            .sum();

        metrics.recordMetadata("model_total_entities", totalEntities);
        metrics.recordMetadata("model_total_value_objects", totalValueObjects);
        metrics.recordMetadata("model_total_aggregates", totalAggregates);
        metrics.recordMetadata("model_total_repositories", totalRepositories);
        metrics.recordMetadata("model_total_app_services", totalApplicationServices);

        // Calculate domain complexity score
        double complexityScore = calculateDomainComplexity(
            boundedContexts, totalEntities, totalValueObjects,
            totalAggregates, totalRepositories, totalApplicationServices);
        metrics.recordMetadata("model_complexity_score", String.format("%.2f", complexityScore));
    }

    /**
     * Analyze generated code artifacts and calculate synthetic LOC.
     */
    public static void analyzeCodeArtifacts(List<CodeArtifact> artifacts, CompilationMetrics metrics) {
        long totalLOC = 0;

        for (CodeArtifact artifact : artifacts) {
            long artifactLOC = calculateArtifactLOC(artifact);
            totalLOC += artifactLOC;
            
            // Count by type
            switch (artifact.artifactType()) {
                case AGGREGATE_ROOT, ENTITY -> metrics.incrementCounter("classes_generated");
                case VALUE_OBJECT, DOMAIN_EVENT -> metrics.incrementCounter("records_generated");
                case REPOSITORY, DOMAIN_SERVICE -> metrics.incrementCounter("interfaces_generated");
                case ENUM -> metrics.incrementCounter("enums_generated");
                default -> metrics.incrementCounter("other_generated");
            }
        }

        metrics.recordCounter("synthetic_loc", totalLOC);
        metrics.recordCounter("files_generated", artifacts.size());
    }

    /**
     * Analyze generated files on disk for actual LOC.
     */
    public static void analyzeGeneratedFiles(List<String> generatedFiles, CompilationMetrics metrics) {
        long actualLOC = 0;
        long actualFiles = 0;

        for (String filePath : generatedFiles) {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    long fileLOC = Files.lines(path)
                        .filter(line -> !line.trim().isEmpty())
                        .filter(line -> !line.trim().startsWith("//"))
                        .filter(line -> !line.trim().startsWith("/*"))
                        .filter(line -> !line.trim().startsWith("*"))
                        .filter(line -> !line.trim().equals("*/"))
                        .count();

                    actualLOC += fileLOC;
                    actualFiles++;
                }
            } catch (IOException e) {
                // Skip files that can't be read
                System.err.println("Warning: Could not analyze file " + filePath + ": " + e.getMessage());
            }
        }

        metrics.recordCounter("actual_loc", actualLOC);
        metrics.recordCounter("actual_files", actualFiles);

        // Calculate accuracy of synthetic LOC estimation
        long syntheticLOC = metrics.getSyntheticLOC();
        if (syntheticLOC > 0) {
            double accuracy = (double) actualLOC / syntheticLOC * 100.0;
            metrics.recordMetadata("model_loc_estimation_accuracy", String.format("%.1f%%", accuracy));
        }
    }

    /**
     * Calculate LOC for a CodeArtifact based on its source code.
     */
    private static long calculateArtifactLOC(CodeArtifact artifact) {
        if (artifact.sourceCode() == null || artifact.sourceCode().isEmpty()) {
            return 0;
        }
        
        // Count non-empty, non-comment lines
        return artifact.sourceCode().lines()
            .filter(line -> !line.trim().isEmpty())
            .filter(line -> !line.trim().startsWith("//"))
            .filter(line -> !line.trim().startsWith("/*"))
            .filter(line -> !line.trim().startsWith("*"))
            .filter(line -> !line.trim().equals("*/"))
            .count();
    }

    private static double calculateDomainComplexity(
            int boundedContexts, long entities, long valueObjects,
            long aggregates, long repositories, long applicationServices) {

        // Simple complexity scoring algorithm
        double score = 0.0;

        score += boundedContexts * 10.0;           // Bounded contexts add significant complexity
        score += entities * 5.0;                   // Each entity adds complexity
        score += valueObjects * 2.0;               // Value objects add less complexity
        score += aggregates * 8.0;                 // Aggregates are complex
        score += repositories * 3.0;               // Repositories moderate complexity
        score += applicationServices * 6.0;        // Application services significant complexity

        return score;
    }
}
