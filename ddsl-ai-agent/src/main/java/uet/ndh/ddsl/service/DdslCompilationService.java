package uet.ndh.ddsl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uet.ndh.ddsl.controller.dto.CompileResponse;

/**
 * AI-layer adapter for compile API.
 * Delegates compilation to shared core compiler service.
 */
@Service
public class DdslCompilationService {

    private static final Logger log = LoggerFactory.getLogger(DdslCompilationService.class);
    private final uet.ndh.ddsl.compiler.DdslCompilationService coreCompiler;

    public DdslCompilationService() {
        this.coreCompiler = new uet.ndh.ddsl.compiler.DdslCompilationService();
    }

    /**
     * Compile DDSL source code into Java artifacts.
     *
     * @param ddslCode    valid DDSL source
     * @param basePackage target Java base package
     * @return compilation response with artifacts or errors
     */
    public CompileResponse compile(String ddslCode, String basePackage) {
        log.info("Compiling DDSL ({} chars) via core compiler", ddslCode != null ? ddslCode.length() : 0);

        uet.ndh.ddsl.compiler.api.CompileResponse coreResponse = coreCompiler.compile(ddslCode, basePackage);
        return toControllerResponse(coreResponse);
    }

    private CompileResponse toControllerResponse(uet.ndh.ddsl.compiler.api.CompileResponse coreResponse) {
        return new CompileResponse(
                coreResponse.success(),
                coreResponse.artifacts().stream()
                        .map(a -> new CompileResponse.Artifact(
                                a.fileName(),
                                a.packageName(),
                                a.type(),
                                a.sourceCode()
                        ))
                        .toList(),
                coreResponse.errors(),
                coreResponse.diagnostics().stream()
                        .map(d -> new CompileResponse.DiagnosticMessage(
                                d.message(),
                                d.severity(),
                                d.line(),
                                d.column(),
                                d.ruleId()
                        ))
                        .toList()
        );
    }
}
