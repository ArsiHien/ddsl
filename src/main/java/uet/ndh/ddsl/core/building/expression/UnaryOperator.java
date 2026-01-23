package uet.ndh.ddsl.core.building.expression;

/**
 * Unary operators for expressions.
 */
public enum UnaryOperator {
    NOT("!", "NOT", true),
    NEGATE("-", "NEGATE", true),
    INCREMENT("++", "INCREMENT", false),
    DECREMENT("--", "DECREMENT", false);

    private final String symbol;
    private final String name;
    private final boolean isPrefix;

    UnaryOperator(String symbol, String name, boolean isPrefix) {
        this.symbol = symbol;
        this.name = name;
        this.isPrefix = isPrefix;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public boolean isPrefix() {
        return isPrefix;
    }
}
