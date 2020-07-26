package org.cloudburstmc.server.utils.data;

public enum RailDirection {
    NORTH_SOUTH,
    EAST_WEST,
    ASCENDING_EAST,
    ASCENDING_WEST,
    ASCENDING_NORTH,
    ASCENDING_SOUTH,
    CURVED_SOUTH_EAST,
    CURVED_SOUTH_WEST,
    CURVED_NORTH_WEST,
    CURVED_NORTH_EAST;

    public static RailDirection[] simpleValues() {
        return new RailDirection[]{NORTH_SOUTH, EAST_WEST, ASCENDING_EAST, ASCENDING_WEST, ASCENDING_NORTH, ASCENDING_SOUTH};
    }
}
