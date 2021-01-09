package org.cloudburstmc.api.util.data;

import com.google.common.collect.ImmutableSet;
import org.cloudburstmc.api.util.Direction;

import java.util.Set;

public class VineDirection implements Comparable<VineDirection> {

    private final ImmutableSet<Direction> directions;
    private final byte data;

    private VineDirection(ImmutableSet<Direction> directions) {
        this.directions = directions;

        byte bits = 0;
        for (Direction direction : directions) {
            if (direction.getHorizontalIndex() == -1) {
                throw new IllegalArgumentException("Invalid direction " + direction);
            }

            bits |= 1 << direction.getHorizontalIndex();
        }

        this.data = bits;
    }

    public ImmutableSet<Direction> getDirections() {
        return directions;
    }

    public byte getData() {
        return data;
    }

    @Override
    public int compareTo(VineDirection o) {
        return Byte.compare(this.data, o.data);
    }

    public static VineDirection of(Direction... directions) {
        return new VineDirection(ImmutableSet.copyOf(directions));
    }

    public static VineDirection of(Set<Direction> directions) {
        return new VineDirection(ImmutableSet.copyOf(directions));
    }
}
