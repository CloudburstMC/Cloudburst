package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.math.GenericMath;
import org.cloudburstmc.server.inventory.CloudCraftingGrid;
import org.cloudburstmc.server.inventory.transaction.CraftItemStackTransaction;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

public class CraftResultsAction extends ItemStackAction {
    private final int timesCrafted;

    public CraftResultsAction(int reqId, int timesCrafted) {
        super(reqId, null, null);
        this.timesCrafted = timesCrafted;
    }

    @Override
    public boolean isValid(CloudPlayer source) {
        return timesCrafted > 0;
    }

    @Override
    public boolean execute(CloudPlayer source) {
        return true;
    }

    @Override
    public void onAddToTransaction(InventoryTransaction transaction) {
        super.onAddToTransaction(transaction);
        CloudItemStack item = ((CraftItemStackTransaction) transaction).getPrimaryOutput();
        transaction.getSource().getInventoryManager().getCraftingGrid().setItem(CloudCraftingGrid.CRAFTING_RESULT_SLOT, item.withAmount(GenericMath.clamp(item.getAmount() * timesCrafted, 1, item.getBehavior().getMaxStackSize(item))), false);
    }

    @Override
    public void onExecuteSuccess(CloudPlayer source) {
        // no-op
    }
}
