package uet.ndh.ddsl.codegen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Handles writing generated code artifacts to the file system.
 * 
 * This is the final step in the code generation pipeline.
 * It handles:
 * - Creating package directory structures
 * - Writing Java source files
 * - Preserving existing files (optional overwrite control)
 * - Generation report/statistics
 */
@Component
@Slf4j
public class ProjectWriter {
    
    private final WriterConfig config;
    
    public ProjectWriter() {
        this(WriterConfig.defaults());
    }
    
    public ProjectWriter(WriterConfig config) {
        this.config = config;
    }
    
    /**
     * Write all artifacts to the output directory.
     * 
     * @param artifacts List of code artifacts to write
     * @param outputDir Base output directory (e.g., "src/main/java")
     * @return Write result with statistics
     */
    public WriteResult writeAll(List<CodeArtifact> artifacts, Path outputDir) {
        int written = 0;
        int skipped = 0;
        int failed = 0;
        
        for (CodeArtifact artifact : artifacts) {
            try {
                boolean wasWritten = writeArtifact(artifact, outputDir);
                if (wasWritten) {
                    written++;
                } else {
                    skipped++;
                }
            } catch (IOException e) {
                log.error("Failed to write artifact: {}", artifact.fullyQualifiedName(), e);
                failed++;
            }
        }
        
        log.info("Code generation complete: {} written, {} skipped, {} failed", 
                 written, skipped, failed);
        
        return new WriteResult(written, skipped, failed);
    }
    
    /**
     * Write a single artifact to the output directory.
     * 
     * @param artifact The artifact to write
     * @param outputDir Base output directory
     * @return true if the file was written, false if skipped
     */
    public boolean writeArtifact(CodeArtifact artifact, Path outputDir) throws IOException {
        Path filePath = outputDir.resolve(artifact.relativePath());
        
        // Check if file exists and handle based on config
        if (Files.exists(filePath)) {
            if (!config.overwriteExisting()) {
                log.debug("Skipping existing file: {}", filePath);
                return false;
            }
            if (config.backupBeforeOverwrite()) {
                backupFile(filePath);
            }
        }
        
        // Create parent directories
        Files.createDirectories(filePath.getParent());
        
        // Write the file
        Files.writeString(filePath, artifact.sourceCode(), StandardCharsets.UTF_8);
        log.debug("Wrote: {}", filePath);
        
        return true;
    }
    
    /**
     * Create a backup of an existing file.
     */
    private void backupFile(Path filePath) throws IOException {
        Path backupPath = filePath.resolveSibling(filePath.getFileName() + ".bak");
        Files.copy(filePath, backupPath);
        log.debug("Backed up: {} -> {}", filePath, backupPath);
    }
    
    /**
     * Clean the output directory (remove all generated files).
     * Use with caution!
     */
    public void cleanOutputDirectory(Path outputDir, String basePackage) throws IOException {
        Path packageDir = outputDir.resolve(basePackage.replace('.', '/'));
        
        if (Files.exists(packageDir)) {
            Files.walk(packageDir)
                .sorted((a, b) -> -a.compareTo(b)) // Reverse order for deletion
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete: {}", path);
                    }
                });
            log.info("Cleaned output directory: {}", packageDir);
        }
    }
    
    /**
     * Configuration for the project writer.
     */
    public record WriterConfig(
        boolean overwriteExisting,
        boolean backupBeforeOverwrite,
        boolean createPackageInfo
    ) {
        public static WriterConfig defaults() {
            return new WriterConfig(true, false, false);
        }
        
        public static WriterConfig preserveExisting() {
            return new WriterConfig(false, false, false);
        }
        
        public static WriterConfig withBackup() {
            return new WriterConfig(true, true, false);
        }
    }
    
    /**
     * Result of a write operation.
     */
    public record WriteResult(
        int filesWritten,
        int filesSkipped,
        int filesFailed
    ) {
        public int total() {
            return filesWritten + filesSkipped + filesFailed;
        }
        
        public boolean isSuccess() {
            return filesFailed == 0;
        }
    }
}
