package dev.jackraidenph.libraomni.math.graph.iterator;

import dev.jackraidenph.libraomni.math.graph.IndexedGraph;

import java.util.*;

public class DepthFirstIterator<T> implements Iterator<T> {

    private final IndexedGraph<T> graph;
    private int current;
    private final Deque<Integer> stack = new ArrayDeque<>();
    private final Set<Integer> visited;

    public DepthFirstIterator(IndexedGraph<T> graph) {
        this(graph, graph.getStartingIndex());
    }

    public DepthFirstIterator(IndexedGraph<T> graph, int startingIndex) {
        this.graph = graph;
        visited = new HashSet<>(graph.nodesAmount());
        if (this.graph.hasIndex(startingIndex)) {
            this.stack.push(startingIndex);
            this.current = startingIndex;
        } else {
            throw new IllegalArgumentException("Index does not exits: %d".formatted(startingIndex));
        }
    }

    @Override
    public void remove() {
        this.graph.removeNode(current);
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public T next() {
        current = stack.pop();
        visited.add(current);
        T currentNode = graph.getNode(current);
        for (Integer adj : graph.getAdjacentIndices(current)) {
            if (!visited.contains(adj)) {
                stack.push(adj);
            }
        }
        return currentNode;
    }
}
