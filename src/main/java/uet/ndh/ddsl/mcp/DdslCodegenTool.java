package uet.ndh.ddsl.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.ast.model.DomainModel;
import uet.ndh.ddsl.codegen.CodeArtifact;
import uet.ndh.ddsl.codegen.poet.PoetModule;
import uet.ndh.ddsl.parser.DdslParser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Tool that generates Java code from validated DDSL source.
 * This is a convenience tool for the LLM to preview generated output.
 */
@Component
public class DdslCodegenTool {

    @Tool(description = """
            Generates Java Spring Boot code from valid DDSL source code.
            Returns a JSON object with:
            - "success": boolean
            - "artifacts": array of {"name", "type", "content"} representing generated Java files
            - "errors": array of error messages if generation failed
            """)
    public String generateCode(
            @ToolParam(description = "Valid DDSL source code to generate Java code from") String code
    ) {
        try {
            var parser = new DdslParser(code, "<input>");
            DomainModel model = parser.parse();

            if (parser.hasErrors()) {
                var errors = parser.getErrors().stream().map(Object::toString).toList();
                return "{\"success\":false,\"artifacts\":[],\"errors\":" + toJsonArray(errors) + "}";
            }

            var generator = new PoetModule("com.example.domain");
            List<CodeArtifact> artifacts = generator.generateFromModel(model);

            var artifactJsons = artifacts.stream()
                    .map(a -> String.format(
                            "{\"name\":\"%s\",\"type\":\"%s\",\"content\":\"%s\"}",
                            escapeJson(a.fileName()),
                            a.artifactType().name(),
                            escapeJson(a.sourceCode())
                    ))
                    .collect(Collectors.joining(","));

            return "{\"success\":true,\"artifacts\":[" + artifactJsons + "],\"errors\":[]}";

        } catch (Exception e) {
            return "{\"success\":false,\"artifacts\":[],\"errors\":[\"" + escapeJson(e.getMessage()) + "\"]}";
        }
    }

    private String toJsonArray(List<String> items) {
        return "[" + items.stream()
                .map(s -> "\"" + escapeJson(s) + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
