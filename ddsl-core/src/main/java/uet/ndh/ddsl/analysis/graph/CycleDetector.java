package uet.ndh.ddsl.analysis.graph;

import java.util.*;

/**
 * Detects cycles in dependency graphs using DFS with 3-color marking.
 * Implements the algorithm described in Cormen et al. Introduction to Algorithms.
 * 
 * Colors:
 * - WHITE: Node has not been visited
 * - GRAY: Node is currently being processed (in recursion stack)
 * - BLACK: Node and all its descendants have been fully processed
 * 
 * A cycle exists if we encounter a GRAY node during DFS (back edge).
 */
public class CycleDetector {
    
    private enum Color {
        WHITE, GRAY, BLACK
    }
    
    /**
     * Represents a detected cycle in the dependency graph.
     * 
     * @param path List of nodes forming the cycle (including the repeated start node at end)
     * @param edgeType The dominant edge type in the cycle
     */
    public record Cycle(List<DepNode> path, EdgeType dominantEdgeType) {
        
        /**
         * Returns a string representation of the cycle path.
         */
        public String pathString() {
            if (path.isEmpty()) return "";
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) sb.append(" → ");
                sb.append(path.get(i).name());
            }
            return sb.toString();
        }
    }
    
    /**
     * Result of cycle detection containing found cycles and topological order.
     * 
     * @param cycles List of detected cycles (empty if acyclic)
     * @param topologicalOrder Valid topological ordering (empty if cycles exist)
     */
    public record Result(List<Cycle> cycles, List<DepNode> topologicalOrder) {
        
        /**
         * Returns true if the graph is acyclic (DAG).
         */
        public boolean isAcyclic() {
            return cycles.isEmpty();
        }
        
        /**
         * Returns true if cycles were detected.
         */
        public boolean hasCycles() {
            return !cycles.isEmpty();
        }
    }
    
    /**
     * Detects cycles in the entire dependency graph.
     */
    public Result detectCycles(DepGraph graph) {
        Objects.requireNonNull(graph, "graph cannot be null");
        return detectCyclesInternal(graph, graph.allNodes());
    }
    
    /**
     * Detects cycles within a specific bounded context.
     */
    public Result detectCyclesInContext(DepGraph graph, String boundedContext) {
        Objects.requireNonNull(graph, "graph cannot be null");
        Objects.requireNonNull(boundedContext, "boundedContext cannot be null");
        
        Set<DepNode> contextNodes = graph.nodesInContext(boundedContext);
        return detectCyclesInternal(graph, contextNodes);
    }
    
    /**
     * Detects cycles considering only specific edge types.
     */
    public Result detectCyclesWithEdgeTypes(DepGraph graph, Set<EdgeType> edgeTypes) {
        Objects.requireNonNull(graph, "graph cannot be null");
        Objects.requireNonNull(edgeTypes, "edgeTypes cannot be null");
        
        DepGraph filteredGraph = filterByEdgeTypes(graph, edgeTypes);
        return detectCycles(filteredGraph);
    }
    
    private Result detectCyclesInternal(DepGraph graph, Collection<DepNode> nodesToCheck) {
        Map<String, Color> colors = new HashMap<>();
        Map<String, String> parents = new HashMap<>();
        List<Cycle> cycles = new ArrayList<>();
        List<DepNode> finishOrder = new ArrayList<>();
        
        // Initialize all nodes as WHITE
        for (DepNode node : nodesToCheck) {
            colors.put(node.qualifiedId(), Color.WHITE);
        }
        
        // DFS from each unvisited node
        for (DepNode node : nodesToCheck) {
            if (colors.get(node.qualifiedId()) == Color.WHITE) {
                dfsVisit(graph, node, colors, parents, cycles, finishOrder, nodesToCheck);
            }
        }
        
        // Build topological order (reverse of finish order)
        Collections.reverse(finishOrder);
        
        // If cycles exist, return empty topological order
        List<DepNode> topoOrder = cycles.isEmpty() ? finishOrder : Collections.emptyList();
        
        return new Result(Collections.unmodifiableList(cycles), 
                         Collections.unmodifiableList(topoOrder));
    }
    
    private void dfsVisit(DepGraph graph, DepNode node, Map<String, Color> colors,
                         Map<String, String> parents, List<Cycle> cycles,
                         List<DepNode> finishOrder, Collection<DepNode> validNodes) {
        String nodeId = node.qualifiedId();
        colors.put(nodeId, Color.GRAY);
        
        // Visit all neighbors
        for (DepEdge edge : graph.outgoingEdges(nodeId)) {
            DepNode neighbor = edge.to();
            
            // Only consider nodes in our valid set
            if (!isInSet(neighbor, validNodes)) {
                continue;
            }
            
            String neighborId = neighbor.qualifiedId();
            Color neighborColor = colors.getOrDefault(neighborId, Color.WHITE);
            
            if (neighborColor == Color.GRAY) {
                // Found a back edge - cycle detected
                List<DepNode> cyclePath = reconstructCycle(node, neighbor, parents);
                EdgeType dominantType = determineDominantEdgeType(cyclePath, graph);
                cycles.add(new Cycle(cyclePath, dominantType));
            } else if (neighborColor == Color.WHITE) {
                // Continue DFS
                parents.put(neighborId, nodeId);
                dfsVisit(graph, neighbor, colors, parents, cycles, finishOrder, validNodes);
            }
            // If BLACK, already processed - no cycle through this path
        }
        
        // Mark as finished
        colors.put(nodeId, Color.BLACK);
        finishOrder.add(node);
    }
    
    private List<DepNode> reconstructCycle(DepNode from, DepNode to, Map<String, String> parents) {
        List<DepNode> cycle = new ArrayList<>();
        
        // Build path from 'from' node back to 'to' node
        DepNode current = from;
        while (current != null && !current.qualifiedId().equals(to.qualifiedId())) {
            cycle.add(current);
            String parentId = parents.get(current.qualifiedId());
            current = parentId != null ? findNode(parentId, cycle) : null;
        }
        
        // Add the target node (completes the cycle)
        if (current != null) {
            cycle.add(current);
        }
        
        // Reverse to get cycle in correct order (to -> ... -> from -> to)
        Collections.reverse(cycle);
        
        // Add 'from' node at end to show cycle completion
        cycle.add(from);
        
        return cycle;
    }
    
    private DepNode findNode(String qualifiedId, List<DepNode> candidates) {
        return candidates.stream()
            .filter(n -> n.qualifiedId().equals(qualifiedId))
            .findFirst()
            .orElse(null);
    }
    
    private boolean isInSet(DepNode node, Collection<DepNode> set) {
        return set.stream()
            .anyMatch(n -> n.qualifiedId().equals(node.qualifiedId()));
    }
    
    private EdgeType determineDominantEdgeType(List<DepNode> cyclePath, DepGraph graph) {
        if (cyclePath.size() < 2) {
            return EdgeType.TYPE_DEPENDENCY;
        }
        
        Map<EdgeType, Integer> edgeTypeCount = new HashMap<>();
        
        for (int i = 0; i < cyclePath.size() - 1; i++) {
            DepNode from = cyclePath.get(i);
            DepNode to = cyclePath.get(i + 1);
            
            for (DepEdge edge : graph.outgoingEdges(from.qualifiedId())) {
                if (edge.to().qualifiedId().equals(to.qualifiedId())) {
                    edgeTypeCount.merge(edge.edgeType(), 1, Integer::sum);
                }
            }
        }
        
        return edgeTypeCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(EdgeType.TYPE_DEPENDENCY);
    }
    
    private DepGraph filterByEdgeTypes(DepGraph graph, Set<EdgeType> edgeTypes) {
        DepGraph filtered = new DepGraph();
        
        // Add all nodes
        graph.allNodes().forEach(filtered::addNode);
        
        // Add only edges of specified types
        for (DepNode node : graph.allNodes()) {
            for (DepEdge edge : graph.outgoingEdges(node.qualifiedId())) {
                if (edgeTypes.contains(edge.edgeType())) {
                    filtered.addEdge(edge);
                }
            }
        }
        
        return filtered;
    }
}
