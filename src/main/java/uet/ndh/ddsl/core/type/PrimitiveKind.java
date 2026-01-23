package uet.ndh.ddsl.core.type;

import uet.ndh.ddsl.core.JavaType;

/**
 * Enumeration of primitive types supported by the DSL.
 */
public enum PrimitiveKind {
    INT("int", "java.lang"),
    LONG("long", "java.lang"),
    DOUBLE("double", "java.lang"),
    BOOLEAN("boolean", "java.lang"),
    STRING("String", "java.lang"),
    BIG_DECIMAL("BigDecimal", "java.math"),
    UUID("UUID", "java.util"),
    INSTANT("Instant", "java.time"),
    LOCAL_DATE("LocalDate", "java.time"),
    LOCAL_DATE_TIME("LocalDateTime", "java.time");

    private final String typeName;
    private final String packageName;

    PrimitiveKind(String typeName, String packageName) {
        this.typeName = typeName;
        this.packageName = packageName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getPackageName() {
        return packageName;
    }

    public JavaType toJavaType() {
        return new JavaType(typeName, packageName);
    }
}
