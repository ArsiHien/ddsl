package uet.ndh.ddsl.ast.model.event;

import uet.ndh.ddsl.ast.AstNode;
import uet.ndh.ddsl.ast.SourceSpan;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.visitor.AstVisitor;

import java.util.List;

/**
 * Represents a DDD Domain Event - something that happened in the domain
 * that domain experts care about.
 * 
 * Pure data record - no logic except accept().
 */
public record DomainEventDecl(
    SourceSpan span,
    String name,
    List<FieldDecl> fields,
    TypeRef sourceAggregate,
    String documentation
) implements AstNode {
    
    public DomainEventDecl {
        fields = fields != null ? List.copyOf(fields) : List.of();
    }
    
    @Override
    public <R> R accept(AstVisitor<R> visitor) {
        return visitor.visitDomainEvent(this);
    }
}
