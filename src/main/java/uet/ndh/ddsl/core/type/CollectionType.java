package uet.ndh.ddsl.core.type;

import uet.ndh.ddsl.core.JavaType;
import java.util.List;

/**
 * Represents a collection type in the DSL type system.
 */
public class CollectionType extends JavaType {
    private final JavaType elementType;
    private final CollectionKind collectionKind;

    public CollectionType(JavaType elementType, CollectionKind collectionKind) {
        super(collectionKind.getTypeName(), collectionKind.getPackageName(),
              true, List.of(elementType));
        this.elementType = elementType;
        this.collectionKind = collectionKind;
    }

    public JavaType getElementType() {
        return elementType;
    }

    public CollectionKind getCollectionKind() {
        return collectionKind;
    }

    @Override
    public String getFullyQualifiedName() {
        return collectionKind.getPackageName() + "." + collectionKind.getTypeName() +
               "<" + elementType.getFullyQualifiedName() + ">";
    }
}
