package uet.ndh.ddsl.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uet.ndh.ddsl.controller.dto.CompileRequest;
import uet.ndh.ddsl.controller.dto.CompileResponse;
import uet.ndh.ddsl.service.DdslCompilationService;

/**
 * REST API for compiling valid DDSL code into Java source files.
 * Parses the DDSL input, runs JavaPoet-based code generation, and returns
 * the generated Java artifacts.
 */
@RestController
@RequestMapping("/api/compile")
public class CompilationController {

    private static final Logger log = LoggerFactory.getLogger(CompilationController.class);

    private final DdslCompilationService compilationService;

    public CompilationController(DdslCompilationService compilationService) {
        this.compilationService = compilationService;
    }

    /**
     * Compile DDSL source code into Java artifacts.
     *
     * @param request the compile request containing DDSL code and options
     * @return generated Java source files or error details
     */
    @PostMapping
    public ResponseEntity<CompileResponse> compile(@RequestBody CompileRequest request) {
        log.info("POST /api/compile — code: {} chars, basePackage: {}",
                request.code() != null ? request.code().length() : 0,
                request.basePackage());

        if (request.code() == null || request.code().isBlank()) {
            return ResponseEntity.badRequest().body(
                    CompileResponse.failure("DDSL code cannot be empty")
            );
        }

        String basePackage = request.basePackage() != null && !request.basePackage().isBlank()
                ? request.basePackage()
                : "com.example.domain";

        CompileResponse response = compilationService.compile(request.code(), basePackage);
        return ResponseEntity.ok(response);
    }
}
