package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class TakeItemStackAction extends ItemStackAction {
    private final int count;

    public TakeItemStackAction(int id, int count, StackRequestSlotInfoData source, StackRequestSlotInfoData target) {
        super(id, source, target);
        this.count = count;
    }

    @Override
    public boolean isValid(CloudPlayer source) {
        BaseInventory inv = source.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        if (source.isCreative() && getSourceData().getContainer() == ContainerSlotType.CREATIVE_OUTPUT) {
            return true;
        }
        return inv.getItem(getSourceSlot()).equals(getSourceItem(), false, true) &&
                inv.getItem(getSourceSlot()).getAmount() >= this.count;
    }

    @Override
    public boolean execute(CloudPlayer source) {
        BaseInventory inv = source.getInventoryManager().getInventoryByType(getSourceData().getContainer());
        BaseInventory targetInv = source.getInventoryManager().getInventoryByType(getTargetData().getContainer());

        CloudItemStack original = inv.getItem(getSourceSlot());
        CloudItemStack old = targetInv.getItem(getTargetSlot());
        CloudItemStack take;

        if (original.getAmount() > count) {
            take = (CloudItemStack) original.withAmount(count);
            original = (CloudItemStack) original.decrementAmount(count);
        } else if (source.isCreative() && getSourceData().getContainer() == ContainerSlotType.CREATIVE_OUTPUT) {
            take = (CloudItemStack) original.withAmount(count);
            original = CloudItemRegistry.get().AIR;
        } else {
            take = original;
            original = CloudItemRegistry.get().AIR;
        }
        if (!targetInv.setItem(getTargetSlot(), take, false)) {
            return false;
        }

        if (!inv.setItem(getSourceSlot(), original, false)) {
            // Revert previous one
            targetInv.setItem(getTargetSlot(), old, false);
            return false;
        }
        return true;
    }

    @Override
    public CloudItemStack getSourceItem() {
        if (getRequestId() == getSourceData().getStackNetworkId()) {
            //Unique situation when client doesn't know the Stack Net ID of the crafted item, so it sends the same as the item stack request id
            return getTransaction().getSource().getCraftingInventory().getCraftingResult();
        }
        return super.getSourceItem();
    }
}
