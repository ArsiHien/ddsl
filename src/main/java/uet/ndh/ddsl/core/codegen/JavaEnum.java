package uet.ndh.ddsl.core.codegen;

import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a generated Java enum.
 */
public class JavaEnum {
    private final String packageName;
    private final String enumName;
    private final List<String> constants;
    private final List<Field> fields;
    private final List<Method> methods;
    private final Set<String> imports;

    public JavaEnum(String packageName, String enumName) {
        this.packageName = packageName;
        this.enumName = enumName;
        this.constants = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.imports = new HashSet<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getEnumName() {
        return enumName;
    }

    public List<String> getConstants() {
        return new ArrayList<>(constants);
    }

    public List<Field> getFields() {
        return new ArrayList<>(fields);
    }

    public List<Method> getMethods() {
        return new ArrayList<>(methods);
    }

    public void addConstant(String constant) {
        constants.add(constant);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public void addImport(String importStatement) {
        if (importStatement != null && !importStatement.isEmpty()) {
            imports.add(importStatement);
        }
    }

    /**
     * Generate complete Java enum source code.
     * @return Complete Java enum source code
     */
    public String generateSourceCode() {
        StringBuilder sb = new StringBuilder();

        // Package declaration
        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        // Imports
        for (String importStmt : imports) {
            sb.append(importStmt).append("\n");
        }
        if (!imports.isEmpty()) {
            sb.append("\n");
        }

        // Enum declaration
        sb.append("public enum ").append(enumName).append(" {\n");

        // Constants
        for (int i = 0; i < constants.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append("    ").append(constants.get(i));
        }
        if (!constants.isEmpty() && (!fields.isEmpty() || !methods.isEmpty())) {
            sb.append(";\n\n");
        } else if (!constants.isEmpty()) {
            sb.append("\n");
        }

        // Fields
        for (Field field : fields) {
            sb.append("    ").append(field.generateFieldDeclaration()).append("\n");
        }
        if (!fields.isEmpty()) sb.append("\n");

        // Methods
        for (Method method : methods) {
            sb.append("    ").append(method.generateMethodBody().replace("\n", "\n    ")).append("\n\n");
        }

        sb.append("}");
        return sb.toString();
    }
}
