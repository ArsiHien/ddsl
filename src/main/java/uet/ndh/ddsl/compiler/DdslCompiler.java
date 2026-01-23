package uet.ndh.ddsl.compiler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.codegen.CodeArtifacts;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.core.semantic.TwoPassSemanticAnalyzer;
import uet.ndh.ddsl.codegen.JavaCodeGenerator;
import uet.ndh.ddsl.codegen.FileGenerator;
import uet.ndh.ddsl.compiler.metrics.CompilationMetrics;
import uet.ndh.ddsl.compiler.metrics.CodeAnalyzer;
import uet.ndh.ddsl.parser.ParseException;
import uet.ndh.ddsl.parser.YamlParser;
import uet.ndh.ddsl.validator.DomainModelValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main compiler for the DDSL language implementing modern compiler architecture.
 *
 * Compilation Pipeline:
 * 1. YAML Deserialization → AST Construction
 * 2. Two-Pass Semantic Analysis (Symbol Table + Reference Resolution)
 * 3. Normalization/Desugaring Phase
 * 4. Graph-Based Dependency Analysis
 * 5. DDD Tactical Design Validation
 * 6. Model-to-Text (M2T) Code Generation
 * 7. File System Artifact Generation
 *
 * Academic references:
 * - Structure-driven model construction from serialized syntax
 * - Classical two-pass semantic analysis with symbol tables
 * - Graph-based semantic validation
 */
@Service
@RequiredArgsConstructor
public class DdslCompiler {

    private final YamlParser parser;
    private final DomainModelValidator validator;
    private final JavaCodeGenerator codeGenerator;
    private final FileGenerator fileGenerator;
    private final TwoPassSemanticAnalyzer semanticAnalyzer;

