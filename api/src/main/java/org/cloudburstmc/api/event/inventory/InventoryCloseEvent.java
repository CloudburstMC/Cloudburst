package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.inventory.InventoryScreen;

/**
 * Called when a player closes an inventory screen.
 *
 * <p>This event is <strong>not {@link org.cloudburstmc.api.event.Cancellable}</strong>.
 * The client initiates inventory closes unilaterally; it has already dismissed the UI before
 * the server receives the notification.
 * Cancelling the close server-side would cause a permanent desync: the client would show the
 * game world while the server still considered the screen open, breaking all subsequent
 * inventory interaction. If you need to prevent a player from leaving an inventory, you must
 * reopen it in a listener for this event instead of trying to cancel the close.</p>
 */
public final class InventoryCloseEvent extends InventoryEvent {

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
