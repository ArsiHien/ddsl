package uet.ndh.ddsl.analysis.graph;

import java.util.*;

/**
 * Validates bounded context isolation using BFS reachability analysis.
 * Ensures that cross-context interactions only occur through domain events,
 * enforcing the Bounded Context Isolation principle from DDD.
 */
public class BoundedContextIsolationValidator {
    
    /**
     * Represents a violation of bounded context isolation.
     * 
     * @param source The source component making the invalid reference
     * @param target The target component being improperly referenced
     * @param edge The edge representing the invalid dependency
     * @param violationType Type of isolation violation
     * @param message Human-readable description of the violation
     */
    public record IsolationViolation(
        DepNode source,
        DepNode target,
        DepEdge edge,
        ViolationType violationType,
        String message
    ) {
        /**
         * Types of bounded context isolation violations.
         */
        public enum ViolationType {
            /** Direct type reference across contexts (not through events). */
            DIRECT_CROSS_CONTEXT_REFERENCE,
            
            /** Event handler without corresponding event publisher. */
            ORPHANED_EVENT_HANDLER,
            
            /** Event published but never handled. */
            UNHANDLED_EVENT,
            
            /** Cyclic dependency between bounded contexts. */
            CONTEXT_CYCLE
        }
    }
    
    /**
     * Validates the entire graph for bounded context isolation issues.
     * 
     * @param graph The dependency graph to validate
     * @return List of isolation violations (empty if valid)
     */
    public List<IsolationViolation> validate(DepGraph graph) {
        Objects.requireNonNull(graph, "graph cannot be null");
        
        List<IsolationViolation> violations = new ArrayList<>();
        
        // Check for direct cross-context references (GRAPH002)
        violations.addAll(findDirectCrossContextReferences(graph));
        
        // Check for context-level cycles (GRAPH001 at context level)
        violations.addAll(findContextCycles(graph));
        
        // Check for orphaned handlers and unhandled events (GRAPH003/GRAPH004)
        violations.addAll(findEventIssues(graph));
        
        return Collections.unmodifiableList(violations);
    }
    
    /**
     * Validates isolation for a specific bounded context.
     */
    public List<IsolationViolation> validateContext(DepGraph graph, String boundedContext) {
        Objects.requireNonNull(graph, "graph cannot be null");
        Objects.requireNonNull(boundedContext, "boundedContext cannot be null");
        
        DepGraph subgraph = graph.subgraphForContext(boundedContext);
        return validate(subgraph);
    }
    
    private List<IsolationViolation> findDirectCrossContextReferences(DepGraph graph) {
        List<IsolationViolation> violations = new ArrayList<>();
        
        for (DepEdge edge : graph.crossContextEdges()) {
            // Skip event-based communications - they're allowed
            if (isEventBasedCommunication(edge)) {
                continue;
            }
            
            String message = String.format(
                "Direct cross-context reference from '%s' to '%s'. " +
                "Cross-context communication should go through domain events.",
                edge.from().name(),
                edge.to().name()
            );
            
            violations.add(new IsolationViolation(
                edge.from(),
                edge.to(),
                edge,
                IsolationViolation.ViolationType.DIRECT_CROSS_CONTEXT_REFERENCE,
                message
            ));
        }
        
        return violations;
    }
    
    private List<IsolationViolation> findContextCycles(DepGraph graph) {
        List<IsolationViolation> violations = new ArrayList<>();
        
        // Build context-level graph
        Map<String, Set<String>> contextDependencies = new HashMap<>();
        
        for (DepEdge edge : graph.crossContextEdges()) {
            String fromContext = edge.from().boundedContext();
            String toContext = edge.to().boundedContext();
            
            if (!fromContext.equals(toContext)) {
                contextDependencies
                    .computeIfAbsent(fromContext, k -> new HashSet<>())
                    .add(toContext);
            }
        }
        
        // Detect cycles at context level using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String context : contextDependencies.keySet()) {
            if (!visited.contains(context)) {
                if (hasCycleDFS(context, contextDependencies, visited, recursionStack, new ArrayList<>())) {
                    String message = String.format(
                        "Cyclic dependency detected between bounded contexts involving '%s'",
                        context
                    );
                    
                    violations.add(new IsolationViolation(
                        null, null, null,
                        IsolationViolation.ViolationType.CONTEXT_CYCLE,
                        message
                    ));
                }
            }
        }
        
