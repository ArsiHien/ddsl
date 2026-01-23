package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.building.expression.Expression;

/**
 * Assignment statement.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class AssignmentStatement extends Statement {
    private final String variableName;
    private final Expression value;

    public AssignmentStatement(String variableName, Expression value) {
        this.variableName = variableName;
        this.value = value;
    }

    @Override
    public String generateCode() {
        return variableName + " = " + value.generateCode() + ";";
    }

    @Override
    public Statement copy() {
        return new AssignmentStatement(this.variableName, this.value.copy());
    }
}
