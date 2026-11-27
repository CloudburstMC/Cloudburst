package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.inventory.InventoryScreen;

/**
 * Called when a player closes an inventory screen.
 *
 * <p>The screen has already closed. This event cannot be canceled.</p>
 */
public class InventoryCloseEvent extends InventoryEvent {

    private final InventoryCloseReason reason;

    public InventoryCloseEvent(InventoryScreen screen) {
        this(screen, InventoryCloseReason.UNKNOWN);
    }

    public InventoryCloseEvent(InventoryScreen screen, InventoryCloseReason reason) {
        super(screen);
        this.reason = reason;
    }

    /**
     * Returns the reason this inventory was closed.
     *
     * @return the close reason
     */
    public InventoryCloseReason getReason() {
        return reason;
    }
}
