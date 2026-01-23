package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.building.expression.Expression;

/**
 * Variable declaration statement.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class VariableDeclarationStatement extends Statement {
    private final String type;
    private final String name;
    private final Expression initializer;
    private final boolean isFinal;

    public VariableDeclarationStatement(String type, String name, Expression initializer, boolean isFinal) {
        this.type = type;
        this.name = name;
        this.initializer = initializer;
        this.isFinal = isFinal;
    }

    @Override
    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        if (isFinal) {
            sb.append("final ");
        }
        sb.append(type).append(" ").append(name);
        if (initializer != null) {
            sb.append(" = ").append(initializer.generateCode());
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public Statement copy() {
        Expression copiedInitializer = (initializer != null) ? initializer.copy() : null;
        return new VariableDeclarationStatement(this.type, this.name, copiedInitializer, this.isFinal);
    }
}
