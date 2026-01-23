package uet.ndh.ddsl.core.building.statement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uet.ndh.ddsl.core.building.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Method invocation statement.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class MethodInvocation extends Statement {
    private final String receiver;
    private final String methodName;
    private final List<Expression> arguments;

    public MethodInvocation(String receiver, String methodName, List<Expression> arguments) {
        this.receiver = receiver;
        this.methodName = methodName;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }

    @Override
    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        if (receiver != null && !receiver.isEmpty()) {
            sb.append(receiver).append(".");
        }
        sb.append(methodName).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments.get(i).generateCode());
        }
        sb.append(");");
        return sb.toString();
    }

    @Override
    public Statement copy() {
        List<Expression> copiedArguments = new ArrayList<>();
        for (Expression arg : this.arguments) {
            copiedArguments.add(arg.copy());
        }
        return new MethodInvocation(this.receiver, this.methodName, copiedArguments);
    }
}
