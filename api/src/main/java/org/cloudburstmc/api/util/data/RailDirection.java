package org.cloudburstmc.api.util.data;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Direction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum RailDirection {
    NORTH_SOUTH(false, false, Direction.NORTH, Direction.SOUTH, null),
    EAST_WEST(false, false, Direction.EAST, Direction.WEST, null),
    ASCENDING_EAST(true, false, Direction.EAST, Direction.WEST, Direction.EAST),
    ASCENDING_WEST(true, false, Direction.EAST, Direction.WEST, Direction.WEST),
    ASCENDING_NORTH(true, false, Direction.NORTH, Direction.SOUTH, Direction.NORTH),
    ASCENDING_SOUTH(true, false, Direction.NORTH, Direction.SOUTH, Direction.SOUTH),
    SOUTH_EAST(false, true, Direction.SOUTH, Direction.EAST, null),
    SOUTH_WEST(false, true, Direction.SOUTH, Direction.WEST, null),
    NORTH_WEST(false, true, Direction.NORTH, Direction.WEST, null),
    NORTH_EAST(false, true, Direction.NORTH, Direction.EAST, null);

    private static final List<RailDirection> SIMPLE_VALUES = Collections.unmodifiableList(Arrays.asList(
            NORTH_SOUTH, EAST_WEST, ASCENDING_EAST, ASCENDING_WEST, ASCENDING_NORTH, ASCENDING_SOUTH
    ));

    private static final RailDirection[] CURVED_VALUES = {SOUTH_EAST, SOUTH_WEST, NORTH_WEST, NORTH_EAST};

    private final boolean ascending;
    private final boolean curved;
    private final List<Direction> connectingDirections;
    private final Direction ascendingDirection;

    RailDirection(boolean ascending, boolean curved, Direction from, Direction to, Direction ascendingDirection) {
        this.ascending = ascending;
        this.curved = curved;
        this.connectingDirections = Collections.unmodifiableList(Arrays.asList(from, to));
        this.ascendingDirection = ascendingDirection;
    }

    @NonNull
    public static RailDirection straight(Direction face) {
        return switch (face) {
            case NORTH, SOUTH -> NORTH_SOUTH;
            case EAST, WEST -> EAST_WEST;
            default ->
                    throw new IllegalArgumentException("Cannot make a straight rail for vertical direction: " + face);
        };
    }

    @NonNull
    public static RailDirection ascending(Direction face) {
        return switch (face) {
            case NORTH -> ASCENDING_NORTH;
            case SOUTH -> ASCENDING_SOUTH;
            case EAST -> ASCENDING_EAST;
            case WEST -> ASCENDING_WEST;
            default ->
                    throw new IllegalArgumentException("Cannot make an ascending rail for vertical direction: " + face);
        };
    }

    @NonNull
    public static RailDirection curved(Direction f1, Direction f2) {
        for (RailDirection o : CURVED_VALUES) {
            if (o.connectingDirections.contains(f1) && o.connectingDirections.contains(f2)) {
                return o;
            }
        }
        throw new IllegalArgumentException("No curved rail direction for " + f1 + " and " + f2);
    }

    public static List<RailDirection> simpleValues() {
        return SIMPLE_VALUES;
    }

    public List<Direction> connectingDirections() {
        return connectingDirections;
    }

    public @Nullable Direction ascendingDirection() {
        return ascendingDirection;
    }

    public boolean isStraight() {
        return !ascending && !curved;
    }

    public boolean isAscending() {
        return ascending;
    }

    public boolean isCurved() {
        return curved;
    }
}
