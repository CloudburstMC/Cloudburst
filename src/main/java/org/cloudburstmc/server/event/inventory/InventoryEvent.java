package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.inventory.Inventory;
import org.cloudburstmc.server.player.Player;

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
