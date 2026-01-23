package uet.ndh.ddsl.core.application.applicationservice;

import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.ValidationError;
import uet.ndh.ddsl.core.application.usecase.UseCaseStep;
import uet.ndh.ddsl.core.building.Method;

import java.util.ArrayList;
import java.util.List; /**
 * Represents a use case in an application service.
 */
public class UseCase {
    private final String name;
    private final DataTransferObject inputDto;
    private final DataTransferObject outputDto;
    private final List<UseCaseStep> steps;
    private final boolean transactional;

    public UseCase(String name, DataTransferObject inputDto, DataTransferObject outputDto, boolean transactional) {
        this.name = name;
        this.inputDto = inputDto;
        this.outputDto = outputDto;
        this.transactional = transactional;
        this.steps = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public DataTransferObject getInputDto() {
        return inputDto;
    }

    public DataTransferObject getOutputDto() {
        return outputDto;
    }

    public List<UseCaseStep> getSteps() {
        return new ArrayList<>(steps);
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void addStep(UseCaseStep step) {
        steps.add(step);
    }

    /**
     * Create a deep copy of this use case for normalization.
     */
    public UseCase copy() {
        DataTransferObject copiedInputDto = (inputDto != null) ? inputDto.copy() : null;
        DataTransferObject copiedOutputDto = (outputDto != null) ? outputDto.copy() : null;

        UseCase copy = new UseCase(this.name, copiedInputDto, copiedOutputDto, this.transactional);

        // Copy all steps
        for (UseCaseStep step : this.steps) {
            copy.addStep(step.copy());
        }

        return copy;
    }

    /**
     * Generate use case method.
     * @return Java method for this use case
     */
    public Method generateUseCaseMethod() {
        Method method = new Method(name, new JavaType(outputDto.getName(), ""));
        // TODO: Implement method generation with steps
        return method;
    }

    public List<ValidationError> validate(SourceLocation location) {
        List<ValidationError> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError("Use case name cannot be empty", location));
        }

        // Input DTO is optional for some use cases
        if (inputDto != null) {
            errors.addAll(inputDto.validate(location));
        }

        // Output DTO is also optional for void returns - that's valid
        if (outputDto != null) {
            errors.addAll(outputDto.validate(location));
        }

        return errors;
    }
}
