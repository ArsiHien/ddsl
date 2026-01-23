package uet.ndh.ddsl.core.model;

import uet.ndh.ddsl.core.codegen.JavaClass;

/**
 * Represents a custom domain exception.
 */
public class DomainException {
    private final String exceptionName;
    private final String superClass;
    private final String message;

    public DomainException(String exceptionName, String superClass, String message) {
        this.exceptionName = exceptionName;
        this.superClass = superClass != null ? superClass : "RuntimeException";
        this.message = message;
    }

    public DomainException(String exceptionName) {
        this(exceptionName, "RuntimeException", null);
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public String getSuperClass() {
        return superClass;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Generate exception class.
     * @return JavaClass for this exception
     */
    public JavaClass generateExceptionClass() {
        // TODO: Implement exception class generation
        JavaClass exceptionClass = new JavaClass("", exceptionName);
        return exceptionClass;
    }
}
