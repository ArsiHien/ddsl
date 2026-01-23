package uet.ndh.ddsl.core.building;

import uet.ndh.ddsl.core.JavaType;

/**
 * Represents a method parameter.
 */
public record Parameter(String name, JavaType type, boolean isFinal) {

    public Parameter(String name, JavaType type) {
        this(name, type, false);
    }

    /**
     * Create a copy of this parameter.
     */
    public Parameter copy() {
        return new Parameter(this.name, this.type, this.isFinal);
    }

    public String generateParameterDeclaration() {
        StringBuilder sb = new StringBuilder();
        if (isFinal) {
            sb.append("final ");
        }
        sb.append(type.getSimpleName()).append(" ").append(name);
        return sb.toString();
    }
}
