package org.cloudburstmc.api.event.inventory;

/**
 * The reason the inventory was closed.
 */
public enum InventoryCloseReason {
    /**
     * Reason is not known.
     */
    UNKNOWN,
    /**
     * The player closed the inventory themselves.
     */
    PLAYER,
    /**
     * A plugin closed the inventory.
     */
    PLUGIN,
    /**
     * The player disconnected.
     */
    DISCONNECT,
    /**
     * The player was teleported.
     */
    TELEPORT,
    /**
     * The player died.
     */
    DEATH,
    /**
     * A new inventory was opened while this one was already open,
     * causing this one to be closed first.
     */
    OPEN_NEW,
    /**
     * The player can no longer use this inventory
     * (e.g. moved too far away from the block, or the block was removed).
     */
    CANT_USE,
    /**
     * The chunk or world containing the inventory was unloaded while the
     * player still had the screen open.
     */
    UNLOADED
}
