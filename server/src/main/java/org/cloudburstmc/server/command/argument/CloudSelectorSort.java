package org.cloudburstmc.server.command.argument;

/**
 * Describes how selector matches are ordered before the result limit is applied.
 */
public enum CloudSelectorSort {
    /**
     * Keeps the natural iteration order of the source collection.
     */
    ARBITRARY,
    /**
     * Orders matches from nearest to farthest.
     */
    NEAREST,
    /**
     * Orders matches from farthest to nearest.
     */
    FURTHEST,
    /**
     * Randomizes match order.
     */
    RANDOM
}
