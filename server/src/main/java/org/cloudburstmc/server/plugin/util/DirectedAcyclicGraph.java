package org.cloudburstmc.server.plugin.util;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.*;

@ToString
@EqualsAndHashCode
public class DirectedAcyclicGraph<T> {
    private final Deque<Node<T>> nodes = new ArrayDeque<>();

    public Collection<T> sort() throws GraphException {
        // Now find nodes that have no edges.
        Queue<Node<T>> noEdges = getNodesWithNoEdges();

        // Run Kahn's algorithm.
        Collection<T> sorted = new ArrayDeque<>();
        while (!noEdges.isEmpty()) {
            Node<T> itemNode = noEdges.poll();
            T item = itemNode.getData();
            sorted.add(item);

            for (Node<T> node : withEdge(item)) {
                node.removeEdge(itemNode);
                if (node.getAdjacent().isEmpty()) {
                    if (!noEdges.contains(node)) {
                        noEdges.add(node);
                    }
                }
            }
        }

        if (hasEdges()) {
            throw new CycleException("Cycle found: " + toString());
        }

        return sorted;
    }

    public void addEdges(T one, T two) {
        Node<T> nodeOne = add(one);
        Node<T> nodeTwo = add(two);
        nodeOne.addEdge(nodeTwo);
    }

    public Node<T> add(T t) {
        Optional<Node<T>> willAdd = get(t);
        if (!willAdd.isPresent()) {
            Node<T> node = new Node<>(t);
            nodes.add(node);
            return node;
        } else {
            return willAdd.get();
        }
    }

    public Optional<Node<T>> get(T t) {
        for (Node<T> node : nodes) {
            if (node.data.equals(t)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public void remove(Node<T> node) {
        for (Node<T> tNode : nodes) {
            tNode.removeEdge(node);
        }
        nodes.remove(node);
    }

    public Collection<Node<T>> withEdge(T t) {
        Optional<Node<T>> inOptional = get(t);
        if (!inOptional.isPresent()) {
            return Collections.emptyList();
        }
        Node<T> in = inOptional.get();
        List<Node<T>> queue = new ArrayList<>();
        for (Node<T> node : nodes) {
            if (node.isAdjacent(in)) {
                queue.add(node);
            }
        }
        return queue;
    }

    public boolean hasEdges() {
        for (Node<T> node : nodes) {
            if (!node.adjacent.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public Deque<Node<T>> getNodesWithNoEdges() {
        Deque<Node<T>> found = new ArrayDeque<>();
        for (Node<T> node : nodes) {
            if (node.getAdjacent().isEmpty()) {
                found.add(node);
            }
        }
        return found;
    }

    @Value
    public static class Node<T> {
        T data;
        Deque<Node<T>> adjacent = new ArrayDeque<>();

        public void addEdge(Node<T> edge) {
            if (!isAdjacent(edge)) {
                adjacent.add(edge);
            }
        }

        public boolean removeEdge(Node<T> edge) {
            return adjacent.remove(edge);
        }

        public boolean isAdjacent(Node<T> edge) {
            return adjacent.contains(edge);
        }
    }
}
