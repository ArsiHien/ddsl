package uet.ndh.ddsl.core.application.usecase;

import uet.ndh.ddsl.core.building.CodeBlock; /**
 * Step that saves an entity using a repository.
 */
public class SaveEntityStep extends UseCaseStep {
    private final String repositoryVar;
    private final String entityVar;

    public SaveEntityStep(int order, String repositoryVar, String entityVar) {
        super(order);
        this.repositoryVar = repositoryVar;
        this.entityVar = entityVar;
    }

    public String getRepositoryVar() {
        return repositoryVar;
    }

    public String getEntityVar() {
        return entityVar;
    }

    @Override
    public CodeBlock generateCode() {
        CodeBlock block = new CodeBlock();
        // Generate: repository.save(entity);
        // TODO: Implement with proper statement generation
        return block;
    }

    @Override
    public UseCaseStep copy() {
        return new SaveEntityStep(this.getOrder(), this.repositoryVar, this.entityVar);
    }
}
