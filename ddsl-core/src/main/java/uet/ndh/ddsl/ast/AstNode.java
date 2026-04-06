package uet.ndh.ddsl.ast;

import uet.ndh.ddsl.ast.visitor.AstVisitor;

/**
 * Base interface for all AST nodes in the DDSL language.
 * 
 * Following the Crafting Interpreters approach, AST nodes are pure data objects
 * with no logic. All processing is done via the Visitor pattern.
 * 
 * @param <R> The return type of the visitor
 */
public interface AstNode {
    
    /**
     * Source location information for error reporting.
     */
    SourceSpan span();
    
    /**
     * Accept a visitor to process this node.
     * This is the only method with "logic" - it dispatches to the appropriate visit method.
     */
    <R> R accept(AstVisitor<R> visitor);
}
