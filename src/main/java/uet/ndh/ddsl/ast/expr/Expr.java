package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.expr.collection.CollectionAggregation;
import uet.ndh.ddsl.ast.expr.collection.CollectionFilter;
import uet.ndh.ddsl.ast.expr.collection.CollectionFlatten;
import uet.ndh.ddsl.ast.expr.collection.CollectionGroupBy;
import uet.ndh.ddsl.ast.expr.specification.SpecificationCondition;
import uet.ndh.ddsl.ast.expr.string.StringCondition;
import uet.ndh.ddsl.ast.expr.string.StringOperation;
import uet.ndh.ddsl.ast.expr.temporal.TemporalExpr;

/**
 * Marker interface for all expression types.
 * Expressions produce values.
 */
public sealed interface Expr extends AstNode
    permits LiteralExpr, VariableExpr, BinaryExpr, UnaryExpr,
            MethodCallExpr, FieldAccessExpr, NewInstanceExpr,
            ListExpr, MapExpr, TernaryExpr, ThisExpr, NullExpr,
            MatchExpr,
            // Extended expressions
            TemporalExpr,
            StringCondition, StringOperation,
            CollectionAggregation, CollectionFilter, CollectionFlatten, CollectionGroupBy,
            SpecificationCondition {
}
