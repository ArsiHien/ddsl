package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.JavaType;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.type.*;
import uet.ndh.ddsl.parser.ParseException;

/**
 * Converts string type references to JavaType objects.
 */
public class JavaTypeConverter {

    public JavaType convertType(String typeString, SourceLocation location) throws ParseException {
        if (typeString == null || typeString.trim().isEmpty()) {
            throw new ParseException("Type cannot be empty", location);
        }

        typeString = typeString.trim();

        // Handle primitive types
        PrimitiveKind primitiveKind = parsePrimitiveType(typeString);
        if (primitiveKind != null) {
            return new PrimitiveType(primitiveKind);
        }

        // Handle collection types
        if (typeString.startsWith("List<") && typeString.endsWith(">")) {
            String elementTypeString = typeString.substring(5, typeString.length() - 1);
            JavaType elementType = convertType(elementTypeString, location);
            return new CollectionType(elementType, CollectionKind.LIST);
        }

        if (typeString.startsWith("Set<") && typeString.endsWith(">")) {
            String elementTypeString = typeString.substring(4, typeString.length() - 1);
            JavaType elementType = convertType(elementTypeString, location);
            return new CollectionType(elementType, CollectionKind.SET);
        }

        if (typeString.startsWith("Map<") && typeString.endsWith(">")) {
            // For simplicity, we'll treat Map as a generic collection
            return new CollectionType(new PrimitiveType(PrimitiveKind.STRING), CollectionKind.MAP);
        }

        // Handle custom types
        return new CustomType(typeString, "", false, false, false);
    }

    private PrimitiveKind parsePrimitiveType(String typeString) {
        return switch (typeString.toLowerCase()) {
            case "int", "integer" -> PrimitiveKind.INT;
            case "long" -> PrimitiveKind.LONG;
            case "double" -> PrimitiveKind.DOUBLE;
            case "boolean" -> PrimitiveKind.BOOLEAN;
            case "string" -> PrimitiveKind.STRING;
            case "bigdecimal" -> PrimitiveKind.BIG_DECIMAL;
            case "uuid" -> PrimitiveKind.UUID;
            case "instant" -> PrimitiveKind.INSTANT;
            case "localdate" -> PrimitiveKind.LOCAL_DATE;
            case "localdatetime" -> PrimitiveKind.LOCAL_DATE_TIME;
            default -> null;
        };
    }
}
