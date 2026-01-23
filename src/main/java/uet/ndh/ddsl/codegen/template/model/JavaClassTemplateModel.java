package uet.ndh.ddsl.codegen.template.model;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Set;

/**
 * Template data model for Java class generation.
 */
@Data
@Builder
public class JavaClassTemplateModel {

    // Basic class information
    private String packageName;
    private String className;
    private String javadoc;

    // Class modifiers
    private boolean isFinal;
    private boolean isAbstract;
    private boolean isPublic;

    // Inheritance and interfaces
    private String superClass;
    private String superClassImport;

    @Singular
    private List<String> implementedInterfaces;

    @Singular
    private List<String> interfaceImports;

    // Imports
    @Singular
    private Set<String> imports;

    // Class contents
    @Singular
    private List<FieldTemplateModel> fields;

    @Singular
    private List<ConstructorTemplateModel> constructors;

    @Singular
    private List<MethodTemplateModel> methods;

    @Singular
    private List<JavaClassTemplateModel> innerClasses;

    // Annotations
    @Singular
    private List<String> annotations;

    /**
     * Check if this class has any content (fields, constructors, methods)
     */
    public boolean hasContent() {
        return !fields.isEmpty() || !constructors.isEmpty() || !methods.isEmpty();
    }

    /**
     * Get all imports sorted for consistent output
     */
    public List<String> getSortedImports() {
        return imports.stream()
            .filter(imp -> imp != null && !imp.trim().isEmpty())
            .sorted()
            .toList();
    }
}
