package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.inventory.Inventory;

/**
 * author: Box
 * Nukkit Project
 */
public class InventoryOpenEvent extends InventoryEvent implements Cancellable {

    private final Player who;

    public InventoryOpenEvent(Inventory inventory, Player who) {
        super(inventory);
        this.who = who;
    }

    public Player getPlayer() {
        return this.who;
    }
}