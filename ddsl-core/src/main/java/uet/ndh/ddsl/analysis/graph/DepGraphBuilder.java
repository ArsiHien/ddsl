package uet.ndh.ddsl.analysis.graph;

import uet.ndh.ddsl.analysis.scope.Symbol;
import uet.ndh.ddsl.analysis.scope.SymbolTable;
import uet.ndh.ddsl.ast.common.TypeRef;
import uet.ndh.ddsl.ast.member.FieldDecl;
import uet.ndh.ddsl.ast.model.BoundedContextDecl;
import uet.ndh.ddsl.ast.model.ModuleDecl;
import uet.ndh.ddsl.ast.model.aggregate.AggregateDecl;
import uet.ndh.ddsl.ast.model.entity.EntityDecl;
import uet.ndh.ddsl.ast.model.event.DomainEventDecl;
import uet.ndh.ddsl.ast.model.event.EventHandlerDecl;
import uet.ndh.ddsl.ast.model.event.EventHandlerContainerDecl;
import uet.ndh.ddsl.ast.model.factory.FactoryDecl;
import uet.ndh.ddsl.ast.model.repository.RepositoryDecl;
import uet.ndh.ddsl.ast.model.service.DomainServiceDecl;
import uet.ndh.ddsl.ast.model.specification.SpecificationDecl;
import uet.ndh.ddsl.ast.model.valueobject.ValueObjectDecl;
import uet.ndh.ddsl.ast.visitor.TreeWalkingVisitor;

import java.util.*;

/**
 * AST visitor that builds a dependency graph by analyzing domain component relationships.
 * Traverses the AST and creates nodes/edges representing dependencies between components.
 */
public class DepGraphBuilder extends TreeWalkingVisitor<Void> {
    
    private final DepGraph graph;
    private final SymbolTable symbolTable;
    private final Deque<String> contextStack;
    private final Map<String, DepNode> nodeRegistry;
    
    public DepGraphBuilder(SymbolTable symbolTable) {
        this.graph = new DepGraph();
        this.symbolTable = symbolTable;
        this.contextStack = new ArrayDeque<>();
        this.nodeRegistry = new HashMap<>();
    }
    
    /**
     * Returns the built dependency graph.
     */
    public DepGraph build() {
        return graph;
    }
    
    @Override
    public Void visitModule(ModuleDecl decl) {
        contextStack.push(decl.name());
        try {
            return super.visitModule(decl);
        } finally {
            contextStack.pop();
        }
    }
    
    @Override
    public Void visitBoundedContext(BoundedContextDecl decl) {
        String qualifiedId = qualifiedId(decl.name());
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.BOUNDED_CONTEXT)
            .boundedContext(decl.name())
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        contextStack.push(decl.name());
        
