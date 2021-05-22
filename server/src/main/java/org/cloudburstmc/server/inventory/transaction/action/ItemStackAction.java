package org.cloudburstmc.server.inventory.transaction.action;

import lombok.Getter;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.inventory.transaction.ItemStackTransaction;

public abstract class ItemStackAction extends InventoryAction {
    @Getter
    private final int sourceSlot;
    @Getter
    private final int targetSlot;
    private final int requestId;
    private ItemStackTransaction transaction;

    public ItemStackAction(int reqId, ItemStack sourceItem, int sourceSlot, ItemStack targetItem, int targetSlot) {
        super(sourceItem, targetItem);
        this.sourceSlot = sourceSlot;
        this.targetSlot = targetSlot;
        this.requestId = reqId;
    }

    @Override
    public void onAddToTransaction(InventoryTransaction transaction) {
        this.transaction = (ItemStackTransaction) transaction;
    }
}
