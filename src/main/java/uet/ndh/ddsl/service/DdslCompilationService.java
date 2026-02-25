package uet.ndh.ddsl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.controller.dto.CompileResponse;
import uet.ndh.ddsl.parser.DdslParser;

import java.util.List;

/**
 * Service that orchestrates the DDSL → Java compilation pipeline:
 * <ol>
 *   <li>Parse DDSL source into a {@link DomainModel} AST</li>
 *   <li>Run JavaPoet-based code generation via {@link PoetModule}</li>
 *   <li>Return the generated artifacts as {@link CompileResponse}</li>
 * </ol>
 */
@Service
public class DdslCompilationService {

    private static final Logger log = LoggerFactory.getLogger(DdslCompilationService.class);

    /**
     * Compile DDSL source code into Java artifacts.
     *
     * @param ddslCode    valid DDSL source
     * @param basePackage target Java base package
     * @return compilation response with artifacts or errors
     */
    public CompileResponse compile(String ddslCode, String basePackage) {
        log.info("Compiling DDSL ({} chars) → Java (package: {})",
                ddslCode.length(), basePackage);

        // 1. Parse
        DdslParser parser;
        DomainModel model;
        try {
            parser = new DdslParser(ddslCode, "<input>");
            model = parser.parse();
        } catch (Exception e) {
            log.error("DDSL parse failed", e);
            return CompileResponse.failure("Parse error: " + e.getMessage());
        }

        if (parser.hasErrors()) {
            List<String> errors = parser.getErrors().stream()
                    .map(err -> "line %d:%d — %s".formatted(err.line(), err.column(), err.message()))
                    .toList();
            log.warn("DDSL parse produced {} errors", errors.size());
            return CompileResponse.failure(errors);
        }

        // 2. Generate Java code
        try {
            var poetModule = new PoetModule(basePackage);
            List<CodeArtifact> codeArtifacts = poetModule.generateFromModel(model);

            List<CompileResponse.Artifact> artifacts = codeArtifacts.stream()
                    .map(a -> new CompileResponse.Artifact(
                            a.fileName(),
                            a.packageName(),
                            a.artifactType().name(),
                            a.sourceCode()
                    ))
                    .toList();

            log.info("Code generation produced {} artifacts", artifacts.size());
            return CompileResponse.success(artifacts);

        } catch (Exception e) {
            log.error("Code generation failed", e);
            return CompileResponse.failure("Codegen error: " + e.getMessage());
        }
    }
}
