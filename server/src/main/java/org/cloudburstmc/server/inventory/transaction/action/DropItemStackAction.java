package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class DropItemStackAction extends ItemStackAction {
    private int count;
    private final boolean randomly; // Unsure of actual usage atm. only seen false sent from client so far

    public DropItemStackAction(int id, int count, boolean random, ItemStackRequestSlotData source, ItemStackRequestSlotData target) {
        super(id, source, target);
        this.count = count;
        this.randomly = random;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);
        return inv.getItem(getSourceSlot()).isSimilarMetadata(getSourceItem()) &&
                inv.getItem(getSourceSlot()).getCount() >= count;
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);
        ItemStack drop;

        if (getSourceItem().getCount() > count) {
            drop = (ItemStack) getSourceItem().withCount(count);

            if (!inv.setItem(getSourceSlot(), getSourceItem().withCount(getSourceItem().getCount() - count), true)) {
                return false;
            }
        } else {
            drop = getSourceItem();
            if (!inv.setItem(getSourceSlot(), ItemStack.AIR, true)) {
                return false;
            }
        }

        player.getLevel().dropItem(player.getPosition(), drop);
        return true;
    }

    @Override
    protected List<ItemStackResponseContainer> getContainers(CloudPlayer player) {
        return List.of(new ItemStackResponseContainer(getSourceData().getContainer(),
                List.of(NetworkUtils.itemStackToNetwork(getSourceData(), getSourceInventory(player)))));
    }
}
