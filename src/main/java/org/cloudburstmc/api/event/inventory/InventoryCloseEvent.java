package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.player.Player;

/**
 * author: Box
 * Nukkit Project
 */
public final class InventoryCloseEvent extends InventoryEvent {

    private final Player who;

    public InventoryCloseEvent(Inventory inventory, Player who) {
        super(inventory);
        this.who = who;
    }

    public Player getPlayer() {
        return this.who;
    }
}
