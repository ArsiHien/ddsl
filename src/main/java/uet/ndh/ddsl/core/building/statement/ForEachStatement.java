package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.building.expression.Expression;
import java.util.List;
import java.util.ArrayList;

/**
 * For-each statement for iterating over collections.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class ForEachStatement extends Statement {
    private final String itemType;
    private final String itemVariable;
    private final Expression collection;
    private final List<Statement> bodyStatements;

    public ForEachStatement(String itemType, String itemVariable, Expression collection, List<Statement> bodyStatements) {
        this.itemType = itemType;
        this.itemVariable = itemVariable;
        this.collection = collection;
        this.bodyStatements = bodyStatements;
    }

    @Override
    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("for (").append(itemType).append(" ").append(itemVariable)
          .append(" : ").append(collection.generateCode()).append(") {\n");

        for (Statement stmt : bodyStatements) {
            sb.append("    ").append(stmt.generateCode()).append("\n");
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public Statement copy() {
        List<Statement> copiedBodyStatements = new ArrayList<>();
        for (Statement stmt : this.bodyStatements) {
            copiedBodyStatements.add(stmt.copy());
        }
        return new ForEachStatement(this.itemType, this.itemVariable,
                                   this.collection.copy(), copiedBodyStatements);
    }
}
