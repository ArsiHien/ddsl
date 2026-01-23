package uet.ndh.ddsl.core.building.expression;

/**
 * Enumeration of binary operators.
 */
public enum Operator {
    PLUS("+"), MINUS("-"), MULTIPLY("*"), DIVIDE("/"), MODULO("%"),
    EQUALS("=="), NOT_EQUALS("!="),
    GREATER_THAN(">"), LESS_THAN("<"), GREATER_EQUAL(">="), LESS_EQUAL("<="),
    AND("&&"), OR("||"), NOT("!");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
