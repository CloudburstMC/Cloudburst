package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class ConsumeItemAction extends ItemStackAction {
    private final int count;

    public ConsumeItemAction(int reqId, int count, @NonNull StackRequestSlotInfoData sourceData) {
        super(reqId, sourceData, null);
        this.count = count;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        return inv.getItem(getSourceSlot()).equals(getSourceItem(), false, true)
                && inv.getItem(getSourceSlot()).getAmount() >= count;
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        CloudItemStack item = inv.getItem(getSourceSlot());

        CloudItemStack replace = CloudItemRegistry.get().AIR;

        if (item.getAmount() > count) {
            replace = (CloudItemStack) item.withAmount(item.getAmount() - count);
        }

        return inv.setItem(getSourceSlot(), replace, false);
    }
}
