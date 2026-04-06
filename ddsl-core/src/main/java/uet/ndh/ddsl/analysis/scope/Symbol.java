package uet.ndh.ddsl.analysis.scope;

import uet.ndh.ddsl.ast.AstNode;

/**
 * Represents a symbol in the symbol table.
 * Symbols are entries that represent named entities in the source code.
 */
public record Symbol(
    String name,
    SymbolKind kind,
    AstNode declaration,
    Scope scope,
    TypeInfo type
) {
    
    public enum SymbolKind {
        BOUNDED_CONTEXT,
        MODULE,
        AGGREGATE,
        ENUM,
        ENTITY,
        VALUE_OBJECT,
        DOMAIN_SERVICE,
        DOMAIN_EVENT,
        REPOSITORY,
        FACTORY,
        SPECIFICATION,
        APPLICATION_SERVICE,
        USE_CASE,
        FIELD,
        METHOD,
        PARAMETER,
        LOCAL_VARIABLE,
        INVARIANT,
        BEHAVIOR
    }
    
    /**
     * Type information for the symbol.
     */
    public record TypeInfo(
        String typeName,
        boolean isCollection,
        String elementType
    ) {
        public static TypeInfo simple(String typeName) {
            return new TypeInfo(typeName, false, null);
        }
        
        public static TypeInfo collection(String collectionType, String elementType) {
            return new TypeInfo(collectionType, true, elementType);
        }
    }
}
