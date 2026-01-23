package uet.ndh.ddsl.core.type;

/**
 * Enumeration of collection types supported by the DSL.
 */
public enum CollectionKind {
    LIST("List", "java.util"),
    SET("Set", "java.util"),
    MAP("Map", "java.util");

    private final String typeName;
    private final String packageName;

    CollectionKind(String typeName, String packageName) {
        this.typeName = typeName;
        this.packageName = packageName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getPackageName() {
        return packageName;
    }
}
