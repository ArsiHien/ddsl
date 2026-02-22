package uet.ndh.ddsl.codegen.poet;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import uet.ndh.ddsl.ast.common.TypeRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Maps DDSL type references to JavaPoet TypeNames.
 * 
 * This is the single source of truth for type resolution in the code generation pipeline.
 * It handles:
 * - Primitive types (int, long, boolean, etc.)
 * - Common Java types (String, UUID, BigDecimal, etc.)
 * - Temporal types (Instant, LocalDate, LocalDateTime)
 * - Collection types (List, Set, Map)
 * - Domain-specific types (resolved via package context)
 */
public class TypeMapper {
    
    // Primitive type mappings
    private static final Map<String, TypeName> PRIMITIVE_TYPES = Map.ofEntries(
        Map.entry("int", TypeName.INT),
        Map.entry("Integer", TypeName.INT.box()),
        Map.entry("long", TypeName.LONG),
        Map.entry("Long", TypeName.LONG.box()),
        Map.entry("double", TypeName.DOUBLE),
        Map.entry("Double", TypeName.DOUBLE.box()),
        Map.entry("float", TypeName.FLOAT),
        Map.entry("Float", TypeName.FLOAT.box()),
        Map.entry("boolean", TypeName.BOOLEAN),
        Map.entry("Boolean", TypeName.BOOLEAN.box()),
        Map.entry("byte", TypeName.BYTE),
        Map.entry("Byte", TypeName.BYTE.box()),
        Map.entry("short", TypeName.SHORT),
        Map.entry("Short", TypeName.SHORT.box()),
        Map.entry("char", TypeName.CHAR),
        Map.entry("Character", TypeName.CHAR.box()),
        Map.entry("void", TypeName.VOID)
    );
    
    // Common Java type mappings
    private static final Map<String, ClassName> COMMON_TYPES = Map.ofEntries(
        Map.entry("String", ClassName.get(String.class)),
        Map.entry("UUID", ClassName.get(UUID.class)),
        Map.entry("BigDecimal", ClassName.get(BigDecimal.class)),
        Map.entry("Object", ClassName.get(Object.class)),
        Map.entry("Optional", ClassName.get(Optional.class))
    );
    
    // Temporal type mappings
    private static final Map<String, ClassName> TEMPORAL_TYPES = Map.ofEntries(
        Map.entry("Instant", ClassName.get(Instant.class)),
        Map.entry("LocalDate", ClassName.get(LocalDate.class)),
        Map.entry("LocalDateTime", ClassName.get(LocalDateTime.class)),
        Map.entry("Date", ClassName.get("java.util", "Date")),
        Map.entry("DateTime", ClassName.get(LocalDateTime.class)),
        Map.entry("Timestamp", ClassName.get(Instant.class))
    );
    
    // Collection type mappings
    private static final Map<String, ClassName> COLLECTION_TYPES = Map.of(
        "List", ClassName.get(List.class),
        "Set", ClassName.get(Set.class),
        "Map", ClassName.get(Map.class)
    );
    
    // DDD base types - these reference scaffold-generated base classes
    private static final ClassName AGGREGATE_ROOT = ClassName.get("", "AggregateRoot");
    private static final ClassName ENTITY = ClassName.get("", "Entity");
    private static final ClassName VALUE_OBJECT = ClassName.get("", "ValueObject");
    private static final ClassName DOMAIN_EVENT = ClassName.get("", "DomainEvent");
    
    private final String basePackage;
    private final Map<String, String> domainTypePackages;
    
    public TypeMapper(String basePackage) {
        this.basePackage = basePackage;
        this.domainTypePackages = new HashMap<>();
    }
    
    /**
     * Register a domain type with its package for resolution.
     */
    public void registerDomainType(String typeName, String packageName) {
        domainTypePackages.put(typeName, packageName);
    }
    
