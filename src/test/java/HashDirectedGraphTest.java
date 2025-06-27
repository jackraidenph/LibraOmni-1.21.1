import dev.jackraidenph.libraomni.math.graph.HashDirectedGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HashDirectedGraphTest {

    private HashDirectedGraph<String> graph;

    @BeforeEach
    public void setup() {
        graph = new HashDirectedGraph<>();
    }

    @Test
    public void testAddNode() {
        boolean added = graph.addNode(0, "Test1");
        assertTrue(added);
        assertEquals("Test1", graph.getNode(0));
    }

    @Test
    public void testRemove() {
        graph.addNode(0, "Test1");
        boolean remove = graph.removeNode(0);
        assertTrue(remove);

        remove = graph.removeNode(0);
        assertFalse(remove);
        assertNull(graph.getNode(0));
    }

    @Test
    public void testAddEdgeIndices() {
        graph.addNode(0, "Test1");
        graph.addNode(1, "Test2");

        boolean added = graph.addEdge(0, 1);

        assertTrue(added);

        assertTrue(graph.getAdjacentIndices(0).contains(1));
        assertTrue(graph.getAdjacentNodes(0).contains("Test2"));
    }

    @Test
    public void testAddRemoveEdgeIndexIndex() {
        graph.addNode(0, "Test1");
        graph.addNode(1, "Test2");

        assertTrue(graph.addEdge(0, 1));
        assertTrue(graph.getAdjacentIndices(0).contains(1));
        assertTrue(graph.getAdjacentNodes(0).contains("Test2"));
    }

    @Test
    public void testAddRemoveEdgeIndexNode() {
        graph.addNode(0, "Test1");
        graph.addNode(1, "Test2");

        assertTrue(graph.addEdge(0, "Test2"));
        assertTrue(graph.getAdjacentIndices(0).contains(1));
        assertTrue(graph.getAdjacentNodes(0).contains("Test2"));
    }

    @Test
    public void testAddRemoveEdgeNodeIndex() {
        graph.addNode(0, "Test1");
        graph.addNode(1, "Test2");

        assertTrue(graph.addEdge("Test1", 1));
        assertTrue(graph.getAdjacentIndices(0).contains(1));
        assertTrue(graph.getAdjacentNodes(0).contains("Test2"));
    }

    @Test
    public void testAddRemoveEdgeNodeNode() {
        graph.addNode(0, "Test1");
        graph.addNode(1, "Test2");

        assertTrue(graph.addEdge("Test1", "Test2"));
        assertTrue(graph.getAdjacentIndices(0).contains(1));
        assertTrue(graph.getAdjacentNodes(0).contains("Test2"));
    }

    @Test
    public void testBreadthFirstSearch() {
        graph.addNode(0, "0");
        graph.addNode(1, "1");
        graph.addNode(2, "2");
        graph.addNode(3, "3");
        graph.addNode(4, "4");

        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(0, 3);

        graph.addEdge(1, 4);
        graph.addEdge(2, 4);
        graph.addEdge(3, 4);

        List<String> list = new ArrayList<>();
        graph.breadthFirstIterator().forEachRemaining(list::add);

        assertEquals(List.of("0", "1", "2", "3", "4"), list);
    }

    @Test
    public void testDepthFirstSearch() {
        graph.addNode(0, "0");
        graph.addNode(1, "1");
        graph.addNode(2, "2");
        graph.addNode(3, "3");
        graph.addNode(4, "4");

        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(0, 3);

        graph.addEdge(1, 4);
        graph.addEdge(2, 4);
        graph.addEdge(3, 4);

        List<String> list = new ArrayList<>();
        graph.depthFirstIterator().forEachRemaining(list::add);

        assertEquals(List.of("0", "1", "4", "2", "3"), list);
    }

    @Test
    public void testGraphGetReversed() {
        graph.addNode(0, "0");
        graph.addNode(1, "1");
        graph.addNode(2, "2");
        graph.addNode(3, "3");
        graph.addNode(4, "4");

        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(0, 3);

        graph.addEdge(1, 4);
        graph.addEdge(2, 4);
        graph.addEdge(3, 4);

        HashDirectedGraph<String> reversal = graph.getReversed();

        assertTrue(reversal.getAdjacentIndices(0).isEmpty());
        assertEquals(Set.of("0"), reversal.getAdjacentNodes(1));
        assertEquals(Set.of("0"), reversal.getAdjacentNodes(2));
        assertEquals(Set.of("0"), reversal.getAdjacentNodes(3));
        assertEquals(Set.of("1", "2", "3"), reversal.getAdjacentNodes(4));
    }

    @Test
    public void testSubgraph() {
        graph.addNode(0, "0");
        graph.addNode(1, "1");
        graph.addNode(2, "2");
        graph.addNode(3, "3");
        graph.addNode(4, "4");
        graph.addNode(5, "5");
        graph.addNode(6, "6");

        graph.addEdge(0, 1);
        graph.addEdge(1, 2);

        graph.addEdge(0, 3);
        graph.addEdge(3, 4);
        graph.addEdge(3, 5);
        graph.addEdge(4, 6);

        HashDirectedGraph<String> subgraph = graph.getSubgraph(3);

        List<String> subgraphNodes = new ArrayList<>();
        subgraph.depthFirstIterator().forEachRemaining(subgraphNodes::add);

        assertEquals(List.of("3", "4", "6", "5"), subgraphNodes);
    }

    @Test
    public void testHasCycles() {
        graph.addNode(0, "0");
        graph.addNode(1, "1");
        graph.addNode(2, "2");
        graph.addNode(3, "3");
        graph.addNode(4, "4");

        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);
        graph.addEdge(4, 1);

        assertTrue(graph.hasCycles());
    }

    @Test
    public void testHasNoCycles() {
        graph.addNode(0, "0");
        graph.addNode(1, "1");
        graph.addNode(2, "2");
        graph.addNode(3, "3");
        graph.addNode(4, "4");

        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);

        assertFalse(graph.hasCycles());
    }

    @Test
    public void testIsDisconnected() {
        graph.addNode(0, "0");
        graph.addNode(1, "1");
        graph.addNode(2, "2");
        graph.addNode(3, "3");
        graph.addNode(4, "4");

        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);

        graph.addNode(5, "5");

        assertTrue(graph.isDisconnected());
    }

    @Test
    public void testIsNotDisconnected() {
        graph.addNode(0, "0");
        graph.addNode(1, "1");
        graph.addNode(2, "2");
        graph.addNode(3, "3");
        graph.addNode(4, "4");

        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);

        assertFalse(graph.isDisconnected());
    }
}
