package uet.ndh.ddsl.ast.stmt;

import uet.ndh.ddsl.ast.AstNode;

/**
 * Marker interface for all statement types.
 * Statements perform actions but don't produce values.
 */
public sealed interface Stmt extends AstNode
    permits BlockStmt, ExpressionStmt, IfStmt, ForEachStmt, 
            ReturnStmt, AssignmentStmt, VariableDeclarationStmt {
}
