package uet.ndh.ddsl.ast.common;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a type reference in the AST.
 * This is a syntactic representation - semantic resolution happens later.
 * 
 * Pure data record - no logic except accept().
 */
public record TypeRef(
    SourceSpan span,
    String name,
    List<TypeRef> typeArguments,
    boolean isNullable,
    boolean isCollection,
    CollectionKind collectionKind
) implements AstNode {
    
    public TypeRef {
        typeArguments = typeArguments != null ? List.copyOf(typeArguments) : List.of();
    }
    
    /**
     * Create a simple type reference with no generics.
     */
    public static TypeRef simple(SourceSpan span, String name) {
        return new TypeRef(span, name, List.of(), false, false, null);
    }
    
    /**
     * Create a nullable type reference.
     */
    public static TypeRef nullable(SourceSpan span, String name) {
        return new TypeRef(span, name, List.of(), true, false, null);
    }
    
    /**
     * Create a collection type reference.
     */
    public static TypeRef collection(SourceSpan span, CollectionKind kind, TypeRef elementType) {
        return new TypeRef(span, kind.name(), List.of(elementType), false, true, kind);
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitTypeRef(this);
    }
    
    /**
     * Kind of collection.
     */
    public enum CollectionKind {
        LIST,
        SET,
        MAP,
        ARRAY
    }
}
