package uet.ndh.ddsl.core.application.usecase;

import uet.ndh.ddsl.core.building.CodeBlock;
import uet.ndh.ddsl.core.building.expression.Expression;

/**
 * Step that loads an entity from a repository.
 */
public class LoadEntityStep extends UseCaseStep {
    private final String repositoryVar;
    private final Expression idExpression;
    private final String resultVar;

    public LoadEntityStep(int order, String repositoryVar, Expression idExpression, String resultVar) {
        super(order);
        this.repositoryVar = repositoryVar;
        this.idExpression = idExpression;
        this.resultVar = resultVar;
    }

    public String getRepositoryVar() {
        return repositoryVar;
    }

    public Expression getIdExpression() {
        return idExpression;
    }

    public String getResultVar() {
        return resultVar;
    }

    @Override
    public CodeBlock generateCode() {
        CodeBlock block = new CodeBlock();
        // Generate: Entity entity = repository.findById(id);
        // TODO: Implement with proper statement generation
        return block;
    }

    @Override
    public UseCaseStep copy() {
        return new LoadEntityStep(this.getOrder(), this.repositoryVar,
                                 this.idExpression.copy(), this.resultVar);
    }
}
