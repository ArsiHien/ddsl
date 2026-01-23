package uet.ndh.ddsl.codegen.template.mapper;

import uet.ndh.ddsl.codegen.template.model.*;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;
import uet.ndh.ddsl.core.building.Parameter;
import uet.ndh.ddsl.core.building.Visibility;
import uet.ndh.ddsl.core.codegen.Constructor;
import uet.ndh.ddsl.core.codegen.JavaClass;
import uet.ndh.ddsl.core.codegen.JavaEnum;
import uet.ndh.ddsl.core.codegen.JavaInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps domain objects to FreeMarker template models.
 */
public class TemplateModelMapper {

    /**
     * Convert JavaClass to template model for FreeMarker.
     */
    public JavaClassTemplateModel mapJavaClass(JavaClass javaClass) {
        JavaClassTemplateModel.JavaClassTemplateModelBuilder builder = JavaClassTemplateModel.builder()
            .packageName(javaClass.getPackageName())
            .className(javaClass.getClassName())
            .javadoc("Generated class: " + javaClass.getClassName())
            .isFinal(javaClass.isFinal())
            .isAbstract(javaClass.isAbstract())
            .isPublic(true);

        // Handle superclass
        if (javaClass.getSuperClass() != null) {
            builder.superClass(javaClass.getSuperClass().getSimpleName())
                   .superClassImport(javaClass.getSuperClass().getImportStatement());
        }

        // Handle interfaces
        for (var interfaceType : javaClass.getInterfaces()) {
            builder.implementedInterface(interfaceType.getSimpleName())
                   .interfaceImport(interfaceType.getImportStatement());
        }

        // Handle imports
        Set<String> allImports = new HashSet<>(javaClass.getImports());
        builder.imports(allImports);

        // Map fields
        for (Field field : javaClass.getFields()) {
            builder.field(mapField(field));
        }

        // Map constructors
        for (Constructor constructor : javaClass.getConstructors()) {
            builder.constructor(mapConstructor(constructor, javaClass.getClassName()));
        }

        // Map methods
        for (Method method : javaClass.getMethods()) {
            builder.method(mapMethod(method));
        }

        // Map inner classes
        for (JavaClass innerClass : javaClass.getInnerClasses()) {
            builder.innerClass(mapJavaClass(innerClass));
        }

        return builder.build();
    }

    /**
     * Convert JavaInterface to template model for FreeMarker.
     */
    public Map<String, Object> mapJavaInterface(JavaInterface javaInterface) {
        Map<String, Object> model = new HashMap<>();

        model.put("packageName", javaInterface.getPackageName());
        model.put("interfaceName", javaInterface.getInterfaceName());
        model.put("javadoc", "Generated interface: " + javaInterface.getInterfaceName());

        // Handle super interfaces
        List<String> superInterfaceNames = new ArrayList<>();
        for (var superInterface : javaInterface.getSuperInterfaces()) {
            superInterfaceNames.add(superInterface.getSimpleName());
        }
        model.put("superInterfaces", superInterfaceNames);

        // Handle imports
        Set<String> sortedImports = new HashSet<>();
        // Add imports from the interface
        model.put("sortedImports", new ArrayList<>(sortedImports));

        // Map methods
        List<MethodTemplateModel> methodModels = new ArrayList<>();
        for (Method method : javaInterface.getMethods()) {
            methodModels.add(mapMethod(method));
        }
        model.put("methods", methodModels);

        return model;
    }

    /**
     * Convert JavaEnum to template model for FreeMarker.
     */
    public Map<String, Object> mapJavaEnum(JavaEnum javaEnum) {
        Map<String, Object> model = new HashMap<>();

        model.put("packageName", javaEnum.getPackageName());
        model.put("enumName", javaEnum.getEnumName());
        model.put("javadoc", "Generated enum: " + javaEnum.getEnumName());
        model.put("constants", javaEnum.getConstants());

        // Handle imports
        Set<String> sortedImports = new HashSet<>();
        model.put("sortedImports", new ArrayList<>(sortedImports));

        // Map fields
        List<FieldTemplateModel> fieldModels = new ArrayList<>();
        for (Field field : javaEnum.getFields()) {
            fieldModels.add(mapField(field));
        }
        model.put("fields", fieldModels);

        // Map methods
        List<MethodTemplateModel> methodModels = new ArrayList<>();
        for (Method method : javaEnum.getMethods()) {
            methodModels.add(mapMethod(method));
        }
        model.put("methods", methodModels);

        return model;
    }

