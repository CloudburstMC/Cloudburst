package org.cloudburstmc.api.map;

/**
 * Cursor appearances available on maps.
 */
public enum MapCursorType {
    /**
     * A player within the mapped area.
     */
    PLAYER,
    /**
     * A displayed map in an item frame.
     */
    FRAME,
    /**
     * A player outside the mapped area.
     */
    PLAYER_OFF_MAP,
    /**
     * A distant player outside the tracking range.
     */
    PLAYER_OFF_LIMITS
}
