package uet.ndh.ddsl.ast.common;

/**
 * Visibility modifiers for fields and methods.
 */
public enum Visibility {
    PUBLIC,
    PROTECTED,
    PRIVATE,
    PACKAGE_PRIVATE;

    public String toJavaKeyword() {
        return switch (this) {
            case PUBLIC -> "public";
            case PROTECTED -> "protected";
            case PRIVATE -> "private";
            case PACKAGE_PRIVATE -> "";
        };
    }
}
