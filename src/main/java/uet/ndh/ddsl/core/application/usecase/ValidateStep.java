package uet.ndh.ddsl.core.application.usecase;

import uet.ndh.ddsl.core.building.CodeBlock;
import uet.ndh.ddsl.core.building.expression.Expression;

/**
 * Step that validates something.
 */
public class ValidateStep extends UseCaseStep {
    private final Expression validationExpression;
    private final String errorMessage;

    public ValidateStep(int order, Expression validationExpression, String errorMessage) {
        super(order);
        this.validationExpression = validationExpression;
        this.errorMessage = errorMessage;
    }

    public Expression getValidationExpression() {
        return validationExpression;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public CodeBlock generateCode() {
        CodeBlock block = new CodeBlock();
        // Generate: if (!validation) throw new ValidationException("message");
        // TODO: Implement with proper statement generation
        return block;
    }

    @Override
    public UseCaseStep copy() {
        return new ValidateStep(this.getOrder(), this.validationExpression.copy(), this.errorMessage);
    }
}
