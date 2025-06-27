package dev.jackraidenph.libraomni.math.graph;

import dev.jackraidenph.libraomni.math.graph.iterator.BreadthFirstIterator;
import dev.jackraidenph.libraomni.math.graph.iterator.DepthFirstIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public interface IndexedGraph<T> extends Iterable<T> {
    Collection<Integer> getNodeIndices();

    Collection<T> getNodes();

    boolean addNode(int index, T node);

    T getNode(int index);

    boolean removeNode(int index);

    default boolean removeNode(T node) {
        return removeNode(getNodeIndex(node));
    }

    int getNodeIndex(T node);

    default boolean hasIndex(int index) {
        return getNodeIndices().contains(index);
    }

    default boolean hasNode(T node) {
        return getNodes().contains(node);
    }

    default Collection<Integer> getAdjacentIndices(int index) {
        if (getEdges() == null) {
            return Collections.emptySet();
        }

        Collection<Integer> adjacent = getEdges().get(index);
        if (adjacent == null) {
            return Collections.emptySet();
        }

        return adjacent;
    }

    default Collection<Integer> getAdjacentIndices(T node) {
        return getAdjacentIndices(getNodeIndex(node));
    }

    default Collection<T> getAdjacentNodes(int index) {
        return getAdjacentIndices(index).stream().map(this::getNode).collect(Collectors.toSet());
    }

    default Collection<T> getAdjacentNodes(T node) {
        return getAdjacentNodes(getNodeIndex(node));
    }

    boolean removeEdge(int from, int to);

    default boolean removeEdge(T from, int to) {
        return removeEdge(getNodeIndex(from), to);
    }

    default boolean removeEdge(int from, T to) {
        return removeEdge(from, getNodeIndex(to));
    }

    default boolean removeEdge(T from, T to) {
        return removeEdge(getNodeIndex(from), getNodeIndex(to));
    }

    boolean addEdge(int from, int to);

    default boolean addEdge(T from, int to) {
        return addEdge(getNodeIndex(from), to);
    }

    default boolean addEdge(int from, T to) {
        return addEdge(from, getNodeIndex(to));
    }

    default boolean addEdge(T from, T to) {
        return addEdge(getNodeIndex(from), getNodeIndex(to));
    }

    int getStartingIndex();

    Map<Integer, ? extends Collection<Integer>> getEdges();

    boolean isDirected();

    boolean isAcyclic();

    boolean hasCycles();

    @NotNull
    @Override
    default BreadthFirstIterator<T> iterator() {
        return this.breadthFirstIterator();
    }

    default BreadthFirstIterator<T> breadthFirstIterator() {
        return new BreadthFirstIterator<>(this);
    }

    default DepthFirstIterator<T> depthFirstIterator() {
        return new DepthFirstIterator<>(this);
    }
}
