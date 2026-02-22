package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.NaturalLanguageCondition;
import uet.ndh.ddsl.ast.stmt.Stmt;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a match expression (pattern matching / switch expression).
 * 
 * Can be used as both an expression (returns value) or statement (executes actions).
 * 
 * Syntax:
 * <pre>
 *     match expression with:
 *         CasePattern: CaseBody
 *         [Value1, Value2]: CaseBody
 *         Value when Condition: CaseBody
 *         null: CaseBody
 *         default: CaseBody
 * </pre>
 * 
 * Examples:
 * <pre>
 *     // As expression
 *     discount rate as match customer tier with:
 *         Gold:    0.15
 *         Silver:  0.10
 *         default: 0.00
 *     
 *     // As statement
 *     match order status with:
 *         Pending:
 *             - apply processing fee
 *         Shipped:
 *             - notify customer
 *         default:
 *             - log unknown status
 * </pre>
 */
public record MatchExpr(
    SourceSpan span,
    Expr matchTarget,
    List<MatchCase> cases,
    MatchCase defaultCase
) implements Expr {
    
    public MatchExpr {
        cases = cases != null ? List.copyOf(cases) : List.of();
    }
    
    /**
     * Check if this match has a default case.
     */
    public boolean hasDefault() {
        return defaultCase != null;
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        // TODO: Add visitMatchExpr to AstVisitor
        return null;
    }
    
    /**
     * A single case in a match expression.
     */
    public record MatchCase(
        SourceSpan span,
        CasePattern pattern,
        NaturalLanguageCondition guard,     // Optional "when" guard
        MatchCaseBody body
    ) {
        
        /**
         * Factory for simple case without guard.
         */
        public static MatchCase simple(SourceSpan span, CasePattern pattern, MatchCaseBody body) {
            return new MatchCase(span, pattern, null, body);
        }
        
        /**
         * Factory for guarded case.
         */
        public static MatchCase guarded(SourceSpan span, CasePattern pattern, 
                                         NaturalLanguageCondition guard, MatchCaseBody body) {
            return new MatchCase(span, pattern, guard, body);
        }
        
        /**
         * Check if this case has a guard.
         */
        public boolean hasGuard() {
            return guard != null;
        }
    }
    
    /**
     * Pattern for a match case.
     */
    public sealed interface CasePattern 
        permits CasePattern.SingleValue, CasePattern.MultipleValues, 
                CasePattern.NullValue, CasePattern.DefaultPattern {
        
        /**
         * Single value pattern (enum value, literal, identifier).
         */
        record SingleValue(String value, Expr expression) implements CasePattern {
            public static SingleValue of(String value) {
                return new SingleValue(value, null);
            }
            
            public static SingleValue of(Expr expr) {
                return new SingleValue(null, expr);
            }
        }
        
        /**
         * Multiple values pattern: [Value1, Value2, Value3]
         */
        record MultipleValues(List<String> values) implements CasePattern {
            public MultipleValues {
                values = values != null ? List.copyOf(values) : List.of();
            }
        }
        
        /**
         * Null value pattern.
         */
        record NullValue() implements CasePattern {}
        
        /**
         * Default pattern.
         */
        record DefaultPattern() implements CasePattern {}
    }
    
    /**
     * Body of a match case.
     */
    public sealed interface MatchCaseBody 
        permits MatchCaseBody.ExpressionBody, MatchCaseBody.StatementBody, 
                MatchCaseBody.BlockBody {
        
        /**
         * Single expression result.
         */
        record ExpressionBody(Expr expression) implements MatchCaseBody {}
        
        /**
         * Single statement.
         */
        record StatementBody(Stmt statement) implements MatchCaseBody {}
        
        /**
         * Block of statements.
         */
        record BlockBody(List<Stmt> statements) implements MatchCaseBody {
            public BlockBody {
                statements = statements != null ? List.copyOf(statements) : List.of();
            }
        }
    }
}
