package uet.ndh.ddsl.core.application.usecase;

import uet.ndh.ddsl.core.building.CodeBlock; /**
 * Step that publishes a domain event.
 */
public class PublishEventStep extends UseCaseStep {
    private final String eventVar;

    public PublishEventStep(int order, String eventVar) {
        super(order);
        this.eventVar = eventVar;
    }

    public String getEventVar() {
        return eventVar;
    }

    @Override
    public CodeBlock generateCode() {
        CodeBlock block = new CodeBlock();
        // Generate: eventPublisher.publish(event);
        // TODO: Implement with proper statement generation
        return block;
    }

    @Override
    public UseCaseStep copy() {
        return new PublishEventStep(this.getOrder(), this.eventVar);
    }
}
