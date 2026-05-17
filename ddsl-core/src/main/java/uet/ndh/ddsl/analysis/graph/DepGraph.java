package uet.ndh.ddsl.analysis.graph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Directed dependency graph for DDD domain components.
 * Uses adjacency list representation for efficient traversal.
 */
public class DepGraph {
    
    private final Map<String, DepNode> nodes;
    private final Map<String, List<DepEdge>> outgoingEdges;
    private final Map<String, List<DepEdge>> incomingEdges;
    
    public DepGraph() {
        this.nodes = new HashMap<>();
        this.outgoingEdges = new HashMap<>();
        this.incomingEdges = new HashMap<>();
    }
    
    /**
     * Adds a node to the graph.
     * @throws IllegalArgumentException if a node with the same qualifiedId already exists
     */
    public void addNode(DepNode node) {
        Objects.requireNonNull(node, "node cannot be null");
        if (nodes.containsKey(node.qualifiedId())) {
            throw new IllegalArgumentException(
                "Node already exists: " + node.qualifiedId());
        }
        nodes.put(node.qualifiedId(), node);
        outgoingEdges.put(node.qualifiedId(), new ArrayList<>());
        incomingEdges.put(node.qualifiedId(), new ArrayList<>());
    }
    
    /**
     * Adds an edge to the graph.
     * Both source and target nodes must already exist in the graph.
     * @throws IllegalArgumentException if either endpoint doesn't exist
     */
    public void addEdge(DepEdge edge) {
        Objects.requireNonNull(edge, "edge cannot be null");
        
        String fromId = edge.from().qualifiedId();
        String toId = edge.to().qualifiedId();
        
        if (!nodes.containsKey(fromId)) {
            throw new IllegalArgumentException("Source node not found: " + fromId);
        }
        if (!nodes.containsKey(toId)) {
            throw new IllegalArgumentException("Target node not found: " + toId);
        }
        
        outgoingEdges.get(fromId).add(edge);
        incomingEdges.get(toId).add(edge);
    }
    
    /**
     * Returns a node by its qualified ID.
     */
    public Optional<DepNode> node(String qualifiedId) {
        return Optional.ofNullable(nodes.get(qualifiedId));
    }
    
    /**
     * Returns all nodes in the graph.
     */
    public Collection<DepNode> allNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }
    
    /**
     * Returns the number of nodes in the graph.
     */
    public int nodeCount() {
        return nodes.size();
    }
    
    /**
     * Returns the number of edges in the graph.
     */
    public int edgeCount() {
        return outgoingEdges.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * Returns outgoing edges from a node.
     */
    public List<DepEdge> outgoingEdges(String qualifiedId) {
        return Collections.unmodifiableList(
            outgoingEdges.getOrDefault(qualifiedId, Collections.emptyList()));
    }
    
    /**
     * Returns incoming edges to a node.
     */
    public List<DepEdge> incomingEdges(String qualifiedId) {
        return Collections.unmodifiableList(
            incomingEdges.getOrDefault(qualifiedId, Collections.emptyList()));
    }
    
    /**
     * Returns all edges of a specific type.
     */
    public List<DepEdge> edgesOfType(EdgeType edgeType) {
        Objects.requireNonNull(edgeType, "edgeType cannot be null");
        return outgoingEdges.values().stream()
            .flatMap(List::stream)
            .filter(e -> e.edgeType() == edgeType)
            .toList();
    }
    
    /**
     * Returns all cross-context edges.
     */
    public List<DepEdge> crossContextEdges() {
        return outgoingEdges.values().stream()
            .flatMap(List::stream)
            .filter(DepEdge::isCrossContext)
            .toList();
    }
    
    /**
     * Returns all nodes within a specific bounded context.
     */
    public Set<DepNode> nodesInContext(String boundedContext) {
        Objects.requireNonNull(boundedContext, "boundedContext cannot be null");
        return nodes.values().stream()
            .filter(n -> boundedContext.equals(n.boundedContext()))
            .collect(Collectors.toUnmodifiableSet());
    }
    
    /**
     * Returns all bounded context names in the graph.
     */
    public Set<String> boundedContexts() {
        return nodes.values().stream()
            .map(DepNode::boundedContext)
            .filter(bc -> !bc.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }
    
    /**
     * Creates a subgraph containing only nodes from a specific bounded context.
     */
    public DepGraph subgraphForContext(String boundedContext) {
        Objects.requireNonNull(boundedContext, "boundedContext cannot be null");
        DepGraph subgraph = new DepGraph();
        
        Set<String> contextNodeIds = nodesInContext(boundedContext).stream()
            .map(DepNode::qualifiedId)
            .collect(Collectors.toSet());
        
        // Add nodes
        nodes.values().stream()
            .filter(n -> boundedContext.equals(n.boundedContext()))
            .forEach(subgraph::addNode);
        
        // Add edges where both endpoints are in the context
        outgoingEdges.values().stream()
            .flatMap(List::stream)
            .filter(e -> contextNodeIds.contains(e.from().qualifiedId()))
            .filter(e -> contextNodeIds.contains(e.to().qualifiedId()))
            .forEach(subgraph::addEdge);
        
        return subgraph;
    }
    
    /**
     * Creates a subgraph containing only cross-context edges.
     */
    public DepGraph crossContextGraph() {
        DepGraph subgraph = new DepGraph();
        
        // Add all nodes that participate in cross-context edges
        Set<DepNode> participatingNodes = new HashSet<>();
        crossContextEdges().forEach(e -> {
            participatingNodes.add(e.from());
            participatingNodes.add(e.to());
        });
        participatingNodes.forEach(subgraph::addNode);
        
        // Add only cross-context edges
        crossContextEdges().forEach(subgraph::addEdge);
        
        return subgraph;
    }
    
    /**
     * Returns a topological ordering of the graph nodes (Kahn's algorithm).
     * Returns empty list if the graph contains cycles.
     */
    public List<DepNode> topologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();
        nodes.keySet().forEach(id -> inDegree.put(id, 0));
        
        // Calculate in-degrees
        for (List<DepEdge> edges : outgoingEdges.values()) {
            for (DepEdge edge : edges) {
                inDegree.merge(edge.to().qualifiedId(), 1, Integer::sum);
            }
        }
        
        // Start with nodes having no incoming edges
        Queue<DepNode> queue = new LinkedList<>();
        nodes.values().forEach(node -> {
            if (inDegree.getOrDefault(node.qualifiedId(), 0) == 0) {
                queue.offer(node);
            }
        });
        
        List<DepNode> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            DepNode current = queue.poll();
            result.add(current);
            
            for (DepEdge edge : outgoingEdges(current.qualifiedId())) {
                String neighborId = edge.to().qualifiedId();
                int newDegree = inDegree.get(neighborId) - 1;
                inDegree.put(neighborId, newDegree);
                
                if (newDegree == 0) {
                    queue.offer(edge.to());
                }
            }
        }
        
        // If we couldn't process all nodes, there's a cycle
        if (result.size() != nodes.size()) {
            return Collections.emptyList();
        }
        
        return Collections.unmodifiableList(result);
    }
    
    @Override
    public String toString() {
        return String.format("DepGraph[nodes=%d, edges=%d]", nodeCount(), edgeCount());
    }
}
