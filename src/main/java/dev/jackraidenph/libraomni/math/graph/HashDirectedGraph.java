package dev.jackraidenph.libraomni.math.graph;

import dev.jackraidenph.libraomni.math.graph.iterator.BreadthFirstIterator;

import java.util.*;
import java.util.Map.Entry;

public class HashDirectedGraph<T> implements IndexedGraph<T> {
    private final Map<Integer, SequencedSet<Integer>> adjacencySetsMap;
    private final Map<Integer, T> indexToNode;
    private final Map<T, Integer> nodeToIndex;
    private int startingIndex = -1;
    private int edgesAmount = 0;

    public HashDirectedGraph() {
        this(new HashMap<>(), new HashMap<>());
    }

    private HashDirectedGraph(Map<Integer, T> indexToNode, Map<T, Integer> nodeToIndex) {
        this.indexToNode = indexToNode;
        this.nodeToIndex = nodeToIndex;
        adjacencySetsMap = new HashMap<>();
    }

    public Set<Integer> getNodeIndices() {
        return Collections.unmodifiableSet(this.indexToNode.keySet());
    }

    public Collection<T> getNodes() {
        return Collections.unmodifiableCollection(this.indexToNode.values());
    }

    public Map<Integer, Set<Integer>> getEdges() {
        return Collections.unmodifiableMap(adjacencySetsMap);
    }

    public boolean addNode(int index, T node) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative");
        }

        if (!allowNullNodes() && node == null) {
            throw new IllegalArgumentException("Node must not be null");
        }

        if (!this.hasIndex(startingIndex)) {
            this.setStartingIndex(index);
        }

        this.indexToNode.put(index, node);
        Integer prevIndex = nodeToIndex.get(node);
        if (prevIndex != null && !prevIndex.equals(index)) {
            indexToNode.remove(prevIndex);
            adjacencySetsMap.remove(index);
        }
        this.nodeToIndex.put(node, index);
        this.adjacencySetsMap.put(index, new LinkedHashSet<>());
        return true;
    }

    public T getNode(int index) {
        return this.indexToNode.get(index);
    }

    public boolean removeNode(int index) {
        if (this.indexToNode.containsKey(index)) {
            this.nodeToIndex.remove(getNode(index));
            this.indexToNode.remove(index);
            this.adjacencySetsMap.remove(index);

            if (index == this.startingIndex) {
                this.startingIndex = -1;
            }

            return true;
        }
        return false;
    }

    public int getNodeIndex(T node) {
        Integer index = this.nodeToIndex.get(node);
        return index == null ? -1 : index;
    }

    private void indexSanityCheck(int index) {
        if (!hasIndex(index)) {
            throw new IllegalArgumentException("Index %d not in the graph".formatted(index));
        }
    }

    public boolean addEdge(int from, int to) {
        indexSanityCheck(from);
        indexSanityCheck(to);

        if(this.adjacencySetsMap.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to)) {
            edgesAmount++;
        }
        return true;
    }

    public boolean removeEdge(int from, int to) {
        indexSanityCheck(from);
        indexSanityCheck(to);

        if (this.adjacencySetsMap.get(from).remove(to)) {
            edgesAmount--;
        }
        if (adjacencySetsMap.get(from).isEmpty()) {
            adjacencySetsMap.remove(from);
        }
        return true;
    }

    public void setStartingIndex(int startingIndex) {
        if (startingIndex < 0) {
            throw new IllegalArgumentException("Index must be non-negative");
        }

        this.startingIndex = startingIndex;
    }

    @Override
    public boolean allowSelfLoops() {
        return true;
    }

    @Override
    public boolean allowNullNodes() {
        return true;
    }

    public int getStartingIndex() {
        return this.startingIndex;
    }

    /**
     * @param index Subgraph's root index
     * @return Subgraph of the parent directed graph starting at index
     */
    public HashDirectedGraph<T> getSubgraph(int index) {
        indexSanityCheck(index);

        HashDirectedGraph<T> subgraph = new HashDirectedGraph<>();

        Iterator<T> iterator = new BreadthFirstIterator<>(this, index);
        while (iterator.hasNext()) {
            T node = iterator.next();
            int i = this.getNodeIndex(node);
            subgraph.addNode(i, node);

            for (int adj : this.getAdjacentIndices(i)) {
                subgraph.addNode(adj, this.getNode(adj));
                subgraph.addEdge(i, adj);
            }
        }

        return subgraph;
    }

    /**
     * @return A directed graph backed by the same data as the current one, with adjacency reversed
     */
    public HashDirectedGraph<T> getReversed() {
        HashDirectedGraph<T> reversal = new HashDirectedGraph<>(this.indexToNode, this.nodeToIndex);

        for (Integer index : this.adjacencySetsMap.keySet()) {
            for (Integer adjacent : this.adjacencySetsMap.get(index)) {
                reversal.addEdge(adjacent, index);
            }
        }

        return reversal;
    }

    @Override
    public boolean isDirected() {
        return true;
    }

    @Override
    public boolean isAcyclic() {
        return false;
    }

    private boolean hasCycleStep(int current, Set<Integer> visited, Set<Integer> stack) {
        if (stack.contains(current)) {
            return true;
        }

        if (visited.contains(current)) {
            return false;
        }

        visited.add(current);
        stack.add(current);
        for (int i : getAdjacentIndices(current)) {
            if (hasCycleStep(i, visited, stack)) {
                return true;
            }
        }
        stack.remove(current);

        return false;
    }

    @Override
    public boolean hasCycles(int startingFrom) {
        return hasCycleStep(startingFrom, new HashSet<>(nodesAmount()), new HashSet<>(nodesAmount()));
    }

    @Override
    public int nodesAmount() {
        return indexToNode.size();
    }

    @Override
    public int edgesAmount() {
        return edgesAmount;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        StringJoiner nodesJoiner = new StringJoiner(",", "[", "]");
        for (Entry<Integer, T> idxToNode : indexToNode.entrySet()) {
            nodesJoiner.add(idxToNode.getKey() + "=\"" + idxToNode.getValue() + "\"");
        }

        builder.append(nodesJoiner);
        builder.append(", ");
        StringJoiner edgesJoiner = new StringJoiner(",", "[", "]");

        for (Integer idx : indexToNode.keySet()) {
            Collection<Integer> adj = this.getAdjacentIndices(idx);
            edgesJoiner.add(idx + "->" + adj.toString());
        }

        builder.append(edgesJoiner);
        builder.append("}");

        return builder.toString();
    }
}