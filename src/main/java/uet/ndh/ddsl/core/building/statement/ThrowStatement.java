package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.building.expression.Expression;

/**
 * Throw statement.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class ThrowStatement extends Statement {
    private final JavaType exceptionType;
    private final Expression message;

    public ThrowStatement(JavaType exceptionType, Expression message) {
        this.exceptionType = exceptionType;
        this.message = message;
    }

    @Override
    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("throw new ").append(exceptionType.getSimpleName()).append("(");
        if (message != null) {
            sb.append(message.generateCode());
        }
        sb.append(");");
        return sb.toString();
    }

    @Override
    public Statement copy() {
        Expression copiedMessage = (message != null) ? message.copy() : null;
        return new ThrowStatement(this.exceptionType, copiedMessage);
    }
}
