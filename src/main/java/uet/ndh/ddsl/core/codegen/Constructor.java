package uet.ndh.ddsl.core.codegen;

import uet.ndh.ddsl.core.building.Parameter;
import uet.ndh.ddsl.core.building.CodeBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a constructor for a Java class.
 */
public class Constructor {
    private final List<Parameter> parameters;
    private CodeBlock body;
    private final Visibility visibility;

    public enum Visibility {
        PRIVATE, PROTECTED, PUBLIC
    }

    public Constructor(Visibility visibility) {
        this.visibility = visibility;
        this.parameters = new ArrayList<>();
    }

    public List<Parameter> getParameters() {
        return new ArrayList<>(parameters);
    }

    public CodeBlock getBody() {
        return body;
    }

    public void setBody(CodeBlock body) {
        this.body = body;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }

    /**
     * Generate constructor code.
     * @param className The name of the class this constructor belongs to
     * @return Complete Java constructor code
     */
    public String generateConstructorCode(String className) {
        StringBuilder sb = new StringBuilder();

        // Visibility
        switch (visibility) {
            case PRIVATE: sb.append("private "); break;
            case PROTECTED: sb.append("protected "); break;
            case PUBLIC: sb.append("public "); break;
        }

        // Constructor name (same as class name)
        sb.append(className).append("(");

        // Parameters
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).generateParameterDeclaration());
        }

        sb.append(") {\n");

        if (body != null) {
            sb.append(body.generateCode());
        }

        sb.append("}");
        return sb.toString();
    }
}
