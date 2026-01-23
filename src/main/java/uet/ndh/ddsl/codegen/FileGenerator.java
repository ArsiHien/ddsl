package uet.ndh.ddsl.codegen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.codegen.template.TemplateBasedCodeGenerator;
import uet.ndh.ddsl.core.codegen.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates files from code artifacts using template-based generation.
 */
@Component
@Slf4j
public class FileGenerator {

    private TemplateBasedCodeGenerator templateGenerator;

    @Autowired(required = false)
    public void setTemplateGenerator(TemplateBasedCodeGenerator templateGenerator) {
        this.templateGenerator = templateGenerator;
        log.info("Template-based code generation is enabled");
    }

    public List<String> writeToFileSystem(CodeArtifacts artifacts, String outputDir)
            throws IOException {
        List<String> generatedFiles = new ArrayList<>();

        Path basePath = Paths.get(outputDir);
        Files.createDirectories(basePath);

        // Generate Java classes
        for (JavaClass javaClass : artifacts.getJavaClasses()) {
            // Set template generator for enhanced generation
            if (templateGenerator != null) {
                javaClass.setTemplateGenerator(templateGenerator);
            }
            String filePath = writeJavaClass(javaClass, basePath);
            generatedFiles.add(filePath);
        }

        // Generate Java interfaces
        for (JavaInterface javaInterface : artifacts.getInterfaces()) {
            String filePath = writeJavaInterface(javaInterface, basePath);
            generatedFiles.add(filePath);
        }

        // Generate Java enums
        for (JavaEnum javaEnum : artifacts.getEnums()) {
            String filePath = writeJavaEnum(javaEnum, basePath);
            generatedFiles.add(filePath);
        }

        return generatedFiles;
    }

    private String writeJavaClass(JavaClass javaClass, Path basePath) throws IOException {
        String packagePath = javaClass.getPackageName().replace('.', File.separatorChar);
        Path packageDir = basePath.resolve("java").resolve(packagePath);
        Files.createDirectories(packageDir);

        Path classFile = packageDir.resolve(javaClass.getClassName() + ".java");
        String sourceCode = javaClass.generateSourceCode();

        try (FileWriter writer = new FileWriter(classFile.toFile())) {
            writer.write(sourceCode);
        }

        return classFile.toString();
    }

    private String writeJavaInterface(JavaInterface javaInterface, Path basePath) throws IOException {
        String packagePath = javaInterface.getPackageName().replace('.', File.separatorChar);
        Path packageDir = basePath.resolve("java").resolve(packagePath);
        Files.createDirectories(packageDir);

        Path interfaceFile = packageDir.resolve(javaInterface.getInterfaceName() + ".java");

        // Use template-based generation if available
        String sourceCode;
        if (templateGenerator != null) {
            try {
                sourceCode = templateGenerator.generateJavaInterface(javaInterface);
            } catch (Exception e) {
                log.warn("Template generation failed for interface {}, falling back to original generation: {}",
                    javaInterface.getInterfaceName(), e.getMessage());
                sourceCode = javaInterface.generateSourceCode();
            }
        } else {
            sourceCode = javaInterface.generateSourceCode();
        }

        try (FileWriter writer = new FileWriter(interfaceFile.toFile())) {
            writer.write(sourceCode);
        }

        return interfaceFile.toString();
    }

    private String writeJavaEnum(JavaEnum javaEnum, Path basePath) throws IOException {
        String packagePath = javaEnum.getPackageName().replace('.', File.separatorChar);
        Path packageDir = basePath.resolve("java").resolve(packagePath);
        Files.createDirectories(packageDir);

        Path enumFile = packageDir.resolve(javaEnum.getEnumName() + ".java");

        // Use template-based generation if available
        String sourceCode;
        if (templateGenerator != null) {
            try {
                sourceCode = templateGenerator.generateJavaEnum(javaEnum);
            } catch (Exception e) {
                log.warn("Template generation failed for enum {}, falling back to original generation: {}",
                    javaEnum.getEnumName(), e.getMessage());
                sourceCode = javaEnum.generateSourceCode();
            }
        } else {
            sourceCode = javaEnum.generateSourceCode();
        }

        try (FileWriter writer = new FileWriter(enumFile.toFile())) {
            writer.write(sourceCode);
        }

        return enumFile.toString();
    }
}

