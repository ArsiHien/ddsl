package uet.ndh.ddsl.core;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Java type in the DSL type system.
 */
public class JavaType {
    @Getter
    private final String simpleName;
    @Getter
    private final String packageName;
    @Getter
    private final boolean isGeneric;
    private final List<JavaType> typeParameters;

    public JavaType(String simpleName, String packageName) {
        this(simpleName, packageName, false, new ArrayList<>());
    }

    public JavaType(String simpleName, String packageName, boolean isGeneric, List<JavaType> typeParameters) {
        this.simpleName = simpleName;
        this.packageName = packageName;
        this.isGeneric = isGeneric;
        this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
    }

    public List<JavaType> getTypeParameters() {
        return new ArrayList<>(typeParameters);
    }

    public String getFullyQualifiedName() {
        if (packageName == null || packageName.isEmpty()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }

    public String getImportStatement() {
        if (packageName == null || packageName.isEmpty() ||
            "java.lang".equals(packageName)) {
            return null;
        }
        return "import " + getFullyQualifiedName() + ";";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaType javaType = (JavaType) o;
        return Objects.equals(simpleName, javaType.simpleName) &&
               Objects.equals(packageName, javaType.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simpleName, packageName);
    }

    @Override
    public String toString() {
        return getFullyQualifiedName();
    }
}
