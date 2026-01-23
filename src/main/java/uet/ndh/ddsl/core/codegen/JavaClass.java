package uet.ndh.ddsl.core.codegen;

import lombok.Getter;
import lombok.Setter;
import uet.ndh.ddsl.codegen.template.TemplateBasedCodeGenerator;
import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.building.Field;
import uet.ndh.ddsl.core.building.Method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a generated Java class.
 */
@Getter
public class JavaClass {
    private final String packageName;
    private final String className;
    @Setter
    private JavaType superClass;
    private final List<JavaType> interfaces;
    private final List<Field> fields;
    private final List<Constructor> constructors;
    private final List<Method> methods;
    private final List<JavaClass> innerClasses;
    private final Set<String> imports;
    private final boolean isFinal;
    private final boolean isAbstract;

    // Template-based generation
    @Setter
    private TemplateBasedCodeGenerator templateGenerator;

    public JavaClass(String packageName, String className, boolean isFinal, boolean isAbstract) {
        this.packageName = packageName;
        this.className = className;
        this.isFinal = isFinal;
        this.isAbstract = isAbstract;
        this.interfaces = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.constructors = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.innerClasses = new ArrayList<>();
        this.imports = new HashSet<>();
    }

    public JavaClass(String packageName, String className) {
        this(packageName, className, false, false);
    }

    public List<JavaType> getInterfaces() {
        return new ArrayList<>(interfaces);
    }

    public List<Field> getFields() {
        return new ArrayList<>(fields);
    }

    public List<Constructor> getConstructors() {
        return new ArrayList<>(constructors);
    }

    public List<Method> getMethods() {
        return new ArrayList<>(methods);
    }

    public List<JavaClass> getInnerClasses() {
        return new ArrayList<>(innerClasses);
    }

    public Set<String> getImports() {
        return new HashSet<>(imports);
    }

    public void addInterface(JavaType interfaceType) {
        interfaces.add(interfaceType);
        addImport(interfaceType.getImportStatement());
    }

    public void addField(Field field) {
        fields.add(field);
        // Auto-add imports for field types
        if (field.getType() != null) {
            addImport(field.getType().getImportStatement());
        }
    }

    public void addConstructor(Constructor constructor) {
        constructors.add(constructor);
    }

    public void addMethod(Method method) {
        methods.add(method);
        // Auto-add imports for return type and parameter types
        if (method.getReturnType() != null) {
            addImport(method.getReturnType().getImportStatement());
        }
        method.getParameters().forEach(param -> {
            if (param.type() != null) {
                addImport(param.type().getImportStatement());
            }
        });
    }

    public void addInnerClass(JavaClass innerClass) {
        innerClasses.add(innerClass);
    }

    public void addImport(String importStatement) {
        if (importStatement != null &&
            !importStatement.isEmpty() &&
            !importStatement.startsWith("java.lang.") && // Skip java.lang imports
            !isInSamePackage(importStatement)) {
            imports.add(importStatement);
        }
    }

    private boolean isInSamePackage(String importStatement) {
        if (packageName == null || importStatement == null) return false;
        return importStatement.startsWith(packageName + ".");
    }

    /**
     * Generate complete Java source code for this class.
     * Uses template-based generation if available, falls back to string-based generation.
     * @return Complete Java class source code
     */
    public String generateSourceCode() {
        // Use template-based generation if available
        if (templateGenerator != null) {
            try {
                return templateGenerator.generateJavaClass(this);
            } catch (Exception e) {
                // Fall back to string-based generation on any error
                System.err.println("Template generation failed for " + className + ", falling back to string generation: " + e.getMessage());
            }
        }

        // Original string-based generation as fallback
        return generateSourceCodeUsingStrings();
    }

    /**
     * Original string-based source code generation.
     * @return Complete Java class source code
     */
    public String generateSourceCodeUsingStrings() {
        StringBuilder sb = new StringBuilder();

        // Package declaration
        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        // Imports
        List<String> sortedImports = new ArrayList<>(imports);
        sortedImports.sort(String::compareTo);
        for (String importStmt : sortedImports) {
            if (!importStmt.trim().isEmpty()) {
                sb.append(importStmt.startsWith("import ") ? importStmt : "import " + importStmt)
                  .append(";\n");
            }
        }
        if (!imports.isEmpty()) {
            sb.append("\n");
        }

        // Class javadoc (optional)
        sb.append("/**\n");
        sb.append(" * Generated class: ").append(className).append("\n");
        sb.append(" */\n");

        // Class declaration
        sb.append("public ");
        if (isAbstract) sb.append("abstract ");
        if (isFinal) sb.append("final ");
        sb.append("class ").append(className);

        // Extends clause
        if (superClass != null) {
            sb.append(" extends ").append(superClass.getSimpleName());
            addImport(superClass.getImportStatement());
        }

        // Implements clause
        if (!interfaces.isEmpty()) {
            sb.append(" implements ");
            for (int i = 0; i < interfaces.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(interfaces.get(i).getSimpleName());
            }
        }

        sb.append(" {\n");

        // Fields
        for (Field field : fields) {
            String fieldDeclaration = field.generateFieldDeclaration();
            sb.append(indentCode(fieldDeclaration, 1)).append("\n");
        }
        if (!fields.isEmpty()) sb.append("\n");

        // Constructors
        for (Constructor constructor : constructors) {
            String constructorCode = constructor.generateConstructorCode(className);
            sb.append(indentCode(constructorCode, 1)).append("\n\n");
        }

        // Methods
        for (Method method : methods) {
            String methodCode = method.generateMethodBody();
            sb.append(indentCode(methodCode, 1)).append("\n\n");
        }

        // Inner classes
        for (JavaClass innerClass : innerClasses) {
            String innerClassCode = innerClass.generateSourceCode();
            sb.append(indentCode(innerClassCode, 1)).append("\n\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private String indentCode(String code, int indentLevel) {
        if (code == null || code.isEmpty()) return "";

        String indent = "    ".repeat(indentLevel);
        return code.lines()
                   .map(line -> line.isEmpty() ? line : indent + line)
                   .reduce((a, b) -> a + "\n" + b)
                   .orElse("");
    }
}
