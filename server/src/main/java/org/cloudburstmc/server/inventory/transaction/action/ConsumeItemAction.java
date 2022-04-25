package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class ConsumeItemAction extends ItemStackAction {
    private final int count;

    public ConsumeItemAction(int reqId, int count, @NonNull StackRequestSlotInfoData sourceData) {
        super(reqId, sourceData, null);
        this.count = count;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);
        return inv.getItem(getSourceSlot()).equals(getSourceItem(), false, true)
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
    protected List<ItemStackResponsePacket.ContainerEntry> getContainers(CloudPlayer player) {
        return List.of(new ItemStackResponsePacket.ContainerEntry(getSourceData().getContainer(),
                List.of(NetworkUtils.itemStackToNetwork(getSourceData(),
                        getSourceInventory(player)))));
    }

}