        return violations;
    }
    
    private boolean hasCycleDFS(String context, Map<String, Set<String>> dependencies,
                                 Set<String> visited, Set<String> recursionStack,
                                 List<String> path) {
        visited.add(context);
        recursionStack.add(context);
        path.add(context);
        
        Set<String> neighbors = dependencies.getOrDefault(context, Collections.emptySet());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                if (hasCycleDFS(neighbor, dependencies, visited, recursionStack, path)) {
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                return true;
            }
        }
        
        path.remove(path.size() - 1);
        recursionStack.remove(context);
        return false;
    }
    
    private List<IsolationViolation> findEventIssues(DepGraph graph) {
        List<IsolationViolation> violations = new ArrayList<>();
        
        // Find all event publishes and handlers
        Set<DepNode> publishedEvents = new HashSet<>();
        Set<DepNode> handledEvents = new HashSet<>();
        
        Map<DepNode, List<DepEdge>> eventPublishers = new HashMap<>();
        Map<DepNode, List<DepEdge>> eventHandlers = new HashMap<>();
        
        for (DepEdge edge : graph.edgesOfType(EdgeType.EVENT_PUBLISH)) {
            publishedEvents.add(edge.to());
            eventPublishers.computeIfAbsent(edge.to(), k -> new ArrayList<>()).add(edge);
        }
        
        for (DepEdge edge : graph.edgesOfType(EdgeType.EVENT_HANDLER)) {
            handledEvents.add(edge.to());
            eventHandlers.computeIfAbsent(edge.to(), k -> new ArrayList<>()).add(edge);
        }
        
        // Find unhandled events (published but not handled)
        for (DepNode event : publishedEvents) {
            if (!handledEvents.contains(event)) {
                List<DepEdge> publishers = eventPublishers.get(event);
                if (publishers != null && !publishers.isEmpty()) {
                    DepEdge publishEdge = publishers.get(0);
                    
                    String message = String.format(
                        "Domain event '%s' is published by '%s' but has no handlers",
                        event.name(),
                        publishEdge.from().name()
                    );
                    
                    violations.add(new IsolationViolation(
                        publishEdge.from(),
                        event,
                        publishEdge,
                        IsolationViolation.ViolationType.UNHANDLED_EVENT,
                        message
                    ));
                }
            }
        }
        
        // Find orphaned handlers (handled but never published)
        for (DepNode event : handledEvents) {
            if (!publishedEvents.contains(event)) {
                List<DepEdge> handlers = eventHandlers.get(event);
                if (handlers != null && !handlers.isEmpty()) {
                    DepEdge handlerEdge = handlers.get(0);
                    
                    String message = String.format(
                        "Event handler for '%s' exists in '%s' but event is never published",
                        event.name(),
                        handlerEdge.from().name()
                    );
                    
                    violations.add(new IsolationViolation(
                        handlerEdge.from(),
                        event,
                        handlerEdge,
                        IsolationViolation.ViolationType.ORPHANED_EVENT_HANDLER,
                        message
                    ));
                }
            }
        }
        
        return violations;
    }
    
    private boolean isEventBasedCommunication(DepEdge edge) {
        return edge.edgeType() == EdgeType.EVENT_PUBLISH ||
               edge.edgeType() == EdgeType.EVENT_HANDLER ||
               edge.edgeType() == EdgeType.CROSS_CONTEXT;
    }
    
    /**
     * Performs BFS reachability analysis from a source node.
     * Returns all nodes reachable from the source.
     */
    public Set<DepNode> findReachableNodes(DepGraph graph, DepNode source) {
        Objects.requireNonNull(graph, "graph cannot be null");
        Objects.requireNonNull(source, "source cannot be null");
        
        Set<DepNode> visited = new HashSet<>();
        Queue<DepNode> queue = new LinkedList<>();
        
        queue.offer(source);
        visited.add(source);
        
        while (!queue.isEmpty()) {
            DepNode current = queue.poll();
            
            for (DepEdge edge : graph.outgoingEdges(current.qualifiedId())) {
                DepNode neighbor = edge.to();
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }
        
        return Collections.unmodifiableSet(visited);
    }
}