    /**
     * Compile DSL specification to target language with comprehensive metrics tracking.
     */
    public CompilationResult compile(String filePath, String target, String outputDir) {
        CompilationMetrics metrics = new CompilationMetrics();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> generatedFiles = new ArrayList<>();

        try {
            System.out.println("🚀 Starting DDSL compilation...");
            System.out.println("📄 Input file: " + filePath);
            System.out.println("🎯 Target: " + target);
            System.out.println("📁 Output: " + outputDir);
            System.out.println();

            // 1. YAML Deserialization → AST Construction
            DomainModel model;
            try (var parseTimer = metrics.startPhase("PARSING")) {
                model = parser.parseFile(filePath);
                metrics.recordMetadata("input_file", filePath);
                metrics.recordMetadata("target_language", target);
                metrics.recordMetadata("output_directory", outputDir);
            }

            // 2. Two-Pass Semantic Analysis
            TwoPassSemanticAnalyzer.SemanticAnalysisResults semanticResults;
            try (var semanticTimer = metrics.startPhase("SEMANTIC_ANALYSIS")) {
                semanticResults = semanticAnalyzer.analyze(model);

                // Record semantic analysis metrics
                metrics.recordMetadata("symbols_collected", String.valueOf(semanticResults.getSymbolCount()));
                metrics.recordMetadata("dependencies_found", String.valueOf(semanticResults.getDependencyCount()));
                metrics.recordMetadata("semantic_analysis_time", String.valueOf(semanticResults.getAnalysisTimeMs()));

                // Use normalized model for further processing if available
                if (semanticResults.getNormalizedModel() != null) {
                    model = semanticResults.getNormalizedModel().getModel();
                }
            }

            // Analyze domain model structure
            try (var analysisTimer = metrics.startPhase("DOMAIN_ANALYSIS")) {
                CodeAnalyzer.analyzeDomainModel(model, metrics);
            }

            // 3. DDD Tactical Design Validation
            List<ValidationError> validationErrors;
            try (var validationTimer = metrics.startPhase("VALIDATION")) {
                validationErrors = validator.validate(model);

                // Add semantic errors to validation errors
                validationErrors.addAll(semanticResults.getSemanticErrors());
            }

            // Separate errors by severity
            List<ValidationError> blockingErrors = new ArrayList<>();
            List<ValidationError> nonBlockingIssues = new ArrayList<>();

            for (ValidationError error : validationErrors) {
                if (error.severity() == ValidationError.Severity.ERROR) {
                    blockingErrors.add(error);
                    errors.add(error.message() + " at " + error.location());
                    metrics.incrementCounter("validation_errors");
                } else {
                    nonBlockingIssues.add(error);
                    warnings.add(String.format("[%s] %s at %s",
                        error.severity(), error.message(), error.location()));
                    metrics.incrementCounter("validation_warnings");
                }
            }

            // Only stop code generation if there are ERROR-level issues
            if (!blockingErrors.isEmpty()) {
                System.out.println("❌ Code generation stopped due to " + blockingErrors.size() + " ERROR-level validation issues.");
                if (!nonBlockingIssues.isEmpty()) {
                    System.out.println("ℹ️  Also found " + nonBlockingIssues.size() + " non-blocking issues (warnings/info).");
                }
                metrics.recordMetadata("compilation_status", "FAILED_VALIDATION");
                return new CompilationResult(false, errors, generatedFiles, metrics);
            }

            // Print warning summary if we have non-blocking issues but are proceeding
            if (!nonBlockingIssues.isEmpty()) {
                System.out.println("⚠️  Proceeding with code generation despite " + nonBlockingIssues.size() + " warning(s):");
                for (ValidationError issue : nonBlockingIssues) {
                    System.out.println("  - [" + issue.severity() + "] " + issue.message() + " at " + issue.location());
                }
                System.out.println();
            }

            // 4. Model-to-Text (M2T) Code Generation
            CodeArtifacts artifacts;
            try (var codeGenTimer = metrics.startPhase("CODE_GENERATION")) {
                System.out.println("🔨 Generating code using M2T transformation...");
                artifacts = codeGenerator.generate(model);

                // Analyze generated code artifacts
                CodeAnalyzer.analyzeCodeArtifacts(artifacts, metrics);
            }

            // 5. File System Artifact Generation
            List<String> writtenFiles;
            try (var fileWriteTimer = metrics.startPhase("FILE_WRITING")) {
                writtenFiles = fileGenerator.writeToFileSystem(artifacts, outputDir);
                generatedFiles.addAll(writtenFiles);

                // Analyze actual generated files
                CodeAnalyzer.analyzeGeneratedFiles(writtenFiles, metrics);
            }

            // Add warnings to the result for reporting, but mark as successful
            List<String> allMessages = new ArrayList<>(errors);
            allMessages.addAll(warnings);

            metrics.recordMetadata("compilation_status", "SUCCESS");

            // Print metrics report
            metrics.printReport();

            return new CompilationResult(true, allMessages, generatedFiles, metrics);

        } catch (IOException e) {
            errors.add("IO Error: " + e.getMessage());
            metrics.recordMetadata("compilation_status", "FAILED_IO_ERROR");
            metrics.recordMetadata("error_message", e.getMessage());
            return new CompilationResult(false, errors, generatedFiles, metrics);
        } catch (ParseException e) {
            errors.add("Parse Error: " + e.getMessage());
            metrics.recordMetadata("compilation_status", "FAILED_PARSE_ERROR");
            metrics.recordMetadata("error_message", e.getMessage());
            metrics.recordMetadata("error_location", e.getLocation().toString());
            return new CompilationResult(false, errors, generatedFiles, metrics);
        } catch (Exception e) {
            errors.add("Unexpected Error: " + e.getMessage());
            metrics.recordMetadata("compilation_status", "FAILED_UNEXPECTED_ERROR");
            metrics.recordMetadata("error_message", e.getMessage());
            metrics.recordMetadata("error_type", e.getClass().getSimpleName());
            return new CompilationResult(false, errors, generatedFiles, metrics);
        }
    }

