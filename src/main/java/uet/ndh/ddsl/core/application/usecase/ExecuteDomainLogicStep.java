package uet.ndh.ddsl.core.application.usecase;

import lombok.Getter;
import uet.ndh.ddsl.core.building.CodeBlock;
import uet.ndh.ddsl.core.building.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Step that executes domain logic on an entity.
 */
public class ExecuteDomainLogicStep extends UseCaseStep {
    @Getter
    private final String targetVar;
    @Getter
    private final String methodName;
    private final List<Expression> arguments;

    public ExecuteDomainLogicStep(int order, String targetVar, String methodName, List<Expression> arguments) {
        super(order);
        this.targetVar = targetVar;
        this.methodName = methodName;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }

    public List<Expression> getArguments() {
        return new ArrayList<>(arguments);
    }

    @Override
    public CodeBlock generateCode() {
        CodeBlock block = new CodeBlock();
        // Generate: target.methodName(args...);
        // TODO: Implement with proper statement generation
        return block;
    }

    @Override
    public UseCaseStep copy() {
        List<Expression> copiedArguments = new ArrayList<>();
        for (Expression arg : this.arguments) {
            copiedArguments.add(arg.copy());
        }
        return new ExecuteDomainLogicStep(this.getOrder(), this.targetVar, this.methodName, copiedArguments);
    }
}
