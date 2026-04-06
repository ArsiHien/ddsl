package uet.ndh.ddsl.ast.behavior;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.behavior.clause.EmitClause;
import uet.ndh.ddsl.ast.behavior.clause.ErrorAccumulationClause;
import uet.ndh.ddsl.ast.behavior.clause.GivenClause;
import uet.ndh.ddsl.ast.behavior.clause.RequireClause;
import uet.ndh.ddsl.ast.behavior.clause.ReturnClause;
import uet.ndh.ddsl.ast.behavior.clause.ThenClause;
import uet.ndh.ddsl.ast.member.ParameterDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a Behavior in Domain-Driven Design.
 * Behaviors denote domain operations that alter aggregate state while upholding 
 * invariants and generating domain events.
 * 
 * The behavior follows a structured natural language paradigm with the pattern:
 * <pre>
 * when [natural language phrase] with [parameters]:
 *     require that: [conditions]
 *     given: [transformations]
 *     then: [state changes]
 *     and emit [event]
 *     return [value]  // For domain services
 * </pre>
 * 
 * Pure data record - no logic except accept().
 * 
 * @see RequireClause
 * @see GivenClause
 * @see ThenClause
 * @see EmitClause
 * @see ReturnClause
 */
public record BehaviorDecl(
    SourceSpan span,
    NaturalLanguagePhrase phrase,
    List<ParameterDecl> parameters,
    RequireClause requireClause,
    ErrorAccumulationClause errorAccumulationClause,
    GivenClause givenClause,
    List<ThenClause> thenClauses,
    EmitClause emitClause,
    ReturnClause returnClause,
    String documentation
) implements AstNode {
    
    public BehaviorDecl {
        parameters = parameters != null ? List.copyOf(parameters) : List.of();
        thenClauses = thenClauses != null ? List.copyOf(thenClauses) : List.of();
    }
    
    /**
     * Legacy constructor without returnClause for backward compatibility.
     */
    public BehaviorDecl(
        SourceSpan span,
        NaturalLanguagePhrase phrase,
        List<ParameterDecl> parameters,
        RequireClause requireClause,
        ErrorAccumulationClause errorAccumulationClause,
        GivenClause givenClause,
        List<ThenClause> thenClauses,
        EmitClause emitClause,
        String documentation
    ) {
        this(span, phrase, parameters, requireClause, errorAccumulationClause, givenClause,
            thenClauses, emitClause, null, documentation);
    }

    /**
     * Legacy constructor kept for compatibility with older parser callsites.
     */
    public BehaviorDecl(
        SourceSpan span,
        NaturalLanguagePhrase phrase,
        List<ParameterDecl> parameters,
        RequireClause requireClause,
        GivenClause givenClause,
        List<ThenClause> thenClauses,
        EmitClause emitClause,
        String documentation
    ) {
        this(span, phrase, parameters, requireClause, null, givenClause, thenClauses, emitClause, null, documentation);
    }
    
    /**
     * Get the behavior name derived from the natural language phrase.
     */
    public String getName() {
        return phrase != null ? phrase.toMethodName() : "unnamed";
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitBehavior(this);
    }
}
