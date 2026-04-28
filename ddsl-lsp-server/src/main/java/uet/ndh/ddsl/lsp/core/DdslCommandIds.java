package uet.ndh.ddsl.lsp.core;

import java.util.List;

/**
 * Central registry of command identifiers exposed through LSP.
 */
public final class DdslCommandIds {

    private DdslCommandIds() {
    }

    public static final String COMPILE = "ddsl.compile";
    public static final String VALIDATE = "ddsl.validate";
    public static final String GENERATE_CODE = "ddsl.generateCode";

    public static final String CONVERT_TO_ENTITY = "ddsl.convertToEntity";
    public static final String EXTRACT_VALUE_OBJECT = "ddsl.extractValueObject";
    public static final String EXTRACT_TO_VALUE_OBJECT = "ddsl.extractToValueObject";
    public static final String GENERATE_INVARIANTS = "ddsl.generateInvariants";
    public static final String GENERATE_OPERATIONS = "ddsl.generateOperations";
    public static final String GENERATE_COMPONENT_DIAGRAM = "ddsl.generateComponentDiagram";
    public static final String GENERATE_EVENT_FLOW_DIAGRAM = "ddsl.generateEventFlowDiagram";

    public static final List<String> ALL = List.of(
        COMPILE,
        VALIDATE,
        GENERATE_CODE,
        CONVERT_TO_ENTITY,
        EXTRACT_VALUE_OBJECT,
        EXTRACT_TO_VALUE_OBJECT,
        GENERATE_INVARIANTS,
        GENERATE_OPERATIONS,
        GENERATE_COMPONENT_DIAGRAM,
        GENERATE_EVENT_FLOW_DIAGRAM
    );
}
