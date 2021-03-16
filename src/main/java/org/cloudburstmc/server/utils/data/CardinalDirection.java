package org.cloudburstmc.server.utils.data;

import org.cloudburstmc.api.util.Direction;

public enum CardinalDirection {
    SOUTH(Direction.SOUTH),
    SOUTH_SOUTHWEST(Direction.SOUTH),
    SOUTHWEST(Direction.WEST),
    WEST_SOUTHWEST(Direction.WEST),
    WEST(Direction.WEST),
    WEST_NORTHWEST(Direction.WEST),
    NORTHWEST(Direction.NORTH),
    NORTH_NORTHWEST(Direction.NORTH),
    NORTH(Direction.NORTH),
    NORTH_NORTHEAST(Direction.NORTH),
    NORTHEAST(Direction.EAST),
    EAST_NORTHEAST(Direction.EAST),
    EAST(Direction.EAST),
    EAST_SOUTHEAST(Direction.EAST),
    SOUTHEAST(Direction.SOUTH),
    SOUTH_SOUTHEAST(Direction.SOUTH);

    private final Direction direction;

    CardinalDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction toDirection() {
        return direction;
    }
}
