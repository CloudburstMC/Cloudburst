package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.inventory.InventoryScreen;

/**
 * Called when a player closes an inventory screen.
 *
 * <p>The screen has already closed. This event cannot be canceled.</p>
 */
public class InventoryCloseEvent extends InventoryEvent {

    private final Reason reason;

    public InventoryCloseEvent(InventoryScreen screen) {
        this(screen, Reason.UNKNOWN);
    }

    public InventoryCloseEvent(InventoryScreen screen, Reason reason) {
        super(screen);
        this.reason = reason;
    }

    /**
     * Returns the reason this inventory was closed.
     *
     * @return the close reason
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * The reason the inventory was closed.
     */
    public enum Reason {
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
}
