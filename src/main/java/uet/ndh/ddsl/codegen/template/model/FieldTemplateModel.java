package uet.ndh.ddsl.codegen.template.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Template data model for Java field generation.
 */
@Data
@Builder
public class FieldTemplateModel {

    // Field definition
    private String name;
    private String type;
    private String typeImport;
    private String javadoc;

    // Field modifiers
    private String visibility; // private, protected, public, package
    private boolean isStatic;
    private boolean isFinal;
    private boolean isTransient;
    private boolean isVolatile;

    // Field value
    private String initialValue;

    // Annotations
    @Singular
    private List<String> annotations;

    /**
     * Generate field declaration without initialization
     */
    public String getDeclaration() {
        StringBuilder sb = new StringBuilder();

        if (visibility != null && !visibility.equals("package")) {
            sb.append(visibility).append(" ");
        }

        if (isStatic) sb.append("static ");
        if (isFinal) sb.append("final ");
        if (isTransient) sb.append("transient ");
        if (isVolatile) sb.append("volatile ");

        sb.append(type).append(" ").append(name);

        if (initialValue != null) {
            sb.append(" = ").append(initialValue);
        }

        sb.append(";");
        return sb.toString();
    }

    /**
     * Generate getter method name
     */
    public String getGetterName() {
        String prefix = type.equals("boolean") ? "is" : "get";
        return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Generate setter method name
     */
    public String getSetterName() {
        return "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
