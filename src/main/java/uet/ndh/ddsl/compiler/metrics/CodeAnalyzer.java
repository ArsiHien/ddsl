package uet.ndh.ddsl.compiler.metrics;

import uet.ndh.ddsl.core.codegen.CodeArtifacts;
import uet.ndh.ddsl.core.codegen.JavaClass;
import uet.ndh.ddsl.core.codegen.JavaInterface;
import uet.ndh.ddsl.core.codegen.JavaEnum;
import uet.ndh.ddsl.core.model.DomainModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Analyzes generated code and domain models for metrics.
 */
public class CodeAnalyzer {

    /**
     * Analyze domain model structure and record metrics.
     */
    public static void analyzeDomainModel(DomainModel model, CompilationMetrics metrics) {
        metrics.recordMetadata("model_name", model.getModelName());
        metrics.recordMetadata("model_base_package", model.getBasePackage());

        // Count bounded contexts
        int boundedContexts = model.getBoundedContexts().size();
        metrics.recordCounter("bounded_contexts", boundedContexts);
        metrics.recordMetadata("model_bounded_contexts", boundedContexts);

        // Analyze each bounded context
        model.getBoundedContexts().forEach(context -> {
            metrics.incrementCounter("entities_total");
            metrics.recordCounter("entities_in_" + context.getName(),
                context.getAggregates().stream()
                    .mapToInt(agg -> 1 + agg.getEntities().size()) // root + internal entities
                    .sum());

            metrics.recordCounter("value_objects_in_" + context.getName(),
                context.getValueObjects().size());
            metrics.recordCounter("aggregates_in_" + context.getName(),
                context.getAggregates().size());
            metrics.recordCounter("domain_services_in_" + context.getName(),
                context.getDomainServices().size());
            metrics.recordCounter("repositories_in_" + context.getName(),
                context.getRepositories().size());
            metrics.recordCounter("application_services_in_" + context.getName(),
                context.getApplicationServices().size());
            metrics.recordCounter("domain_events_in_" + context.getName(),
                context.getDomainEvents().size());
        });

        // Total counts across all contexts
        long totalEntities = model.getBoundedContexts().stream()
            .mapToLong(ctx -> ctx.getAggregates().stream()
                .mapToLong(agg -> 1L + agg.getEntities().size())
                .sum())
            .sum();

        long totalValueObjects = model.getBoundedContexts().stream()
            .mapToLong(ctx -> ctx.getValueObjects().size())
            .sum();

        long totalAggregates = model.getBoundedContexts().stream()
            .mapToLong(ctx -> ctx.getAggregates().size())
            .sum();

        long totalRepositories = model.getBoundedContexts().stream()
            .mapToLong(ctx -> ctx.getRepositories().size())
            .sum();

        long totalApplicationServices = model.getBoundedContexts().stream()
            .mapToLong(ctx -> ctx.getApplicationServices().size())
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
    public static void analyzeCodeArtifacts(CodeArtifacts artifacts, CompilationMetrics metrics) {
        long totalLOC = 0;

        // Analyze Java classes
        for (JavaClass javaClass : artifacts.getJavaClasses()) {
            long classLOC = calculateJavaClassLOC(javaClass);
            totalLOC += classLOC;
            metrics.incrementCounter("classes_generated");
        }

        // Analyze Java interfaces
        for (JavaInterface javaInterface : artifacts.getInterfaces()) {
            long interfaceLOC = calculateJavaInterfaceLOC(javaInterface);
            totalLOC += interfaceLOC;
            metrics.incrementCounter("interfaces_generated");
        }

        // Analyze Java enums
        for (JavaEnum javaEnum : artifacts.getEnums()) {
            long enumLOC = calculateJavaEnumLOC(javaEnum);
            totalLOC += enumLOC;
            metrics.incrementCounter("enums_generated");
        }

        metrics.recordCounter("synthetic_loc", totalLOC);
        metrics.recordCounter("files_generated",
            artifacts.getJavaClasses().size() +
            artifacts.getInterfaces().size() +
            artifacts.getEnums().size());
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

    private static long calculateJavaClassLOC(JavaClass javaClass) {
        // Estimate LOC for a Java class
        long loc = 0;

        // Package declaration (1 line)
        if (javaClass.getPackageName() != null && !javaClass.getPackageName().isEmpty()) {
            loc += 1;
        }

        // Imports (assume 1 line each)
        loc += javaClass.getImports().size();

        // Class declaration (1-3 lines depending on inheritance/interfaces)
        loc += 3;

        // Fields (assume 1-2 lines each including getters/setters with Lombok)
        loc += javaClass.getFields().size() * 1;

        // Constructors (assume 3-10 lines each)
        loc += javaClass.getConstructors().size() * 6;

        // Methods (assume 5-15 lines each)
        loc += javaClass.getMethods().size() * 8;

        // Inner classes (recursive calculation)
        for (JavaClass innerClass : javaClass.getInnerClasses()) {
            loc += calculateJavaClassLOC(innerClass);
        }

        // Closing brace
        loc += 1;

        return loc;
    }

    private static long calculateJavaInterfaceLOC(JavaInterface javaInterface) {
        // Estimate LOC for a Java interface
        long loc = 3; // package, class declaration, closing brace

        // Methods (assume 2-3 lines each for interface methods)
        loc += javaInterface.getMethods().size() * 2;

        return loc;
    }

    private static long calculateJavaEnumLOC(JavaEnum javaEnum) {
        // Estimate LOC for a Java enum
        long loc = 5; // package, imports, enum declaration, closing brace

        // Enum constants (assume 1 line each)
        loc += javaEnum.getConstants().size();

        // Fields and methods
        loc += javaEnum.getFields().size() * 2;
        loc += javaEnum.getMethods().size() * 5;

        return loc;
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
