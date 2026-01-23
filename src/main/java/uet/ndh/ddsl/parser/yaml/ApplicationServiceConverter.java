package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.application.applicationservice.ApplicationService;
import uet.ndh.ddsl.core.application.applicationservice.UseCase;
import uet.ndh.ddsl.core.application.applicationservice.Dependency;
import uet.ndh.ddsl.core.application.applicationservice.DataTransferObject;
import uet.ndh.ddsl.core.application.usecase.UseCaseStep;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML application service data to ApplicationService AST.
 */
public class ApplicationServiceConverter extends BaseYamlConverter {

    private final JavaTypeConverter typeConverter;
    private final FieldConverter fieldConverter;

    public ApplicationServiceConverter() {
        this.typeConverter = new JavaTypeConverter();
        this.fieldConverter = new FieldConverter(typeConverter);
    }

    public ApplicationService convert(Map<String, Object> data, SourceLocation location, LocationTracker locationTracker, String servicePath)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String description = getOptionalString(data, "description", null);

        ApplicationService service = new ApplicationService(location, name);
        if (description != null) {
            service.setDocumentation(description);
        }

        // Convert dependencies
        List<Object> dependenciesData = getOptionalList(data, "dependencies");
        for (int i = 0; i < dependenciesData.size(); i++) {
            Object depData = dependenciesData.get(i);
            if (depData instanceof Map) {
                String depPath = servicePath + ".dependencies[" + i + "]";
                SourceLocation depLocation = locationTracker.getLocationForKey(depPath);
                Dependency dependency = convertDependency((Map<String, Object>) depData, depLocation);
                service.addDependency(dependency);
            }
        }

        // Convert use cases
        List<Object> useCasesData = getOptionalList(data, "useCases");
        for (int i = 0; i < useCasesData.size(); i++) {
            Object ucData = useCasesData.get(i);
            if (ucData instanceof Map) {
                String useCasePath = servicePath + ".useCases[" + i + "]";
                SourceLocation useCaseLocation = locationTracker.getLocationForKey(useCasePath);
                UseCase useCase = convertUseCase((Map<String, Object>) ucData, useCaseLocation, locationTracker, useCasePath);
                service.addUseCase(useCase);
            }
        }

        return service;
    }

    // Keep the old method for backward compatibility if needed
    public ApplicationService convert(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        return convert(data, location, null, "");
    }

    private Dependency convertDependency(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String typeString = getRequiredString(data, "type", location);
        boolean isRequired = getOptionalBoolean(data, "required", true);

        JavaType type = typeConverter.convertType(typeString, location);

        return new Dependency(name, type, isRequired);
    }

    private UseCase convertUseCase(Map<String, Object> data, SourceLocation location, LocationTracker locationTracker, String useCasePath)
            throws ParseException {
        String name = getRequiredString(data, "name", location);
        String description = getOptionalString(data, "description", null);
        boolean transactional = getOptionalBoolean(data, "transactional", false);

        // Convert input DTO
        DataTransferObject inputDto = null;
        Map<String, Object> inputData = getOptionalMap(data, "input");
        if (inputData != null) {
            SourceLocation inputLocation = locationTracker != null ?
                locationTracker.getLocationForKey(useCasePath + ".input") : location;
            inputDto = convertDto(inputData, inputLocation, locationTracker, useCasePath + ".input");
        }

        // Convert output DTO
        DataTransferObject outputDto = null;
        Map<String, Object> outputData = getOptionalMap(data, "output");
        if (outputData != null) {
            SourceLocation outputLocation = locationTracker != null ?
                locationTracker.getLocationForKey(useCasePath + ".output") : location;

            // Check if this is a simple type (like "void") rather than a DTO
            String outputType = getOptionalString(outputData, "type", null);
            if (outputType != null && !outputData.containsKey("name") && !outputData.containsKey("fields")) {
                // This is a simple type like "void", not a DTO definition
                if (!"void".equalsIgnoreCase(outputType)) {
                    // Create a simple DTO wrapper for non-void simple types
                    outputDto = new DataTransferObject("ReturnValue");
                    // Add the return value as a field
                    JavaType returnType = typeConverter.convertType(outputType, outputLocation);
                    Field returnField = new Field("value", returnType,
                        uet.ndh.ddsl.core.building.Visibility.PUBLIC, true, false, null);
                    outputDto.addField(returnField);
                }
                // For void types, outputDto remains null
            } else {
                // This is a proper DTO definition with name and fields
                outputDto = convertDto(outputData, outputLocation, locationTracker, useCasePath + ".output");
            }
        }

        UseCase useCase = new UseCase(name, inputDto, outputDto, transactional);
        // Note: UseCase doesn't extend ASTNode, so no setDocumentation method

        // Convert steps - for now we'll store them as raw code
        List<Object> stepsData = getOptionalList(data, "steps");
        for (Object stepData : stepsData) {
            if (stepData instanceof Map) {
                // For simplicity, we'll convert steps to raw code blocks
                Map<String, Object> stepMap = (Map<String, Object>) stepData;
                String stepCode = getOptionalString(stepMap, "code", "");
                if (!stepCode.isEmpty()) {
                    // We'd need a more sophisticated step converter here
                    // For now, just store as documentation
                }
            }
        }

        return useCase;
    }

    // Keep the old method for backward compatibility
    private UseCase convertUseCase(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        return convertUseCase(data, location, null, "");
    }

    private DataTransferObject convertDto(Map<String, Object> data, SourceLocation location, LocationTracker locationTracker, String dtoPath)
            throws ParseException {
        SourceLocation nameLocation = locationTracker != null ?
            locationTracker.getLocationForKey(dtoPath + ".name") : location;
        String name = getRequiredString(data, "name", nameLocation);

        DataTransferObject dto = new DataTransferObject(name);

        // Convert fields
        List<Object> fieldsData = getOptionalList(data, "fields");
        for (int i = 0; i < fieldsData.size(); i++) {
            Object fieldData = fieldsData.get(i);
            if (fieldData instanceof Map) {
                SourceLocation fieldLocation = locationTracker != null ?
                    locationTracker.getLocationForKey(dtoPath + ".fields[" + i + "]") : location;
                Field field = fieldConverter.convert((Map<String, Object>) fieldData, fieldLocation);
                dto.addField(field);
            }
        }

        return dto;
    }

    // Keep the old method for backward compatibility
    private DataTransferObject convertDto(Map<String, Object> data, SourceLocation location)
            throws ParseException {
        return convertDto(data, location, null, "");
    }
}
