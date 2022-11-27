package org.cloudburstmc.server.inventory.transaction.action;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class ConsumeItemAction extends ItemStackAction {
    private final int count;

    public ConsumeItemAction(int reqId, int count, @NonNull ItemStackRequestSlotData sourceData) {
        super(reqId, sourceData, null);
        this.count = count;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);
        return inv.getItem(getSourceSlot()).isSimilarMetadata(getSourceItem())
                && inv.getItem(getSourceSlot()).getCount() >= count;
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);
        ItemStack item = inv.getItem(getSourceSlot());

        ItemStack replace = ItemStack.AIR;

        if (item.getCount() > count) {
            replace = (ItemStack) item.withCount(item.getCount() - count);
        }

        return inv.setItem(getSourceSlot(), replace, false);
    }

    @Override
    protected List<ItemStackResponseContainer> getContainers(CloudPlayer player) {
        return List.of(new ItemStackResponseContainer(getSourceData().getContainer(),
                List.of(NetworkUtils.itemStackToNetwork(getSourceData(),
                        getSourceInventory(player)))));
    }

}
