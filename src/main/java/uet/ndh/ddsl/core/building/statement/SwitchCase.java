package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import uet.ndh.ddsl.core.building.expression.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * Switch case.
 */
@Data
public class SwitchCase {
    private final Expression caseValue;
    private final List<Statement> statements;
    private final boolean hasBreak;

    public SwitchCase(Expression caseValue, List<Statement> statements, boolean hasBreak) {
        this.caseValue = caseValue;
        this.statements = statements;
        this.hasBreak = hasBreak;
    }

    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("    case ").append(caseValue.generateCode()).append(":\n");

        for (Statement stmt : statements) {
            sb.append("        ").append(stmt.generateCode()).append("\n");
        }

        if (hasBreak) {
            sb.append("        break;");
        }

        return sb.toString();
    }

    public SwitchCase copy() {
        List<Statement> copiedStatements = new ArrayList<>();
        for (Statement stmt : this.statements) {
            copiedStatements.add(stmt.copy());
        }
        return new SwitchCase(this.caseValue.copy(), copiedStatements, this.hasBreak);
    }
}