    /**
     * Validate DSL specification without generating code, with metrics tracking.
     */
    public CompilationResult validate(String filePath) {
        CompilationMetrics metrics = new CompilationMetrics();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            System.out.println("🔍 Starting DDSL validation...");
            System.out.println("📄 Input file: " + filePath);
            System.out.println();

            // 1. Parse YAML to AST
            DomainModel model;
            try (var parseTimer = metrics.startPhase("PARSING")) {
                model = parser.parseFile(filePath);
                metrics.recordMetadata("input_file", filePath);
            }

            // Analyze domain model structure
            try (var analysisTimer = metrics.startPhase("DOMAIN_ANALYSIS")) {
                CodeAnalyzer.analyzeDomainModel(model, metrics);
            }

            // 2. Validate model
            List<ValidationError> validationErrors;
            try (var validationTimer = metrics.startPhase("VALIDATION")) {
                validationErrors = validator.validate(model);
            }

            // Separate errors by severity
            List<ValidationError> blockingErrors = new ArrayList<>();
            List<ValidationError> nonBlockingIssues = new ArrayList<>();

            for (ValidationError error : validationErrors) {
                if (error.severity() == ValidationError.Severity.ERROR) {
                    blockingErrors.add(error);
                    errors.add(error.message() + " at " + error.location());
                    metrics.incrementCounter("validation_errors");
                } else {
                    nonBlockingIssues.add(error);
                    warnings.add(String.format("[%s] %s at %s",
                        error.severity(), error.message(), error.location()));
                    metrics.incrementCounter("validation_warnings");
                }
            }

            // Report results
            if (blockingErrors.isEmpty() && nonBlockingIssues.isEmpty()) {
                System.out.println("✅ All validation rules passed!");
                metrics.recordMetadata("validation_status", "PERFECT");
                return new CompilationResult(true, errors, new ArrayList<>(), metrics);
            } else if (blockingErrors.isEmpty()) {
                System.out.println("✅ Validation passed with " + nonBlockingIssues.size() + " non-blocking issue(s):");
                for (String warning : warnings) {
                    System.out.println("  - " + warning);
                }
                metrics.recordMetadata("validation_status", "PASSED_WITH_WARNINGS");
                metrics.printReport();
                return new CompilationResult(true, warnings, new ArrayList<>(), metrics);
            } else {
                System.out.println("❌ Validation failed with " + blockingErrors.size() + " ERROR-level issue(s):");
                for (String error : errors) {
                    System.out.println("  - " + error);
                }
                if (!nonBlockingIssues.isEmpty()) {
                    System.out.println("ℹ️  Also found " + nonBlockingIssues.size() + " non-blocking issue(s):");
                    for (String warning : warnings) {
                        System.out.println("  - " + warning);
                    }
                }

                List<String> allMessages = new ArrayList<>(errors);
                allMessages.addAll(warnings);
                metrics.recordMetadata("validation_status", "FAILED");
                metrics.printReport();
                return new CompilationResult(false, allMessages, new ArrayList<>(), metrics);
            }

        } catch (IOException e) {
            errors.add("IO Error: " + e.getMessage());
            metrics.recordMetadata("validation_status", "FAILED_IO_ERROR");
            metrics.recordMetadata("error_message", e.getMessage());
            return new CompilationResult(false, errors, new ArrayList<>(), metrics);
        } catch (ParseException e) {
            errors.add("Parse Error: " + e.getMessage());
            metrics.recordMetadata("validation_status", "FAILED_PARSE_ERROR");
            metrics.recordMetadata("error_message", e.getMessage());
            return new CompilationResult(false, errors, new ArrayList<>(), metrics);
        } catch (Exception e) {
            errors.add("Unexpected Error: " + e.getMessage());
            metrics.recordMetadata("validation_status", "FAILED_UNEXPECTED_ERROR");
            metrics.recordMetadata("error_message", e.getMessage());
            return new CompilationResult(false, errors, new ArrayList<>(), metrics);
        }
    }
}
