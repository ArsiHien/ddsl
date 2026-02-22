package uet.ndh.ddsl.ast.expr.temporal;

import uet.ndh.ddsl.ast.SourceSpan;

/**
 * Represents a duration of time.
 * 
 * Syntax:
 * <pre>
 *     IntLiteral DurationUnit
 *     IntLiteral DurationUnit 'and' IntLiteral DurationUnit
 * </pre>
 * 
 * Examples:
 * <pre>
 *     24 hours
 *     30 days
 *     1 year
 *     2 weeks and 3 days
 * </pre>
 */
public record Duration(
    SourceSpan span,
    int amount,
    DurationUnit unit,
    Duration additional  // For compound durations like "2 weeks and 3 days"
) {
    
    /**
     * Factory for simple duration.
     */
    public static Duration of(SourceSpan span, int amount, DurationUnit unit) {
        return new Duration(span, amount, unit, null);
    }
    
    /**
     * Factory for compound duration.
     */
    public static Duration compound(SourceSpan span, int amount1, DurationUnit unit1, 
                                    int amount2, DurationUnit unit2) {
        Duration additional = new Duration(span, amount2, unit2, null);
        return new Duration(span, amount1, unit1, additional);
    }
    
    /**
     * Check if this is a compound duration.
     */
    public boolean isCompound() {
        return additional != null;
    }
    
    /**
     * Convert to total milliseconds (approximate for months/years).
     */
    public long toMillis() {
        long millis = switch (unit) {
            case SECOND, SECONDS -> amount * 1000L;
            case MINUTE, MINUTES -> amount * 60 * 1000L;
            case HOUR, HOURS -> amount * 60 * 60 * 1000L;
            case DAY, DAYS -> amount * 24 * 60 * 60 * 1000L;
            case WEEK, WEEKS -> amount * 7 * 24 * 60 * 60 * 1000L;
            case MONTH, MONTHS -> amount * 30L * 24 * 60 * 60 * 1000L;
            case YEAR, YEARS -> amount * 365L * 24 * 60 * 60 * 1000L;
        };
        return additional != null ? millis + additional.toMillis() : millis;
    }
    
    /**
     * Duration units.
     */
    public enum DurationUnit {
        SECOND, SECONDS,
        MINUTE, MINUTES,
        HOUR, HOURS,
        DAY, DAYS,
        WEEK, WEEKS,
        MONTH, MONTHS,
        YEAR, YEARS;
        
        /**
         * Get the Java ChronoUnit equivalent name.
         */
        public String toChronoUnit() {
            return switch (this) {
                case SECOND, SECONDS -> "SECONDS";
                case MINUTE, MINUTES -> "MINUTES";
                case HOUR, HOURS -> "HOURS";
                case DAY, DAYS -> "DAYS";
                case WEEK, WEEKS -> "WEEKS";
                case MONTH, MONTHS -> "MONTHS";
                case YEAR, YEARS -> "YEARS";
            };
        }
        
        /**
         * Parse from string.
         */
        public static DurationUnit fromString(String s) {
            return switch (s.toLowerCase()) {
                case "second", "seconds" -> SECONDS;
                case "minute", "minutes" -> MINUTES;
                case "hour", "hours" -> HOURS;
                case "day", "days" -> DAYS;
                case "week", "weeks" -> WEEKS;
                case "month", "months" -> MONTHS;
                case "year", "years" -> YEARS;
                default -> throw new IllegalArgumentException("Unknown duration unit: " + s);
            };
        }
    }
}
