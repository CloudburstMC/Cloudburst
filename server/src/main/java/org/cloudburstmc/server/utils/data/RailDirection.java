package org.cloudburstmc.server.utils.data;

import org.cloudburstmc.api.util.Direction;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public enum RailDirection {
    NORTH_SOUTH(State.STRAIGHT, Direction.NORTH, Direction.SOUTH, null),
    EAST_WEST(State.STRAIGHT, Direction.EAST, Direction.WEST, null),
    ASCENDING_EAST(State.ASCENDING, Direction.EAST, Direction.WEST, Direction.EAST),
    ASCENDING_WEST(State.ASCENDING, Direction.EAST, Direction.WEST, Direction.WEST),
    ASCENDING_NORTH(State.ASCENDING, Direction.NORTH, Direction.SOUTH, Direction.NORTH),
    ASCENDING_SOUTH(State.ASCENDING, Direction.NORTH, Direction.SOUTH, Direction.SOUTH),
    CURVED_SOUTH_EAST(State.CURVED, Direction.SOUTH, Direction.EAST, null),
    CURVED_SOUTH_WEST(State.CURVED, Direction.SOUTH, Direction.WEST, null),
    CURVED_NORTH_WEST(State.CURVED, Direction.NORTH, Direction.WEST, null),
    CURVED_NORTH_EAST(State.CURVED, Direction.NORTH, Direction.EAST, null);

    private final State state;
    private final List<Direction> connectingDirections;
    private final Direction ascendingDirection;

    RailDirection(State state, Direction from, Direction to, Direction ascendingDirection) {
        this.state = state;
        this.connectingDirections = Arrays.asList(from, to);
        this.ascendingDirection = ascendingDirection;
    }

    public static RailDirection straight(Direction face) {
        switch (face) {
            case NORTH:
            case SOUTH:
                return NORTH_SOUTH;
            case EAST:
            case WEST:
                return EAST_WEST;
        }
        return NORTH_SOUTH;
    }

    public static RailDirection ascending(Direction face) {
        switch (face) {
            case NORTH:
                return ASCENDING_NORTH;
            case SOUTH:
                return ASCENDING_SOUTH;
            case EAST:
                return ASCENDING_EAST;
            case WEST:
                return ASCENDING_WEST;
        }
        return ASCENDING_EAST;
    }

    public static RailDirection curved(Direction f1, Direction f2) {
        for (RailDirection o : new RailDirection[]{CURVED_SOUTH_EAST, CURVED_SOUTH_WEST, CURVED_NORTH_WEST, CURVED_NORTH_EAST}) {
            if (o.connectingDirections.contains(f1) && o.connectingDirections.contains(f2)) {
                return o;
            }
        }
        return CURVED_SOUTH_EAST;
    }

    public static RailDirection straightOrCurved(Direction f1, Direction f2) {
        for (RailDirection o : new RailDirection[]{NORTH_SOUTH, EAST_WEST, CURVED_SOUTH_EAST, CURVED_SOUTH_WEST, CURVED_NORTH_WEST, CURVED_NORTH_EAST}) {
            if (o.connectingDirections.contains(f1) && o.connectingDirections.contains(f2)) {
                return o;
            }
        }
        return NORTH_SOUTH;
    }

    public boolean hasConnectingDirections(Direction... faces) {
        return Stream.of(faces).allMatch(connectingDirections::contains);
    }

    public List<Direction> connectingDirections() {
        return connectingDirections;
    }

    public Optional<Direction> ascendingDirection() {
        return Optional.ofNullable(ascendingDirection);
    }

    public enum State {
        STRAIGHT, ASCENDING, CURVED
    }

    public boolean isStraight() {
        return state == State.STRAIGHT;
    }

    public boolean isAscending() {
        return state == State.ASCENDING;
    }

    public boolean isCurved() {
        return state == State.CURVED;
    }

    public static RailDirection[] simpleValues() {
        return new RailDirection[]{NORTH_SOUTH, EAST_WEST, ASCENDING_EAST, ASCENDING_WEST, ASCENDING_NORTH, ASCENDING_SOUTH};
    }
}
