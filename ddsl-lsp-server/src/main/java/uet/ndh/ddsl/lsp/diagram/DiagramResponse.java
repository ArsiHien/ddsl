package uet.ndh.ddsl.lsp.diagram;

import java.util.List;

/**
 * Response envelope for diagram generation commands.
 */
public record DiagramResponse(
        boolean success,
        String diagramType,
        Object model,
        List<String> errors
) {
    public DiagramResponse {
        errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public static DiagramResponse success(String diagramType, Object model) {
        return new DiagramResponse(true, diagramType, model, List.of());
    }

    public static DiagramResponse failure(String diagramType, String error) {
        return new DiagramResponse(false, diagramType, null, List.of(error));
    }

    public static DiagramResponse failure(String diagramType, List<String> errors) {
        return new DiagramResponse(false, diagramType, null, errors);
    }
}
