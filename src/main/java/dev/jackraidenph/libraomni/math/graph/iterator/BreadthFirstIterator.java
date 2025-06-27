package dev.jackraidenph.libraomni.math.graph.iterator;

import dev.jackraidenph.libraomni.math.graph.Graph;

import java.util.*;

public class BreadthFirstIterator<T> implements Iterator<T> {

    private final Set<Integer> visited = new HashSet<>();
    private final Queue<Integer> queue = new LinkedList<>();
    private int currentIndex = -1;
    private int indexOnLevel = 0;
    private int currentWidth = 0;
    Queue<Integer> depthQueue = new LinkedList<>();

    private final Graph<T> graph;

    public BreadthFirstIterator(Graph<T> graph) {
        this(graph, graph.getStartingIndex());
    }

    public BreadthFirstIterator(Graph<T> graph, int startingIndex) {
        this.graph = graph;
        if (this.getGraph().hasIndex(startingIndex)) {
            this.queue.add(startingIndex);
            this.depthQueue.add(0);
            this.visited.add(startingIndex);
        } else {
            throw new IllegalArgumentException("Index does not exits: %d".formatted(startingIndex));
        }
    }

    public Graph<T> getGraph() {
        return this.graph;
    }

    @Override
    public void remove() {
        this.getGraph().removeNode(this.currentIndex);
    }

    @Override
    public boolean hasNext() {
        return !this.queue.isEmpty();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        int next = queue.remove();
        this.currentIndex = next;

        Collection<Integer> adjacent = this.getGraph().getAdjacentIndices(next);
        int depth = depthQueue.remove();
        this.currentWidth = depth;

        if (!depthQueue.isEmpty() && depth == this.getLevel()) {
            this.indexOnLevel++;
        } else {
            this.indexOnLevel = 0;
        }

        for (int adj : adjacent) {
            if (!this.visited.contains(adj)) {
                this.visited.add(adj);
                this.queue.add(adj);
                this.depthQueue.add(depth + 1);
            }
        }

        return this.getGraph().getNode(next);
    }

    public int getLevel() {
        return this.currentWidth;
    }

    public int getIndexOnLevel() {
        return this.indexOnLevel;
    }

}
