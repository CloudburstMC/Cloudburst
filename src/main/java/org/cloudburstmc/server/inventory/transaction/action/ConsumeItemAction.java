package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

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
                && inv.getItem(getSourceSlot()).getAmount() >= count;
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);
        CloudItemStack item = inv.getItem(getSourceSlot());

        CloudItemStack replace = CloudItemRegistry.get().AIR;

        if (item.getAmount() > count) {
            replace = (CloudItemStack) item.withAmount(item.getAmount() - count);
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
