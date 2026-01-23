package uet.ndh.ddsl.core.model.repository;

/**
 * Enumeration of repository method types.
 */
public enum RepositoryMethodType {
    FIND_BY_ID("findById", true),
    SAVE("save", false),
    DELETE("delete", false),
    FIND_ALL("findAll", true),
    FIND_BY_CRITERIA("findBy", true),
    EXISTS("exists", true),
    COUNT("count", true);

    private final String defaultName;
    private final boolean returnsValue;

    RepositoryMethodType(String defaultName, boolean returnsValue) {
        this.defaultName = defaultName;
        this.returnsValue = returnsValue;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public boolean returnsValue() {
        return returnsValue;
    }
}
