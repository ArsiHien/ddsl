package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.building.expression.Expression;
import java.util.List;
import java.util.ArrayList;

/**
 * If statement with optional else clause.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class IfStatement extends Statement {
    private final Expression condition;
    private final List<Statement> thenStatements;
    private final List<Statement> elseStatements;

    public IfStatement(Expression condition, List<Statement> thenStatements, List<Statement> elseStatements) {
        this.condition = condition;
        this.thenStatements = thenStatements;
        this.elseStatements = elseStatements;
    }

    @Override
    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(condition.generateCode()).append(") {\n");

        for (Statement stmt : thenStatements) {
            sb.append("    ").append(stmt.generateCode()).append("\n");
        }

        if (elseStatements != null && !elseStatements.isEmpty()) {
            sb.append("} else {\n");
            for (Statement stmt : elseStatements) {
                sb.append("    ").append(stmt.generateCode()).append("\n");
            }
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public Statement copy() {
        List<Statement> copiedThenStatements = new ArrayList<>();
        for (Statement stmt : this.thenStatements) {
            copiedThenStatements.add(stmt.copy());
        }

        List<Statement> copiedElseStatements = null;
        if (this.elseStatements != null) {
            copiedElseStatements = new ArrayList<>();
            for (Statement stmt : this.elseStatements) {
                copiedElseStatements.add(stmt.copy());
            }
        }

        return new IfStatement(this.condition.copy(), copiedThenStatements, copiedElseStatements);
    }
}
