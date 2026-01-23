package uet.ndh.ddsl.core.building.expression;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Method call expression.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class MethodCallExpression extends Expression {
    private final Expression target;
    private final String methodName;
    private final List<Expression> arguments;
    private final String returnType;

    public MethodCallExpression(Expression target, String methodName,
                               List<Expression> arguments, String returnType) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
        this.returnType = returnType;
    }

    // Backward compatibility constructor
    public MethodCallExpression(Expression target, String methodName, List<Expression> arguments) {
        this(target, methodName, arguments, "Object");
    }

    @Override
    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        if (target != null) {
            sb.append(target.generateCode()).append(".");
        }
        sb.append(methodName).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments.get(i).generateCode());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getType() {
        return returnType;
    }

    @Override
    public Expression copy() {
        List<Expression> copiedArguments = new ArrayList<>();
        for (Expression arg : this.arguments) {
            copiedArguments.add(arg.copy());
        }
        Expression copiedTarget = (target != null) ? target.copy() : null;
        return new MethodCallExpression(copiedTarget, this.methodName, copiedArguments, this.returnType);
    }
}
