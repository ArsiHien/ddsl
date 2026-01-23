package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.building.expression.Expression;
import java.util.List;
import java.util.ArrayList;

/**
 * Switch statement.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class SwitchStatement extends Statement {
    private final Expression switchExpression;
    private final List<SwitchCase> cases;
    private final List<Statement> defaultStatements;

    public SwitchStatement(Expression switchExpression, List<SwitchCase> cases, List<Statement> defaultStatements) {
        this.switchExpression = switchExpression;
        this.cases = cases;
        this.defaultStatements = defaultStatements;
    }

    @Override
    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("switch (").append(switchExpression.generateCode()).append(") {\n");

        for (SwitchCase caseStmt : cases) {
            sb.append(caseStmt.generateCode()).append("\n");
        }

        if (defaultStatements != null && !defaultStatements.isEmpty()) {
            sb.append("    default:\n");
            for (Statement stmt : defaultStatements) {
                sb.append("        ").append(stmt.generateCode()).append("\n");
            }
            sb.append("        break;\n");
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public Statement copy() {
        List<SwitchCase> copiedCases = new ArrayList<>();
        for (SwitchCase caseStmt : this.cases) {
            copiedCases.add(caseStmt.copy());
        }

        List<Statement> copiedDefaultStatements = null;
        if (this.defaultStatements != null) {
            copiedDefaultStatements = new ArrayList<>();
            for (Statement stmt : this.defaultStatements) {
                copiedDefaultStatements.add(stmt.copy());
            }
        }

        return new SwitchStatement(this.switchExpression.copy(), copiedCases, copiedDefaultStatements);
    }
}
