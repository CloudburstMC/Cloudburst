package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class SwapItemStackAction extends ItemStackAction {

    public SwapItemStackAction(int reqId, ItemStackRequestSlotData source, ItemStackRequestSlotData target) {
        super(reqId, source, target);
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);
        BaseInventory targetInv = getTargetInventory(player);

        return inv.getItem(getSourceSlot()).isMergeable(getSourceItem()) &&
                targetInv.getItem(getTargetSlot()).isMergeable(getTargetItem());
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        BaseInventory targetInv = player.getInventoryManager().getInventoryByType(getTargetData().getContainer());

        if (!targetInv.setItem(getTargetSlot(), sourceItem, true)) {
            return false;
        }

        if (!inv.setItem(getSourceSlot(), targetItem, true)) {
            // attempt to revert prior change
            targetInv.setItem(getTargetSlot(), targetItem, true);
            return false;
        }
        return true;
    }

    @Override
    protected List<ItemStackResponseContainer> getContainers(CloudPlayer player) {
        return List.of(new ItemStackResponseContainer(getSourceData().getContainer(),
                        List.of(NetworkUtils.itemStackToNetwork(getSourceData(), getSourceInventory(player)))),
                new ItemStackResponseContainer(getTargetData().getContainer(),
                        List.of(NetworkUtils.itemStackToNetwork(getTargetData(), getTargetInventory(player)))));
    }
}
