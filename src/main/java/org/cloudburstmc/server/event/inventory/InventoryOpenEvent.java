package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.inventory.Inventory;
import org.cloudburstmc.server.player.Player;

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