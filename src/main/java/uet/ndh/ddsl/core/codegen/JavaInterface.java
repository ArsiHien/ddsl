package uet.ndh.ddsl.core.codegen;

import lombok.Getter;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.building.Method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a generated Java interface.
 */
public class JavaInterface {
    @Getter
    private final String packageName;
    @Getter
    private final String interfaceName;
    private final List<Method> methods;
    private final List<JavaType> superInterfaces;
    private final Set<String> imports;

    public JavaInterface(String packageName, String interfaceName) {
        this.packageName = packageName;
        this.interfaceName = interfaceName;
        this.methods = new ArrayList<>();
        this.superInterfaces = new ArrayList<>();
        this.imports = new HashSet<>();
    }

    public List<Method> getMethods() {
        return new ArrayList<>(methods);
    }

    public List<JavaType> getSuperInterfaces() {
        return new ArrayList<>(superInterfaces);
    }

    public void addMethod(Method method) {
        methods.add(method);
    }

    public void addSuperInterface(JavaType superInterface) {
        superInterfaces.add(superInterface);
    }

    public void addImport(String importStatement) {
        if (importStatement != null && !importStatement.isEmpty()) {
            imports.add(importStatement);
        }
    }

    /**
     * Generate complete Java interface source code.
     * @return Complete Java interface source code
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

        // Interface declaration
        sb.append("public interface ").append(interfaceName);

        // Extends clause
        if (!superInterfaces.isEmpty()) {
            sb.append(" extends ");
            for (int i = 0; i < superInterfaces.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(superInterfaces.get(i).getSimpleName());
            }
        }

        sb.append(" {\n");

        // Methods (interface methods are abstract by default)
        for (Method method : methods) {
            sb.append("    ").append(method.generateMethodSignature()).append(";\n\n");
        }

        sb.append("}");
        return sb.toString();
    }
}
