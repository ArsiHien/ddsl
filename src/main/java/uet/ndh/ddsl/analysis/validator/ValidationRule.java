package uet.ndh.ddsl.analysis.validator;

import uet.ndh.ddsl.ast.AstNode;

import java.util.List;

/**
 * Interface for validation rules.
 * Each rule checks one specific aspect of DDD compliance.
 */
public interface ValidationRule<T extends AstNode> {
    
    /**
     * Get the unique ID for this rule.
     */
    String ruleId();
    
    /**
     * Get a description of what this rule checks.
     */
    String description();
    
    /**
     * The type of AST node this rule applies to.
     */
    Class<T> applicableTo();
    
    /**
     * Validate the given node.
     * Returns a list of diagnostics (may be empty if valid).
     */
    List<Diagnostic> validate(T node);
}
