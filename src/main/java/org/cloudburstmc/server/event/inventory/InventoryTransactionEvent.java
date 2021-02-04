package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class InventoryTransactionEvent extends Event implements Cancellable {

    private final InventoryTransaction transaction;

    public InventoryTransactionEvent(InventoryTransaction transaction) {
        this.transaction = transaction;
    }

    public InventoryTransaction getTransaction() {
        return transaction;
    }
}