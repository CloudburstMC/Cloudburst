package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.util.Direction;

import java.util.EnumMap;
import java.util.Map;

public enum DirectionEncoding {
    HORIZONTAL_SOUTH_WEST_NORTH_EAST(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST),
    HORIZONTAL_EAST_WEST_SOUTH_NORTH(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH),
    HORIZONTAL_NORTH_EAST_SOUTH_WEST(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST),
    FACING_DOWN_UP_SOUTH_NORTH_EAST_WEST(Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST),
    FACING_DOWN_UP_NORTH_SOUTH_WEST_EAST(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    private final Map<Direction, Integer> values;

    DirectionEncoding(Direction... directions) {
        EnumMap<Direction, Integer> values = new EnumMap<>(Direction.class);
        for (int index = 0; index < directions.length; index++) {
            Integer previous = values.put(directions[index], index);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate direction " + directions[index] + " in " + this.name());
            }
        }

        this.values = Map.copyOf(values);
    }

    public int encode(Direction direction) {
        Integer value = this.values.get(direction);
        if (value == null) {
            throw new IllegalArgumentException(direction + " cannot be represented by " + this.name());
        }

        return value;
    }
}
