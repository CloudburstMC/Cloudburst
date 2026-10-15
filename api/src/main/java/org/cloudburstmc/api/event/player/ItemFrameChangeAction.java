package org.cloudburstmc.api.event.player;

/**
 * Changes a player can make to a frame's displayed item.
 */
public enum ItemFrameChangeAction {
    /**
     * Inserts an item into an empty frame.
     */
    PLACE,
    /**
     * Removes the displayed item without breaking the frame.
     */
    REMOVE,
    /**
     * Rotates the displayed item by one eighth of a turn.
     */
    ROTATE
}
