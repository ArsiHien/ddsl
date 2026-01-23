package uet.ndh.ddsl.codegen.template.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Template data model for Java method generation.
 */
@Data
@Builder
public class MethodTemplateModel {

    // Method definition
    private String name;
    private String returnType;
    private String returnTypeImport;
    private String javadoc;

    // Method modifiers
    private String visibility; // private, protected, public, package
    private boolean isStatic;
    private boolean isFinal;
    private boolean isAbstract;
    private boolean isSynchronized;

    // Method parameters
    @Singular
    private List<ParameterTemplateModel> parameters;

    // Method exceptions
    @Singular
    private List<String> thrownExceptions;

    @Singular
    private List<String> exceptionImports;

    // Method body
    private String body;
    private List<String> bodyLines;

    // Annotations
    @Singular
    private List<String> annotations;

    /**
     * Generate method signature without body
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();

        if (visibility != null && !visibility.equals("package")) {
            sb.append(visibility).append(" ");
        }

        if (isStatic) sb.append("static ");
        if (isFinal) sb.append("final ");
        if (isAbstract) sb.append("abstract ");
        if (isSynchronized) sb.append("synchronized ");

        sb.append(returnType).append(" ").append(name).append("(");

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
     * Get method body with proper indentation
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

    /**
     * Check if method has implementation (not abstract)
     */
    public boolean hasImplementation() {
        return !isAbstract && (body != null || (bodyLines != null && !bodyLines.isEmpty()));
    }
}
