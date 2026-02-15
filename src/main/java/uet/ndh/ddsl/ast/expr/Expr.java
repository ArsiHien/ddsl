package uet.ndh.ddsl.ast.expr;

import uet.ndh.ddsl.ast.AstNode;

/**
 * Marker interface for all expression types.
 * Expressions produce values.
 */
public sealed interface Expr extends AstNode
    permits LiteralExpr, VariableExpr, BinaryExpr, UnaryExpr,
            MethodCallExpr, FieldAccessExpr, NewInstanceExpr,
            ListExpr, MapExpr, TernaryExpr, ThisExpr, NullExpr {
}
