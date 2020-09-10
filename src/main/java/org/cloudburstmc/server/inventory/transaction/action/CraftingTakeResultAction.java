package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.server.inventory.transaction.CraftingTransaction;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

/**
 * @author CreeperFace
 */
public class CraftingTakeResultAction extends InventoryAction {

    public CraftingTakeResultAction(ItemStack sourceItem, ItemStack targetItem) {
        super(sourceItem, targetItem);
    }

    public void onAddToTransaction(InventoryTransaction transaction) {
        if (transaction instanceof CraftingTransaction) {
            ((CraftingTransaction) transaction).setPrimaryOutput(this.getSourceItem());
        } else {
            throw new RuntimeException(getClass().getName() + " can only be added to CraftingTransactions");
        }
    }

    @Override
    public boolean isValid(Player source) {
        return true;
    }

    @Override
    public boolean execute(Player source) {
        return true;
    }

    @Override
    public void onExecuteSuccess(Player $source) {

    }

    @Override
    public void onExecuteFail(Player source) {

    }
}
