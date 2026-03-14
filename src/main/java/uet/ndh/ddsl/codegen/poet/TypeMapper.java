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
    private static final String SHARED_SUBPACKAGE = "shared";
    private static final String MODEL_SUBPACKAGE = "model";
    private static final String EVENT_SUBPACKAGE = "event";
    private static final String SPECIFICATION_SUBPACKAGE = "specification";
    
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
    
    private final String basePackage;
    private final Map<String, String> domainTypePackages;
    // Maps field names to their declared type names (e.g. "guest" → "GuestProfile")
    private final Map<String, String> fieldTypeMap;
    
    public TypeMapper(String basePackage) {
        this.basePackage = normalizeBasePackage(basePackage);
        this.domainTypePackages = new HashMap<>();
        this.fieldTypeMap = new HashMap<>();
    }

    private String normalizeBasePackage(String pkg) {
        if (pkg == null || pkg.isBlank()) {
            return "com.example.domain";
        }
        String normalized = pkg.trim();
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
    
    /**
     * Register a domain type with its package for resolution.
     */
    public void registerDomainType(String typeName, String packageName) {
        domainTypePackages.put(typeName, packageName);
    }
    
    /**
     * Register a field name → type name mapping from the enclosing entity/aggregate.
     * Used to resolve untyped behavior parameters by matching against field declarations.
     * For example: field "guest: GuestProfile" registers "guest" → "GuestProfile".
     */
    public void registerFieldType(String fieldName, String typeName) {
        fieldTypeMap.put(fieldName, typeName);
    }
    
    /**
     * Clear field type mappings. Call before processing a new entity/aggregate.
     */
    public void clearFieldTypes() {
        fieldTypeMap.clear();
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
        return ClassName.get(basePackage + "." + SHARED_SUBPACKAGE, "AggregateRoot");
    }
    
    /**
     * Get the Entity base interface ClassName.
     */
    public ClassName getEntityInterface() {
        return ClassName.get(basePackage + "." + SHARED_SUBPACKAGE, "Entity");
    }
    
    /**
     * Get the ValueObject base interface ClassName.
     */
    public ClassName getValueObjectInterface() {
        return ClassName.get(basePackage + "." + SHARED_SUBPACKAGE, "ValueObject");
    }
    
    /**
     * Get the DomainEvent base interface ClassName.
     */
    public ClassName getDomainEventInterface() {
        return ClassName.get(basePackage + "." + SHARED_SUBPACKAGE, "DomainEvent");
    }
    
    /**
     * Create a ClassName for a domain type within an aggregate package.
     */
    public ClassName domainType(String aggregateName, String typeName) {
        String packageName = aggregateName == null || aggregateName.isBlank()
            ? basePackage
            : basePackage + "." + aggregateName.toLowerCase();
        return ClassName.get(packageName, typeName);
    }

    /**
     * Resolve a domain type to a concrete class name using registered package metadata.
     */
    public ClassName resolveDomainClassName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return ClassName.get(basePackage, "Object");
        }
        if (domainTypePackages.containsKey(typeName)) {
            return ClassName.get(domainTypePackages.get(typeName), typeName);
        }
        return ClassName.get(basePackage, typeName);
    }

    public String packageForStandaloneModel() {
        return basePackage + "." + MODEL_SUBPACKAGE;
    }

    public String packageForStandaloneEvents() {
        return basePackage + "." + EVENT_SUBPACKAGE;
    }

    public String packageForSpecifications() {
        return basePackage + "." + SPECIFICATION_SUBPACKAGE;
    }
    
    /**
     * Get the base package.
     */
    public String getBasePackage() {
        return basePackage;
    }
    
    /**
     * Try to resolve a parameter name to a domain type.
     * Capitalizes the param name and checks if it matches a registered domain type.
     * For example: "roomType" → "RoomType" → resolves to the RoomType value object.
     * 
     * @param paramName the parameter name (e.g. "roomType", "guest")
     * @return the resolved TypeName if found, null otherwise
     */
    public TypeName tryResolveParamType(String paramName) {
        if (paramName == null || paramName.isEmpty()) return null;
        
        // 1. Check field-to-type mappings first (highest priority)
        // e.g. field "guest: GuestProfile" → param "guest" resolves to GuestProfile
        if (fieldTypeMap.containsKey(paramName)) {
            String fieldTypeName = fieldTypeMap.get(paramName);
            return mapSimpleType(fieldTypeName);
        }
        
        // 2. Capitalize first letter: "roomType" → "RoomType"
        String candidateType = Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
        
        // Check if it's a registered domain type
        if (domainTypePackages.containsKey(candidateType)) {
            return ClassName.get(domainTypePackages.get(candidateType), candidateType);
        }
        
        // Check if it matches a known Java type (String, UUID, etc.)
        if (COMMON_TYPES.containsKey(candidateType)) {
            return COMMON_TYPES.get(candidateType);
        }
        if (TEMPORAL_TYPES.containsKey(candidateType)) {
            return TEMPORAL_TYPES.get(candidateType);
        }
        
        // 3. Case-insensitive match against registered domain types
        String upperParam = candidateType.toUpperCase();
        for (var entry : domainTypePackages.entrySet()) {
            if (entry.getKey().toUpperCase().equals(upperParam)) {
                return ClassName.get(entry.getValue(), entry.getKey());
            }
        }
        
        return null;
    }
}
