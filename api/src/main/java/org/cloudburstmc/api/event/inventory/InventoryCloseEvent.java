package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.inventory.Inventory;

/**
 * author: Box
 * Nukkit Project
 */
public class InventoryCloseEvent extends InventoryEvent {

    private final Player who;

    public InventoryCloseEvent(Inventory inventory, Player who) {
        super(inventory);
        this.who = who;
    }

    public Player getPlayer() {
        return this.who;
    }
}