        try {
            return super.visitBoundedContext(decl);
        } finally {
            contextStack.pop();
        }
    }
    
    @Override
    public Void visitAggregate(AggregateDecl decl) {
        String contextName = currentContext();
        String qualifiedId = qualifiedId(contextName, decl.name());
        
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.AGGREGATE)
            .boundedContext(contextName)
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        
        // Add composition edge to root entity
        if (decl.root() != null) {
            addTypeDependency(node, decl.root().name(), EdgeType.COMPOSITION, decl.root().span());
        }
        
        // Add composition edges to child entities
        for (EntityDecl entity : decl.entities()) {
            addTypeDependency(node, entity.name(), EdgeType.COMPOSITION, entity.span());
        }
        
        // Add composition edges to value objects
        for (ValueObjectDecl vo : decl.valueObjects()) {
            addTypeDependency(node, vo.name(), EdgeType.COMPOSITION, vo.span());
        }
        
        // Analyze field types for dependencies
        if (decl.root() != null) {
            for (FieldDecl field : decl.root().fields()) {
                analyzeFieldDependency(node, field);
            }
        }
        
        return super.visitAggregate(decl);
    }
    
    @Override
    public Void visitEntity(EntityDecl decl) {
        String contextName = currentContext();
        String qualifiedId = qualifiedId(contextName, decl.name());
        
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.ENTITY)
            .boundedContext(contextName)
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        
        // Analyze field types
        for (FieldDecl field : decl.fields()) {
            analyzeFieldDependency(node, field);
        }
        
        return super.visitEntity(decl);
    }
    
    @Override
    public Void visitValueObject(ValueObjectDecl decl) {
        String contextName = currentContext();
        String qualifiedId = qualifiedId(contextName, decl.name());
        
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.VALUE_OBJECT)
            .boundedContext(contextName)
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        
        // Analyze field types
        for (FieldDecl field : decl.fields()) {
            analyzeFieldDependency(node, field);
        }
        
        return super.visitValueObject(decl);
    }
    
    @Override
    public Void visitDomainService(DomainServiceDecl decl) {
        String contextName = currentContext();
        String qualifiedId = qualifiedId(contextName, decl.name());
        
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.DOMAIN_SERVICE)
            .boundedContext(contextName)
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        
        // Analyze dependency field types
        for (FieldDecl dep : decl.dependencies()) {
            analyzeFieldDependency(node, dep);
        }
        
        return super.visitDomainService(decl);
    }
    
    @Override
    public Void visitDomainEvent(DomainEventDecl decl) {
        String contextName = currentContext();
        String qualifiedId = qualifiedId(contextName, decl.name());
        
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.DOMAIN_EVENT)
            .boundedContext(contextName)
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        
        // If event has a source aggregate, add event publish relationship
        if (decl.sourceAggregate() != null) {
            String aggregateName = decl.sourceAggregate().name();
            Optional<DepNode> aggregateNode = findNode(contextName, aggregateName);
            
            if (aggregateNode.isPresent()) {
                DepEdge edge = DepEdge.builder()
                    .from(aggregateNode.get())
                    .to(node)
                    .edgeType(EdgeType.EVENT_PUBLISH)
                    .referenceSpan(decl.span())
                    .build();
                graph.addEdge(edge);
            }
        }
        
        return super.visitDomainEvent(decl);
    }
    
    @Override
    public Void visitEventHandlerContainer(EventHandlerContainerDecl decl) {
        for (EventHandlerDecl handler : decl.handlers()) {
            handler.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visitEventHandler(EventHandlerDecl decl) {
        String contextName = currentContext();
        String eventName = decl.targetEventName();
        
        // Find the event node
        Optional<DepNode> eventNode = findNode(contextName, eventName);
        if (eventNode.isPresent()) {
            // Create handler node
            String handlerName = handlerName(decl);
            String qualifiedId = qualifiedId(contextName, handlerName);
            
            DepNode handlerNode = DepNode.builder()
                .qualifiedId(qualifiedId)
                .name(handlerName)
                .kind(NodeKind.DOMAIN_SERVICE) // Handlers are treated as services
                .boundedContext(contextName)
                .span(decl.span())
                .astNode(decl)
                .build();
            
            registerNode(handlerNode);
            
            // Add event handler edge
            DepEdge edge = DepEdge.builder()
                .from(handlerNode)
                .to(eventNode.get())
                .edgeType(EdgeType.EVENT_HANDLER)
                .referenceSpan(decl.span())
                .build();
            graph.addEdge(edge);
            
            // Check if event is in different context
            if (!eventNode.get().boundedContext().equals(contextName)) {
                DepEdge crossEdge = DepEdge.builder()
                    .from(handlerNode)
                    .to(eventNode.get())
                    .edgeType(EdgeType.CROSS_CONTEXT)
                    .referenceSpan(decl.span())
                    .build();
                graph.addEdge(crossEdge);
            }
        }
        
        return null;
    }
    
    @Override
    public Void visitRepository(RepositoryDecl decl) {
        String contextName = currentContext();
        String qualifiedId = qualifiedId(contextName, decl.name());
        
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.REPOSITORY)
            .boundedContext(contextName)
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        
        // Add repository access edge to aggregate type
        if (decl.aggregateType() != null) {
            String aggregateTypeName = typeName(decl.aggregateType());
            Optional<DepNode> aggregateNode = findNode(contextName, aggregateTypeName);
            
            if (aggregateNode.isPresent()) {
                DepEdge edge = DepEdge.builder()
                    .from(node)
                    .to(aggregateNode.get())
                    .edgeType(EdgeType.REPOSITORY_ACCESS)
                    .referenceSpan(decl.aggregateType().span())
                    .build();
                graph.addEdge(edge);
            }
        }
        
        return super.visitRepository(decl);
    }
    
    @Override
    public Void visitFactory(FactoryDecl decl) {
        String contextName = currentContext();
        String qualifiedId = qualifiedId(contextName, decl.name());
        
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.FACTORY)
            .boundedContext(contextName)
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        
        return super.visitFactory(decl);
    }
    
    @Override
    public Void visitSpecification(SpecificationDecl decl) {
        String contextName = currentContext();
        String qualifiedId = qualifiedId(contextName, decl.name());
        
        DepNode node = DepNode.builder()
            .qualifiedId(qualifiedId)
            .name(decl.name())
            .kind(NodeKind.SPECIFICATION)
            .boundedContext(contextName)
            .span(decl.span())
            .astNode(decl)
            .build();
        
        registerNode(node);
        
        return super.visitSpecification(decl);
    }
    
    private void registerNode(DepNode node) {
        graph.addNode(node);
        nodeRegistry.put(node.qualifiedId(), node);
    }
    
    private void analyzeFieldDependency(DepNode fromNode, FieldDecl field) {
        String typeName = typeName(field.type());
        
        // Skip primitive types
        if (isPrimitiveType(typeName)) {
            return;
        }
        
        addTypeDependency(fromNode, typeName, EdgeType.TYPE_DEPENDENCY, field.type().span());
    }
    
    private void addTypeDependency(DepNode fromNode, String typeName, EdgeType edgeType, 
                                    uet.ndh.ddsl.ast.SourceSpan span) {
        Optional<DepNode> targetNode = findNodeByTypeName(typeName);
        
        if (targetNode.isPresent()) {
            DepNode toNode = targetNode.get();
            
            // Add main edge
            DepEdge edge = DepEdge.builder()
                .from(fromNode)
                .to(toNode)
                .edgeType(edgeType)
                .referenceSpan(span)
                .build();
            graph.addEdge(edge);
            
            // Add cross-context edge if needed
            if (!fromNode.boundedContext().equals(toNode.boundedContext())) {
                DepEdge crossEdge = DepEdge.builder()
                    .from(fromNode)
                    .to(toNode)
                    .edgeType(EdgeType.CROSS_CONTEXT)
                    .referenceSpan(span)
                    .build();
                graph.addEdge(crossEdge);
            }
        }
    }
    
    private Optional<DepNode> findNode(String contextName, String name) {
        String qualifiedId = qualifiedId(contextName, name);
        return Optional.ofNullable(nodeRegistry.get(qualifiedId));
    }
    
    private Optional<DepNode> findNodeByTypeName(String typeName) {
        // First try to resolve via symbol table
        Optional<Symbol> symbol = symbolTable.resolveType(typeName);
        
        if (symbol.isPresent()) {
            // Try to find in current context first
            String currentContext = currentContext();
            Optional<DepNode> node = findNode(currentContext, typeName);
            if (node.isPresent()) {
                return node;
            }
            
            // Search all contexts
            for (DepNode n : graph.allNodes()) {
                if (n.name().equals(typeName)) {
                    return Optional.of(n);
                }
            }
        }
        
        return Optional.empty();
    }
    
    private String currentContext() {
        return contextStack.isEmpty() ? "" : contextStack.peek();
    }
    
    private String qualifiedId(String... parts) {
        return String.join(".", parts);
    }
    
    private String typeName(TypeRef typeRef) {
        if (typeRef.isCollection() && !typeRef.typeArguments().isEmpty()) {
            return typeRef.typeArguments().get(0).name();
        }
        return typeRef.name();
    }
    
    private boolean isPrimitiveType(String typeName) {
        return Set.of("String", "Int", "Long", "Double", "Boolean", 
                      "Date", "DateTime", "UUID", "Void", "Decimal")
            .contains(typeName);
    }
    
    private String handlerName(EventHandlerDecl decl) {
        return "Handler_" + decl.targetEventName();
    }
}
