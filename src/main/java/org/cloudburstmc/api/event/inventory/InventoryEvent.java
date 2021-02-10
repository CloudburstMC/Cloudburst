package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class InventoryEvent extends Event {

    protected final Inventory inventory;

    public InventoryEvent(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player[] getViewers() {
        return this.inventory.getViewers().toArray(new Player[0]);
    }

}
