package uet.ndh.ddsl.core.type;

import uet.ndh.ddsl.core.JavaType;

/**
 * Represents a primitive type in the DSL type system.
 */
public class PrimitiveType extends JavaType {
    private final PrimitiveKind primitiveKind;

    public PrimitiveType(PrimitiveKind primitiveKind) {
        super(primitiveKind.getTypeName(), primitiveKind.getPackageName());
        this.primitiveKind = primitiveKind;
    }

    public PrimitiveKind getPrimitiveKind() {
        return primitiveKind;
    }
}
