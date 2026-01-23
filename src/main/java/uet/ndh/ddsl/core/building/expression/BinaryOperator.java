package uet.ndh.ddsl.core.building.expression;

/**
 * Binary operators for expressions.
 */
public enum BinaryOperator {
    // Arithmetic
    ADD("+", "ADD"),
    SUB("-", "SUB"),
    MUL("*", "MUL"),
    DIV("/", "DIV"),
    MOD("%", "MOD"),

    // Comparison
    EQ("==", "EQ"),
    NEQ("!=", "NEQ"),
    GT(">", "GT"),
    GTE(">=", "GTE"),
    LT("<", "LT"),
    LTE("<=", "LTE"),

    // Logical
    AND("&&", "AND"),
    OR("||", "OR"),

    // Collection
    IN(".contains", "IN"); // Special case - will be handled differently

    private final String symbol;
    private final String name;

    BinaryOperator(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public boolean isArithmetic() {
        return this == ADD || this == SUB || this == MUL || this == DIV || this == MOD;
    }

    public boolean isComparison() {
        return this == EQ || this == NEQ || this == GT || this == GTE || this == LT || this == LTE;
    }

    public boolean isLogical() {
        return this == AND || this == OR;
    }

    public boolean isCollection() {
        return this == IN;
    }
}

