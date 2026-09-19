package org.cloudburstmc.api.entity.ai.route;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * A node in a pathfinding route with A* cost fields.
 *
 * @author daoge_cmd
 */
@Getter
@Setter
public class Node implements Comparable<Node> {

    protected final Vector3f vector;
    protected double G;
    protected double H;
    protected Node parent;

    public Node(Vector3f vector) {
        this.vector = Vector3f.from(vector);
    }

    public Node(double x, double y, double z) {
        this.vector = Vector3f.from(x, y, z);
    }

    public double getF() {
        return G + H;
    }

    @Override
    public int compareTo(Node o) {
        return Double.compare(this.getF(), o.getF());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Node other)) return false;
        return Double.compare(vector.getX(), other.vector.getX()) == 0 && Double.compare(vector.getY(), other.vector.getY()) == 0 && Double.compare(vector.getZ(), other.vector.getZ()) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(vector.getX());
        bits = 31 * bits + Double.doubleToLongBits(vector.getY());
        bits = 31 * bits + Double.doubleToLongBits(vector.getY());
        return Long.hashCode(bits);
    }

    @Override
    public String toString() {
        return "Node{" + vector.getX() + ", " + vector.getY() + ", " + vector.getZ() + ", G=" + G + ", H=" + H + ", F=" + getF() + "}";
    }
}
