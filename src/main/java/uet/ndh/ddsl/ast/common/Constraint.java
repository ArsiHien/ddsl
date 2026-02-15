package uet.ndh.ddsl.ast.common;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.expr.Expr;

/**
 * Represents a constraint on a field.
 * Pure data record.
 */
public record Constraint(
    SourceSpan span,
    ConstraintType type,
    Expr value,
    String message
) {
    
    /**
     * Type of constraint.
     */
    public enum ConstraintType {
        // Identity and presence
        IDENTITY,       // @identity - marks as primary identifier
        REQUIRED,       // @required - non-null
        NOT_NULL,       // @NotNull
        NOT_EMPTY,      // @NotEmpty
        NOT_BLANK,      // @NotBlank
        UNIQUE,         // @unique - unique across instances
        
        // Numerical constraints
        MIN,            // @min(n)
        MAX,            // @max(n)
        MIN_LENGTH,     // @minLength(n)
        MAX_LENGTH,     // @maxLength(n)
        SIZE,           // @size(min, max)
        PRECISION,      // @precision(p, s)
        
        // String/format constraints
        PATTERN,        // @pattern(regex)
        EMAIL,          // @email
        URL,            // @url
        
        // Temporal constraints
        PAST,           // @past
        FUTURE,         // @future
        
        // Numeric sign constraints
        POSITIVE,       // @positive
        NEGATIVE,       // @negative
        
        // Behavioral constraints
        DEFAULT,        // @default(value)
        COMPUTED,       // @computed - derived value
        IMMUTABLE,      // @immutable - cannot change after creation
        
        // Custom
        CUSTOM          // User-defined constraint
    }
}
