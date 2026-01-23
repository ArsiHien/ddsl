package uet.ndh.ddsl.core.type;

import uet.ndh.ddsl.core.JavaType;

/**
 * Represents a custom type (domain objects) in the DSL type system.
 */
public class CustomType extends JavaType {
    private final boolean isValueObject;
    private final boolean isEntity;
    private final boolean isEnum;

    public CustomType(String simpleName, String packageName,
                     boolean isValueObject, boolean isEntity, boolean isEnum) {
        super(simpleName, packageName);
        this.isValueObject = isValueObject;
        this.isEntity = isEntity;
        this.isEnum = isEnum;
    }

    public boolean isValueObject() {
        return isValueObject;
    }

    public boolean isEntity() {
        return isEntity;
    }

    public boolean isEnum() {
        return isEnum;
    }
}
