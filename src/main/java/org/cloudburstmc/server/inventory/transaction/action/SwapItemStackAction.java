package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.player.CloudPlayer;

public class SwapItemStackAction extends ItemStackAction {

    public SwapItemStackAction(int reqId, StackRequestSlotInfoData source, StackRequestSlotInfoData target) {
        super(reqId, source, target);
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        BaseInventory targetInv = player.getInventoryManager().getInventoryByType(getTargetData().getContainer());

        return inv.getItem(getSourceSlot()).equals(getSourceItem(), true, true) &&
                targetInv.getItem(getTargetSlot()).equals(getTargetItem(), true, true);
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        BaseInventory targetInv = player.getInventoryManager().getInventoryByType(getTargetData().getContainer());

        if (!targetInv.setItem(getTargetSlot(), sourceItem, false)) {
            return false;
        }

        if (!inv.setItem(getSourceSlot(), targetItem, false)) {
            // attempt to revert prior change
            targetInv.setItem(getTargetSlot(), targetItem, false);
            return false;
        }
        return true;
    }
}
