package uet.ndh.ddsl.codegen.template.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Template data model for Java constructor generation.
 */
@Data
@Builder
public class ConstructorTemplateModel {

    // Constructor definition
    private String className;
    private String javadoc;

    // Constructor modifiers
    private String visibility; // private, protected, public, package

    // Constructor parameters
    @Singular
    private List<ParameterTemplateModel> parameters;

    // Constructor exceptions
    @Singular
    private List<String> thrownExceptions;

    // Constructor body
    private String body;
    private List<String> bodyLines;

    // Annotations
    @Singular
    private List<String> annotations;

    /**
     * Generate constructor signature without body
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();

        if (visibility != null && !visibility.equals("package")) {
            sb.append(visibility).append(" ");
        }

        sb.append(className).append("(");

        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).getDeclaration());
        }

        sb.append(")");

        if (!thrownExceptions.isEmpty()) {
            sb.append(" throws ");
            sb.append(String.join(", ", thrownExceptions));
        }

        return sb.toString();
    }

    /**
     * Get constructor body with proper formatting
     */
    public String getFormattedBody() {
        if (body != null) {
            return body;
        } else if (bodyLines != null && !bodyLines.isEmpty()) {
            return String.join("\n        ", bodyLines);
        } else {
            return "";
        }
    }
}

