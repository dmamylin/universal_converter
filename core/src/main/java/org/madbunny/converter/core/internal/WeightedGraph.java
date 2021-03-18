package org.madbunny.converter.core.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.function.Function;

public class WeightedGraph {
    public static class Builder {
        private final Map<String, Set<TargetNodeInfo>> edges = new HashMap<>();

        Builder withEdge(String from, String to, BigDecimal weight) {
            var neighbors = edges.computeIfAbsent(from, k -> new HashSet<>());
            neighbors.add(new TargetNodeInfo(to, weight));
            return this;
        }

        WeightedGraph build() {
            return new WeightedGraph(edges);
        }
    }

    public static class Edge {
        public final String from;
        public final String to;
        public final BigDecimal weight;

        private Edge(String from, String to, BigDecimal weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }

    public static enum TraversalState {
        STOP,
        CONTINUE
    }

    private static class TargetNodeInfo {
        public final String nodeId;
        public final BigDecimal edgeWeight;

        public TargetNodeInfo(String nodeId, BigDecimal edgeWeight) {
            this.nodeId = nodeId;
            this.edgeWeight = edgeWeight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TargetNodeInfo that = (TargetNodeInfo)o;
            return nodeId.equals(that.nodeId);
        }

        @Override
        public int hashCode() {
            return nodeId.hashCode();
        }
    }

    private static final Set<TargetNodeInfo> EMPTY_NEIGHBORS = new HashSet<>();

    private final Map<String, Set<TargetNodeInfo>> edges;

    private WeightedGraph(Map<String, Set<TargetNodeInfo>> edges) {
        this.edges = edges;
    }

    /**
     * Traverses the graph in a breadth-first manner and multiplicatively accumulates the total weight of a path.
     * @param origin    an original nodeId to start the traversal from.
     * @param visitor   a callback which is being called iff we've found a non-visited neighbor.
     * @return          multiplication of all weights along the resulting path if a visitor returned TraversalState.STOP
     *                  during the traversal or an empty Optional if the traversal was not interrupted.
    */
    public Optional<BigDecimal> traverseBreadthFirst(String origin, MathContext mathCtx, Function<Edge, TraversalState> visitor) {
        // Weights for paths from origin to nodes in the map
        var pathWeight = new HashMap<String, BigDecimal>(){{
            put(origin, BigDecimal.ONE);
        }};
        Queue<Edge> toVisit = new ArrayDeque<>(){{
            add(new Edge(null, origin, BigDecimal.ONE));
        }};

        while (!toVisit.isEmpty()) {
            var edge = toVisit.peek();
            toVisit.remove();

            // Skip the origin
            if (edge.from != null) {
                var prevWeight = pathWeight.get(edge.from);
                var newWeight = prevWeight.multiply(edge.weight, mathCtx);
                var state = visitor.apply(edge);
                if (state == TraversalState.STOP) {
                    return Optional.of(newWeight);
                }
                pathWeight.put(edge.to, newWeight);
            }

            var neighbors = edges.getOrDefault(edge.to, EMPTY_NEIGHBORS);
            for (var neighbor : neighbors) {
                if (pathWeight.containsKey(neighbor.nodeId)) {
                    // The neighbor is already visited
                    continue;
                }
                toVisit.add(new Edge(edge.to, neighbor.nodeId, neighbor.edgeWeight));
            }
        }

        return Optional.empty();
    }
}
