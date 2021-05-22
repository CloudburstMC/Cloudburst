package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

public class SwapItemStackAction extends ItemStackAction {

    public SwapItemStackAction(int reqId, ItemStack sourceItem, int sourceSlot, ItemStack targetItem, int targetSlot) {
        super(reqId, sourceItem, sourceSlot, targetItem, targetSlot);
    }

    @Override
    public boolean isValid(CloudPlayer source) {
        return true;
    }

    @Override
    public boolean execute(CloudPlayer source) {
        return false;
    }

    @Override
    public void onExecuteSuccess(CloudPlayer source) {

    }

    @Override
    public void onExecuteFail(CloudPlayer source) {

    }
}