    /**
     * Map a DDSL TypeRef to a JavaPoet TypeName.
     */
    public TypeName mapType(TypeRef typeRef) {
        if (typeRef == null) {
            return TypeName.VOID;
        }
        
        // Handle collection types
        if (typeRef.isCollection() && typeRef.collectionKind() != null) {
            return mapCollectionType(typeRef);
        }
        
        // Handle nullable types with Optional
        if (typeRef.isNullable()) {
            TypeName innerType = mapSimpleType(typeRef.name());
            return ParameterizedTypeName.get(ClassName.get(Optional.class), innerType);
        }
        
        return mapSimpleType(typeRef.name());
    }
    
    /**
     * Map a simple type name to TypeName.
     */
    public TypeName mapSimpleType(String typeName) {
        // Check primitives first
        if (PRIMITIVE_TYPES.containsKey(typeName)) {
            return PRIMITIVE_TYPES.get(typeName);
        }
        
        // Check common types
        if (COMMON_TYPES.containsKey(typeName)) {
            return COMMON_TYPES.get(typeName);
        }
        
        // Check temporal types
        if (TEMPORAL_TYPES.containsKey(typeName)) {
            return TEMPORAL_TYPES.get(typeName);
        }
        
        // Check collection types (raw)
        if (COLLECTION_TYPES.containsKey(typeName)) {
            return COLLECTION_TYPES.get(typeName);
        }
        
        // Check registered domain types
        if (domainTypePackages.containsKey(typeName)) {
            return ClassName.get(domainTypePackages.get(typeName), typeName);
        }
        
        // Default: assume it's a domain type in the base package
        return ClassName.get(basePackage, typeName);
    }
    
    /**
     * Map a collection type with generic parameters.
     */
    private TypeName mapCollectionType(TypeRef typeRef) {
        ClassName collectionClass = switch (typeRef.collectionKind()) {
            case LIST -> ClassName.get(List.class);
            case SET -> ClassName.get(Set.class);
            case MAP -> ClassName.get(Map.class);
            case ARRAY -> throw new UnsupportedOperationException("Array types not yet supported");
        };
        
        if (typeRef.collectionKind() == TypeRef.CollectionKind.MAP) {
            // Map requires two type arguments
            if (typeRef.typeArguments().size() >= 2) {
                TypeName keyType = mapType(typeRef.typeArguments().get(0));
                TypeName valueType = mapType(typeRef.typeArguments().get(1));
                return ParameterizedTypeName.get(collectionClass, keyType, valueType);
            }
            // Default to String, Object
            return ParameterizedTypeName.get(collectionClass, 
                ClassName.get(String.class), ClassName.get(Object.class));
        } else {
            // List and Set require one type argument
            if (!typeRef.typeArguments().isEmpty()) {
                TypeName elementType = mapType(typeRef.typeArguments().get(0));
                return ParameterizedTypeName.get(collectionClass, elementType);
            }
            // Default to Object
            return ParameterizedTypeName.get(collectionClass, ClassName.get(Object.class));
        }
    }
    
    /**
     * Get the AggregateRoot base interface ClassName.
     */
    public ClassName getAggregateRootInterface() {
        return ClassName.get(basePackage + ".shared", "AggregateRoot");
    }
    
    /**
     * Get the Entity base interface ClassName.
     */
    public ClassName getEntityInterface() {
        return ClassName.get(basePackage + ".shared", "Entity");
    }
    
    /**
     * Get the ValueObject base interface ClassName.
     */
    public ClassName getValueObjectInterface() {
        return ClassName.get(basePackage + ".shared", "ValueObject");
    }
    
    /**
     * Get the DomainEvent base interface ClassName.
     */
    public ClassName getDomainEventInterface() {
        return ClassName.get(basePackage + ".shared", "DomainEvent");
    }
    
    /**
     * Create a ClassName for a domain type within an aggregate package.
     */
    public ClassName domainType(String aggregateName, String typeName) {
        String packageName = basePackage + "." + aggregateName.toLowerCase();
        return ClassName.get(packageName, typeName);
    }
    
    /**
     * Get the base package.
     */
    public String getBasePackage() {
        return basePackage;
    }
}
