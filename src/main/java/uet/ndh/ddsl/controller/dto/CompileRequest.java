package uet.ndh.ddsl.controller.dto;

/**
 * Request body for DDSL → Java compilation.
 *
 * @param code        valid DDSL source code
 * @param basePackage target Java base package (defaults to "com.example.domain")
 */
public record CompileRequest(
        String code,
        String basePackage
) {}
