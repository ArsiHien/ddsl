package uet.ndh.ddsl.codegen.template.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Template data model for method/constructor parameters.
 */
@Data
@Builder
public class ParameterTemplateModel {

    private String name;
    private String type;
    private String typeImport;
    private boolean isFinal;
    private String defaultValue;

    @Singular
    private List<String> annotations;

    /**
     * Generate parameter declaration
     */
    public String getDeclaration() {
        StringBuilder sb = new StringBuilder();

        if (isFinal) {
            sb.append("final ");
        }

        sb.append(type).append(" ").append(name);

        return sb.toString();
    }
}
