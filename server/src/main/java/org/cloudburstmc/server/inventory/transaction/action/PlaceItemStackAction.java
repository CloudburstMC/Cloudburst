package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

public class PlaceItemStackAction extends ItemStackAction {
    private int count;

    public PlaceItemStackAction(int id, int count, StackRequestSlotInfoData source, StackRequestSlotInfoData target) {
        super(id, source, target);
        this.count = count;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        return inv.getItem(getSourceSlot()).equals(getSourceItem(), false, true) &&
                inv.getItem(getSourceSlot()).getAmount() >= this.count;
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = player.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        BaseInventory targetInv = player.getInventoryManager().getInventoryByType(getTargetData().getContainer());

        CloudItemStack original = inv.getItem(getSourceSlot());
        CloudItemStack target = targetInv.getItem(getTargetSlot());
        CloudItemStack place;
        if (!target.isNull() && target.getAmount() + count > target.getType().getMaximumStackSize()) {
            int diff = target.getType().getMaximumStackSize() - target.getAmount();
            place = (CloudItemStack) target.withAmount(target.getType().getMaximumStackSize());
            count -= diff;
        } else if (target.isNull() && original.getAmount() == count) {
            place = original;
            count = 0;
        } else {
            place = (CloudItemStack) original.withAmount(count);
            count = original.getAmount() - count;
        }

        if (!targetInv.setItem(getTargetSlot(), place)) {
            return false;
        }

        if (count > 0) {
            if (!inv.setItem(getSourceSlot(), original.withAmount(count))) {
                targetInv.setItem(getTargetSlot(), target, false);
                return false;
            }
        } else {
            if (!inv.clear(getSourceSlot())) {
                targetInv.setItem(getTargetSlot(), target, false);
                return false;
            }
        }
        return true;
    }

}
