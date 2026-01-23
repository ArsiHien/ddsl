package uet.ndh.ddsl.core.building.expression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collection operations for aggregation and filtering.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class CollectionExpression extends Expression {
    private final Expression source;
    private final CollectionOperation operation;
    private final Expression predicate; // For filter operations
    private final Expression mapper; // For map operations
    private final String resultType;

    public CollectionExpression(Expression source, CollectionOperation operation,
                               Expression predicate, Expression mapper, String resultType) {
        super();
        this.source = source;
        this.operation = operation;
        this.predicate = predicate;
        this.mapper = mapper;
        this.resultType = resultType;
    }

    @Override
    public String generateCode() {
        String sourceCode = source.generateCode();

        switch (operation) {
            case FILTER:
                if (predicate == null) {
                    throw new IllegalStateException("Filter operation requires predicate");
                }
                return sourceCode + ".stream().filter(" + generateLambda(predicate) + ").collect(Collectors.toList())";

            case MAP:
                if (mapper == null) {
                    throw new IllegalStateException("Map operation requires mapper");
                }
                return sourceCode + ".stream().map(" + generateLambda(mapper) + ").collect(Collectors.toList())";

            case SUM:
                return sourceCode + ".stream().mapToDouble(item -> item.doubleValue()).sum()";

            case AVG:
                return sourceCode + ".stream().mapToDouble(item -> item.doubleValue()).average().orElse(0.0)";

            case COUNT:
                return sourceCode + ".size()";

            case MIN:
                return sourceCode + ".stream().min(Comparator.naturalOrder()).orElse(null)";

            case MAX:
                return sourceCode + ".stream().max(Comparator.naturalOrder()).orElse(null)";

            case ANY:
                if (predicate == null) {
                    return sourceCode + ".size() > 0";
                } else {
                    return sourceCode + ".stream().anyMatch(" + generateLambda(predicate) + ")";
                }

            case ALL:
                if (predicate == null) {
                    throw new IllegalStateException("All operation requires predicate");
                }
                return sourceCode + ".stream().allMatch(" + generateLambda(predicate) + ")";

            case FIRST:
                return sourceCode + ".stream().findFirst().orElse(null)";

            case LAST:
                return sourceCode + ".get(" + sourceCode + ".size() - 1)";

            default:
                throw new IllegalArgumentException("Unsupported collection operation: " + operation);
        }
    }

    private String generateLambda(Expression expression) {
        // Simple lambda generation - assumes expression uses 'item' as the parameter
        return "item -> " + expression.generateCode();
    }

    @Override
    public String getType() {
        return resultType;
    }

    @Override
    public Expression copy() {
        Expression copiedPredicate = (predicate != null) ? predicate.copy() : null;
        Expression copiedMapper = (mapper != null) ? mapper.copy() : null;
        return new CollectionExpression(this.source.copy(), this.operation,
                                      copiedPredicate, copiedMapper, this.resultType);
    }
}
