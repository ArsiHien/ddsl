package uet.ndh.ddsl.shell;

import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import uet.ndh.ddsl.compiler.DdslCompiler;
import uet.ndh.ddsl.compiler.CompilationResult;
import uet.ndh.ddsl.compiler.metrics.CompilationMetrics;

import java.io.File;

/**
 * Shell commands for the DDSL compiler with template-based generation.
 */
@ShellComponent
@RequiredArgsConstructor
public class DdslCommands {

    private final DdslCompiler compiler;


    @ShellMethod(value = "Generate code from DSL specification", key = "generate")
    public String generate(
            @ShellOption(value = "--file", defaultValue = "", help = "YAML specification file")
            String filePath,
            @ShellOption(value = "--target", defaultValue = "java", help = "Target language (java)")
            String target,
            @ShellOption(value = "--output", defaultValue = "target/generated-sources", help = "Output directory")
            String outputDir) {

        try {
            // Validate inputs
            if (filePath.isEmpty()) {
                return "Error: Please specify a YAML file using --file option";
            }

            File file = new File(filePath);
            if (!file.exists()) {
                return "Error: File not found: " + filePath;
            }

            if (!"java".equalsIgnoreCase(target)) {
                return "Error: Only 'java' target is currently supported";
            }

            // Compile
            CompilationResult result = compiler.compile(filePath, target, outputDir);

            if (result.isSuccess()) {
                StringBuilder sb = new StringBuilder();
                sb.append("✅ Code generation completed successfully!\n");

                // Show metrics summary if available
                if (result.hasMetrics()) {
                    CompilationMetrics metrics = result.getMetrics();
                    sb.append("\n📊 Quick Metrics Summary:\n");
                    sb.append(String.format("  ⏱️  Total Time: %d ms\n", metrics.getTotalTime().toMillis()));
                    sb.append(String.format("  📄 Generated Files: %d\n", metrics.getFilesGenerated()));
                    sb.append(String.format("  📏 Synthetic LOC: %,d lines\n", metrics.getSyntheticLOC()));

                    if (metrics.getCounters().containsKey("actual_loc")) {
                        sb.append(String.format("  📐 Actual LOC: %,d lines\n",
                            metrics.getCounters().get("actual_loc")));
                    }
                }

                // Check if there were any warnings
                boolean hasWarnings = result.getErrors().stream()
                    .anyMatch(msg -> msg.contains("[WARNING]") || msg.contains("[INFO]"));

                if (hasWarnings) {
                    sb.append("\n⚠️  Generated with warnings:\n");
                    for (String message : result.getErrors()) {
                        if (message.contains("[WARNING]") || message.contains("[INFO]")) {
                            sb.append("  - ").append(message).append("\n");
                        }
                    }
                }

                sb.append("\n📁 Generated files:\n");
                for (String generatedFile : result.getGeneratedFiles()) {
                    sb.append("  - ").append(generatedFile).append("\n");
                }

                return sb.toString();
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("❌ Code generation failed with errors:\n");
                for (String error : result.getErrors()) {
                    sb.append("  - ").append(error).append("\n");
                }
                return sb.toString();
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Validate DSL specification", key = "validate")
    public String validate(
            @ShellOption(value = "--file", defaultValue = "", help = "YAML specification file")
            String filePath) {

        try {
            if (filePath.isEmpty()) {
                return "Error: Please specify a YAML file using --file option";
            }

            File file = new File(filePath);
            if (!file.exists()) {
                return "Error: File not found: " + filePath;
            }

            CompilationResult result = compiler.validate(filePath);

            if (result.isSuccess()) {
                StringBuilder sb = new StringBuilder();
                if (result.getErrors().isEmpty()) {
                    sb.append("✅ Validation completed successfully - no issues found.\n");
                } else {
                    sb.append("✅ Validation passed with warnings/info:\n");
                    for (String message : result.getErrors()) {
                        sb.append("  - ").append(message).append("\n");
                    }
                }

                // Show metrics summary if available
                if (result.hasMetrics()) {
                    CompilationMetrics metrics = result.getMetrics();
                    sb.append("\n📊 Validation Metrics:\n");
                    sb.append(String.format("  ⏱️  Validation Time: %d ms\n", metrics.getTotalTime().toMillis()));
                    sb.append(String.format("  🔍 Errors Found: %d\n", metrics.getValidationErrors()));
                    sb.append(String.format("  ⚠️  Warnings Found: %d\n", metrics.getValidationWarnings()));

                    if (metrics.getMetadata().containsKey("model_total_entities")) {
                        sb.append(String.format("  🏗️  Total Entities: %s\n",
                            metrics.getMetadata().get("model_total_entities")));
                        sb.append(String.format("  💎 Total Value Objects: %s\n",
                            metrics.getMetadata().get("model_total_value_objects")));
                        sb.append(String.format("  🏛️  Total Aggregates: %s\n",
                            metrics.getMetadata().get("model_total_aggregates")));
                    }
                }

                return sb.toString();
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("❌ Validation failed with errors:\n");
                for (String error : result.getErrors()) {
                    sb.append("  - ").append(error).append("\n");
                }
                return sb.toString();
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
