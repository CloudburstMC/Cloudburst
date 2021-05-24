package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class DropItemStackAction extends ItemStackAction {
    private int count;
    private final boolean randomly; // Unsure of actual usage atm. only seen false sent from client so far

    public DropItemStackAction(int id, int count, boolean random, StackRequestSlotInfoData source, StackRequestSlotInfoData target) {
        super(id, source, target);
        this.count = count;
        this.randomly = random;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        return inv.getItem(getSourceSlot()).equals(getSourceItem(), false, true) &&
                inv.getItem(getSourceSlot()).getAmount() >= count;
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        CloudItemStack drop;

        if (getSourceItem().getAmount() > count) {
            drop = (CloudItemStack) getSourceItem().withAmount(count);

            if (!inv.setItem(getSourceSlot(), getSourceItem().withAmount(getSourceItem().getAmount() - count), true)) {
                return false;
            }
        } else {
            drop = getSourceItem();
            if (!inv.setItem(getSourceSlot(), CloudItemRegistry.get().AIR, true)) {
                return false;
            }
        }

        player.getLevel().dropItem(player.getPosition(), drop);
        return true;
    }

}
