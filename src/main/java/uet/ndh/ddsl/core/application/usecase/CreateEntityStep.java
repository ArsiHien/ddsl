package uet.ndh.ddsl.core.application.usecase;

import uet.ndh.ddsl.core.building.CodeBlock;
import uet.ndh.ddsl.core.building.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Step that creates an entity using a factory.
 */
public class CreateEntityStep extends UseCaseStep {
    private final String factoryVar;
    private final List<Expression> parameters;
    private final String resultVar;

    public CreateEntityStep(int order, String factoryVar, List<Expression> parameters, String resultVar) {
        super(order);
        this.factoryVar = factoryVar;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.resultVar = resultVar;
    }

    public String getFactoryVar() {
        return factoryVar;
    }

    public List<Expression> getParameters() {
        return new ArrayList<>(parameters);
    }

    public String getResultVar() {
        return resultVar;
    }

    @Override
    public CodeBlock generateCode() {
        CodeBlock block = new CodeBlock();
        // Generate: Entity entity = factory.create(params...);
        // TODO: Implement with proper statement generation
        return block;
    }

    @Override
    public UseCaseStep copy() {
        List<Expression> copiedParameters = new ArrayList<>();
        for (Expression param : this.parameters) {
            copiedParameters.add(param.copy());
        }
        return new CreateEntityStep(this.getOrder(), this.factoryVar, copiedParameters, this.resultVar);
    }
}
