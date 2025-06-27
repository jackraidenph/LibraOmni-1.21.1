package dev.jackraidenph.libraomni.math.graph.iterator;

import dev.jackraidenph.libraomni.math.graph.Graph;

import java.util.*;

public class DepthFirstIterator<T> implements Iterator<T> {

    private static final int NO_ELEMENTS_FLAG = -2;

    private final Set<Integer> visited = new HashSet<>();
    private final Stack<Iterator<Integer>> stack = new Stack<>();
    private int next;
    private final Graph<T> graph;

    public DepthFirstIterator(Graph<T> graph) {
        this(graph, graph.getStartingIndex());
    }

    public DepthFirstIterator(Graph<T> graph, int startingIndex) {
        this.graph = graph;
        if (this.graph.hasIndex(startingIndex)) {
            this.stack.push(this.graph.getAdjacentIndices(startingIndex).iterator());
            this.next = startingIndex;
        } else {
            throw new IllegalArgumentException("Index does not exits: %d".formatted(startingIndex));
        }
    }

    @Override
    public void remove() {
        this.graph.removeNode(next);
    }

    @Override
    public boolean hasNext() {
        return this.next != NO_ELEMENTS_FLAG;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        try {
            this.visited.add(this.next);
            return this.graph.getNode(this.next);
        } finally {
            this.advance();
        }
    }

    private void advance() {
        Iterator<Integer> neighbors = this.stack.peek();
        do {
            while (!neighbors.hasNext()) {
                this.stack.pop();
                if (this.stack.isEmpty()) {
                    this.next = NO_ELEMENTS_FLAG;
                    return;
                }
                neighbors = this.stack.peek();
            }

            this.next = neighbors.next();
        } while (this.visited.contains(this.next));

        this.stack.push(this.graph.getAdjacentIndices(this.next).iterator());
    }
}
