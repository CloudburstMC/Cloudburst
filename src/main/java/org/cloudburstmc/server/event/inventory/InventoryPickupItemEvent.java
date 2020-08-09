package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.entity.misc.DroppedItem;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.inventory.Inventory;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class InventoryPickupItemEvent extends InventoryEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final DroppedItem item;

    public InventoryPickupItemEvent(Inventory inventory, DroppedItem item) {
        super(inventory);
        this.item = item;
    }

    public DroppedItem getItem() {
        return item;
    }
}