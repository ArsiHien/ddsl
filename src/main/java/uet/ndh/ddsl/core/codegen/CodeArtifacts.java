package uet.ndh.ddsl.core.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains all generated code artifacts.
 */
public class CodeArtifacts {
    private final List<JavaClass> javaClasses;
    private final List<JavaInterface> interfaces;
    private final List<JavaEnum> enums;
    private final PackageStructure packageStructure;

    public CodeArtifacts(PackageStructure packageStructure) {
        this.packageStructure = packageStructure;
        this.javaClasses = new ArrayList<>();
        this.interfaces = new ArrayList<>();
        this.enums = new ArrayList<>();
    }

    public List<JavaClass> getJavaClasses() {
        return new ArrayList<>(javaClasses);
    }

    public List<JavaInterface> getInterfaces() {
        return new ArrayList<>(interfaces);
    }

    public List<JavaEnum> getEnums() {
        return new ArrayList<>(enums);
    }

    public PackageStructure getPackageStructure() {
        return packageStructure;
    }

    public void addClass(JavaClass javaClass) {
        javaClasses.add(javaClass);
    }

    public void addInterface(JavaInterface javaInterface) {
        interfaces.add(javaInterface);
    }

    public void addEnum(JavaEnum javaEnum) {
        enums.add(javaEnum);
    }

    /**
     * Write all generated artifacts to the file system.
     * @param basePath Base path for generated files
     */
    public void writeToFileSystem(String basePath) {
        // TODO: Implement file system writing
        // This would write all classes, interfaces, and enums to appropriate directories
        throw new UnsupportedOperationException("File system writing not implemented yet");
    }

    /**
     * Get a summary of all generated artifacts.
     * @return Summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Code Artifacts Summary:\n");
        sb.append("======================\n");
        sb.append("Base Package: ").append(packageStructure.getBasePackage()).append("\n");
        sb.append("Classes: ").append(javaClasses.size()).append("\n");
        sb.append("Interfaces: ").append(interfaces.size()).append("\n");
        sb.append("Enums: ").append(enums.size()).append("\n");
        sb.append("\nPackage Structure:\n");
        sb.append("- Domain: ").append(packageStructure.getDomainPackage()).append("\n");
        sb.append("- Application: ").append(packageStructure.getApplicationPackage()).append("\n");
        for (Map.Entry<String, String> entry : packageStructure.getDomainSubPackages().entrySet()) {
            sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
