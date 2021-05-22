package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class PlaceItemStackAction extends ItemStackAction {
    private final StackRequestSlotInfoData sourceData, targetData;
    private int count;

    public PlaceItemStackAction(int id, int count, ItemStack sourceItem, StackRequestSlotInfoData source, ItemStack targetItem, StackRequestSlotInfoData target) {
        super(id, sourceItem, source.getSlot(), targetItem, target.getSlot());
        this.count = count;
        this.targetData = target;
        this.sourceData = source;
    }

    @Override
    public boolean isValid(CloudPlayer source) {
        BaseInventory inv = getSource().getInventoryManager().getInventoryByType(sourceData.getContainer());
        return inv.getItem(getSourceSlot()).equals(getSourceItem(), false, true) &&
                inv.getItem(getSourceSlot()).getAmount() >= this.count;
    }

    @Override
    public boolean execute(CloudPlayer source) {
        BaseInventory inv = getSource().getInventoryManager().getInventoryByType(sourceData.getContainer());
        BaseInventory targetInv = getSource().getInventoryManager().getInventoryByType(targetData.getContainer());

        CloudItemStack original = inv.getItem(getSourceSlot());
        CloudItemStack target = targetInv.getItem(getTargetSlot());
        CloudItemStack place;
        if (!target.isNull() && target.getAmount() + count > target.getType().getMaximumStackSize()) {
            int diff = target.getType().getMaximumStackSize() - target.getAmount();
            place = (CloudItemStack) target.withAmount(target.getType().getMaximumStackSize());
            count -= diff;
        } else {
            place = original;
            count -= original.getAmount();
        }

        if (!targetInv.setItem(getTargetSlot(), place)) {
            return false;
        }

        if (count > 0) {
            if (!inv.setItem(getSourceSlot(), original.withAmount(count))) {
                targetInv.setItem(getTargetSlot(), target);
                return false;
            }
        } else {
            if (!inv.clear(getSourceSlot())) {
                targetInv.setItem(getTargetSlot(), target);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onExecuteSuccess(CloudPlayer source) {
        getTransaction().addResponse(new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.OK, getRequestId(),
                List.of(new ItemStackResponsePacket.ContainerEntry(sourceData.getContainer(), List.of(NetworkUtils.itemStackToNetwork(sourceData, source.getInventoryManager().getInventoryByType(sourceData.getContainer())))),
                        new ItemStackResponsePacket.ContainerEntry(targetData.getContainer(), List.of(NetworkUtils.itemStackToNetwork(targetData, source.getInventoryManager().getInventoryByType(targetData.getContainer()))))
                )));
    }

    @Override
    public void onExecuteFail(CloudPlayer source) {
        getTransaction().addResponse(new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.ERROR, getRequestId(),
                List.of(new ItemStackResponsePacket.ContainerEntry(sourceData.getContainer(), List.of(NetworkUtils.itemStackToNetwork(targetData, source.getInventoryManager().getInventoryByType(targetData.getContainer())))),
                        new ItemStackResponsePacket.ContainerEntry(targetData.getContainer(), List.of(NetworkUtils.itemStackToNetwork(targetData, source.getInventoryManager().getInventoryByType(targetData.getContainer()))))
                )));
    }
}
