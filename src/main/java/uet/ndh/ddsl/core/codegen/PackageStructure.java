package uet.ndh.ddsl.core.codegen;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map; /**
 * Represents the package structure for generated code.
 */
public class PackageStructure {
    @Getter
    private final String basePackage;
    @Getter
    private final String domainPackage;
    @Getter
    private final String applicationPackage;
    private final Map<String, String> domainSubPackages;

    public PackageStructure(String basePackage) {
        this.basePackage = basePackage;
        this.domainPackage = basePackage + ".domain";
        this.applicationPackage = basePackage + ".application";
        this.domainSubPackages = new HashMap<>();
        initializeDefaultSubPackages();
    }

    private void initializeDefaultSubPackages() {
        domainSubPackages.put("model", domainPackage + ".model");
        domainSubPackages.put("service", domainPackage + ".service");
        domainSubPackages.put("event", domainPackage + ".event");
        domainSubPackages.put("repository", domainPackage + ".repository");
        domainSubPackages.put("specification", domainPackage + ".specification");
        domainSubPackages.put("factory", domainPackage + ".factory");
    }

    public Map<String, String> getDomainSubPackages() {
        return new HashMap<>(domainSubPackages);
    }

    public String getPackageForType(String type) {
        return domainSubPackages.getOrDefault(type, domainPackage);
    }

    public void addSubPackage(String type, String packageName) {
        domainSubPackages.put(type, packageName);
    }
}
