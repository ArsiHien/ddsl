package uet.ndh.ddsl.codegen.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uet.ndh.ddsl.codegen.template.mapper.TemplateModelMapper;
import uet.ndh.ddsl.codegen.template.model.JavaClassTemplateModel;
import uet.ndh.ddsl.core.codegen.JavaClass;
import uet.ndh.ddsl.core.codegen.JavaEnum;
import uet.ndh.ddsl.core.codegen.JavaInterface;

import java.util.HashMap;
import java.util.Map;

/**
 * Template-based code generator using FreeMarker.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TemplateBasedCodeGenerator {

    private final TemplateService templateService;
    private final TemplateModelMapper modelMapper = new TemplateModelMapper();

    /**
     * Generate Java class source code using FreeMarker template.
     * Automatically selects the appropriate template based on class characteristics.
     *
     * @param javaClass the JavaClass to generate code for
     * @return generated Java source code
     */
    public String generateJavaClass(JavaClass javaClass) {
        log.debug("Generating Java class: {}.{}", javaClass.getPackageName(), javaClass.getClassName());

        try {
            JavaClassTemplateModel templateModel = modelMapper.mapJavaClass(javaClass);
            Map<String, Object> dataModel = createDataModel(templateModel);

            // Determine which template to use based on class characteristics
            String templateName = selectTemplateForClass(javaClass, templateModel);

            return templateService.processTemplate(templateName, dataModel);
        } catch (Exception e) {
            log.error("Failed to generate Java class: {}.{}", javaClass.getPackageName(), javaClass.getClassName(), e);
            // Fallback to original string-based generation
            return javaClass.generateSourceCodeUsingStrings();
        }
    }

    /**
     * Select the appropriate template based on class characteristics.
     */
    private String selectTemplateForClass(JavaClass javaClass, JavaClassTemplateModel templateModel) {
        String className = javaClass.getClassName();

        // Check if this looks like a Value Object
        if (isValueObject(className, templateModel)) {
            log.debug("Using value-object template for {}", className);
            return "value-object-class";
        }

        // Check if this looks like an Entity
        if (isEntity(className, templateModel)) {
            log.debug("Using entity template for {}", className);
            return "entity-class";
        }

        // Default to general class template
        log.debug("Using general java-class template for {}", className);
        return "java-class";
    }

    /**
     * Determine if this class represents a Value Object.
     */
    private boolean isValueObject(String className, JavaClassTemplateModel templateModel) {
        // Heuristics for detecting Value Objects:
        // 1. Class name suggests it's a value (Money, Email, Address, etc.)
        // 2. Has mostly final fields
        // 3. No ID field
        // 4. Small number of fields (typically 1-5)

        String lowerName = className.toLowerCase();
        if (lowerName.contains("money") || lowerName.contains("email") ||
            lowerName.contains("address") || lowerName.contains("title") ||
            lowerName.contains("content") || lowerName.contains("status") ||
            lowerName.endsWith("value") || lowerName.endsWith("vo")) {
            return true;
        }

        // Check field characteristics
        boolean hasIdField = templateModel.getFields().stream()
            .anyMatch(f -> f.getName().toLowerCase().contains("id"));

        boolean mostFieldsFinal = templateModel.getFields().stream()
            .mapToInt(f -> f.isFinal() ? 1 : 0)
            .sum() >= templateModel.getFields().size() * 0.8; // 80% final fields

        int fieldCount = templateModel.getFields().size();

        return !hasIdField && mostFieldsFinal && fieldCount > 0 && fieldCount <= 5;
    }

    /**
     * Determine if this class represents an Entity.
     */
    private boolean isEntity(String className, JavaClassTemplateModel templateModel) {
        // Heuristics for detecting Entities:
        // 1. Has an ID field
        // 2. Not obviously a Value Object
        // 3. Has business methods

        boolean hasIdField = templateModel.getFields().stream()
            .anyMatch(f -> f.getName().toLowerCase().contains("id"));

        boolean hasBusinessMethods = templateModel.getMethods().stream()
            .anyMatch(m -> !m.getName().startsWith("get") &&
                          !m.getName().startsWith("set") &&
                          !m.getName().equals("equals") &&
                          !m.getName().equals("hashCode") &&
                          !m.getName().equals("toString"));

        return hasIdField || hasBusinessMethods;
    }

    /**
     * Generate Java interface source code using FreeMarker template.
     *
     * @param javaInterface the JavaInterface to generate code for
     * @return generated Java source code
     */
    public String generateJavaInterface(JavaInterface javaInterface) {
        log.debug("Generating Java interface: {}.{}", javaInterface.getPackageName(), javaInterface.getInterfaceName());

        try {
            Map<String, Object> dataModel = modelMapper.mapJavaInterface(javaInterface);

            return templateService.processTemplate("java-interface", dataModel);
        } catch (Exception e) {
            log.error("Failed to generate Java interface: {}.{}", javaInterface.getPackageName(), javaInterface.getInterfaceName(), e);
            // Fallback to original string-based generation
            return javaInterface.generateSourceCode();
        }
    }

    /**
     * Generate Java enum source code using FreeMarker template.
     *
     * @param javaEnum the JavaEnum to generate code for
     * @return generated Java source code
     */
    public String generateJavaEnum(JavaEnum javaEnum) {
        log.debug("Generating Java enum: {}.{}", javaEnum.getPackageName(), javaEnum.getEnumName());

        try {
            Map<String, Object> dataModel = modelMapper.mapJavaEnum(javaEnum);

            return templateService.processTemplate("java-enum", dataModel);
        } catch (Exception e) {
            log.error("Failed to generate Java enum: {}.{}", javaEnum.getPackageName(), javaEnum.getEnumName(), e);
            // For now, return a simple enum generation as fallback
            return generateSimpleEnum(javaEnum);
        }
    }

    /**
     * Convert JavaClassTemplateModel to Map for FreeMarker with enhanced context.
     */
    private Map<String, Object> createDataModel(JavaClassTemplateModel templateModel) {
        Map<String, Object> dataModel = new HashMap<>();

        // Basic class information
        dataModel.put("packageName", templateModel.getPackageName());
        dataModel.put("className", templateModel.getClassName());
        dataModel.put("javadoc", templateModel.getJavadoc());
        dataModel.put("isFinal", templateModel.isFinal());
        dataModel.put("isAbstract", templateModel.isAbstract());
        dataModel.put("isPublic", templateModel.isPublic());
        dataModel.put("superClass", templateModel.getSuperClass());
        dataModel.put("superClassImport", templateModel.getSuperClassImport());
        dataModel.put("implementedInterfaces", templateModel.getImplementedInterfaces());
        dataModel.put("interfaceImports", templateModel.getInterfaceImports());
        dataModel.put("sortedImports", templateModel.getSortedImports());
        dataModel.put("fields", templateModel.getFields());
        dataModel.put("constructors", templateModel.getConstructors());
        dataModel.put("methods", templateModel.getMethods());
        dataModel.put("innerClasses", templateModel.getInnerClasses());
        dataModel.put("annotations", templateModel.getAnnotations());

        // Enhanced context for specialized templates
        dataModel.put("isEntity", isEntity(templateModel.getClassName(), templateModel));
        dataModel.put("isValueObject", isValueObject(templateModel.getClassName(), templateModel));
        dataModel.put("isAggregateRoot", isAggregateRoot(templateModel));
        dataModel.put("hasBusinessMethods", hasBusinessMethods(templateModel));
        dataModel.put("domainType", determineDomainType(templateModel));

        return dataModel;
    }

    /**
     * Check if this entity is likely an aggregate root.
     */
    private boolean isAggregateRoot(JavaClassTemplateModel templateModel) {
        // Heuristics: aggregate roots typically have domain events, collections of other entities, etc.
        String className = templateModel.getClassName().toLowerCase();

        // Common aggregate root names
        if (className.contains("order") || className.contains("customer") ||
            className.contains("product") || className.contains("user") ||
            className.contains("account") || className.contains("post")) {
            return true;
        }

        // Has collections that might represent child entities
        boolean hasCollections = templateModel.getFields().stream()
            .anyMatch(f -> f.getType().startsWith("List<") || f.getType().startsWith("Set<"));

        return hasCollections;
    }

    /**
     * Check if this class has business methods (not just getters/setters).
     */
    private boolean hasBusinessMethods(JavaClassTemplateModel templateModel) {
        return templateModel.getMethods().stream()
            .anyMatch(m -> !m.getName().startsWith("get") &&
                          !m.getName().startsWith("set") &&
                          !m.getName().equals("equals") &&
                          !m.getName().equals("hashCode") &&
                          !m.getName().equals("toString"));
    }

    /**
     * Determine the domain type for template selection.
     */
    private String determineDomainType(JavaClassTemplateModel templateModel) {
        if (isValueObject(templateModel.getClassName(), templateModel)) {
            return "VALUE_OBJECT";
        } else if (isEntity(templateModel.getClassName(), templateModel)) {
            return isAggregateRoot(templateModel) ? "AGGREGATE_ROOT" : "ENTITY";
        } else if (templateModel.getClassName().endsWith("Service")) {
            return "DOMAIN_SERVICE";
        } else if (templateModel.getClassName().endsWith("Repository")) {
            return "REPOSITORY";
        } else if (templateModel.getClassName().endsWith("Factory")) {
            return "FACTORY";
        } else {
            return "GENERIC_CLASS";
        }
    }

    /**
     * Simple fallback enum generation.
     */
    private String generateSimpleEnum(JavaEnum javaEnum) {
        StringBuilder sb = new StringBuilder();

        // Package
        if (javaEnum.getPackageName() != null && !javaEnum.getPackageName().isEmpty()) {
            sb.append("package ").append(javaEnum.getPackageName()).append(";\n\n");
        }

        // Class declaration
        sb.append("/**\n * Generated enum: ").append(javaEnum.getEnumName()).append("\n */\n");
        sb.append("public enum ").append(javaEnum.getEnumName()).append(" {\n");

        // Constants
        if (!javaEnum.getConstants().isEmpty()) {
            for (int i = 0; i < javaEnum.getConstants().size(); i++) {
                sb.append("    ").append(javaEnum.getConstants().get(i));
                if (i < javaEnum.getConstants().size() - 1) {
                    sb.append(",");
                } else {
                    sb.append(";");
                }
                sb.append("\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}

