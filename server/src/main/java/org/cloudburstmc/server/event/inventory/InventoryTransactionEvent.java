package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class InventoryTransactionEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final InventoryTransaction transaction;

    public InventoryTransactionEvent(InventoryTransaction transaction) {
        this.transaction = transaction;
    }

    public InventoryTransaction getTransaction() {
        return transaction;
    }
}