    /**
     * Map Field to FieldTemplateModel.
     */
    private FieldTemplateModel mapField(Field field) {
        FieldTemplateModel.FieldTemplateModelBuilder builder = FieldTemplateModel.builder()
            .name(field.getName())
            .type(field.getType().getSimpleName())
            .visibility(mapVisibility(field.getVisibility()))
            .isStatic(false) // Fields are typically not static in domain models
            .isFinal(field.isFinal())
            .isTransient(false)
            .isVolatile(false);

        // Handle type import
        if (field.getType().getImportStatement() != null && !field.getType().getImportStatement().isEmpty()) {
            builder.typeImport(field.getType().getImportStatement());
        }

        // Handle default value
        if (field.getDefaultValue() != null) {
            builder.initialValue(field.getDefaultValue());
        }

        return builder.build();
    }

    /**
     * Map Constructor to ConstructorTemplateModel.
     */
    private ConstructorTemplateModel mapConstructor(Constructor constructor, String className) {
        ConstructorTemplateModel.ConstructorTemplateModelBuilder builder = ConstructorTemplateModel.builder()
            .className(className)
            .visibility(mapConstructorVisibility(constructor.getVisibility()));

        // Map parameters
        for (Parameter param : constructor.getParameters()) {
            builder.parameter(mapParameter(param));
        }

        // Map body
        if (constructor.getBody() != null) {
            String body = constructor.getBody().generateCode();
            if (body != null && !body.trim().isEmpty()) {
                builder.body(body.trim());
            }
        }

        return builder.build();
    }

    /**
     * Map Method to MethodTemplateModel.
     */
    private MethodTemplateModel mapMethod(Method method) {
        MethodTemplateModel.MethodTemplateModelBuilder builder = MethodTemplateModel.builder()
            .name(method.getName())
            .returnType(method.getReturnType().getSimpleName())
            .visibility(mapVisibility(method.getVisibility()))
            .isStatic(method.isStatic())
            .isFinal(method.isFinal())
            .isAbstract(false) // Methods in our domain are typically concrete
            .isSynchronized(false);

        // Map parameters
        for (Parameter param : method.getParameters()) {
            builder.parameter(mapParameter(param));
        }

        // Map thrown exceptions
        for (var exception : method.getThrowsExceptions()) {
            builder.thrownException(exception.getSimpleName())
                   .exceptionImport(exception.getImportStatement());
        }

        // Map body
        if (method.getBody() != null) {
            String body = method.getBody().generateCode();
            if (body != null && !body.trim().isEmpty()) {
                builder.body(body.trim());
            }
        }

        return builder.build();
    }

    /**
     * Map Parameter to ParameterTemplateModel.
     */
    private ParameterTemplateModel mapParameter(Parameter param) {
        ParameterTemplateModel.ParameterTemplateModelBuilder builder = ParameterTemplateModel.builder()
            .name(param.name())
            .type(param.type().getSimpleName())
            .isFinal(param.isFinal());

        // Handle type import
        if (param.type().getImportStatement() != null && !param.type().getImportStatement().isEmpty()) {
            builder.typeImport(param.type().getImportStatement());
        }

        return builder.build();
    }

    /**
     * Map Visibility enum to string.
     */
    private String mapVisibility(Visibility visibility) {
        return switch (visibility) {
            case PUBLIC -> "public";
            case PRIVATE -> "private";
            case PROTECTED -> "protected";
            case PACKAGE_PRIVATE -> "package";
        };
    }

    /**
     * Map Constructor.Visibility to string.
     */
    private String mapConstructorVisibility(Constructor.Visibility visibility) {
        return switch (visibility) {
            case PUBLIC -> "public";
            case PRIVATE -> "private";
            case PROTECTED -> "protected";
        };
    }
}
