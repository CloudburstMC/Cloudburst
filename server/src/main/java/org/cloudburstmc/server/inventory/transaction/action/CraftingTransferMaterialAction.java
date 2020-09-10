package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.server.inventory.transaction.CraftingTransaction;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

/**
 * @author CreeperFace
 */
public class CraftingTransferMaterialAction extends InventoryAction {

    private int slot;

    public CraftingTransferMaterialAction(ItemStack sourceItem, ItemStack targetItem, int slot) {
        super(sourceItem, targetItem);

        this.slot = slot;
    }

    @Override
    public void onAddToTransaction(InventoryTransaction transaction) {
        if (transaction instanceof CraftingTransaction) {
            if (this.sourceItem.isNull()) {
                ((CraftingTransaction) transaction).setInput(this.slot, this.targetItem);
            } else if (this.targetItem.isNull()) {
                ((CraftingTransaction) transaction).setExtraOutput(this.slot, this.sourceItem);
            } else {
                throw new RuntimeException("Invalid " + getClass().getName() + ", either source or target item must be air, got source: " + this.sourceItem + ", target: " + this.targetItem);
            }
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
