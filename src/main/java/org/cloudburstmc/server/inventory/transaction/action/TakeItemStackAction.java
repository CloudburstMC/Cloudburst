package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.List;

public class TakeItemStackAction extends ItemStackAction {
    private final StackRequestSlotInfoData sourceData, targetData;
    private final int count;

    public TakeItemStackAction(int id, int count, ItemStack sourceItem, StackRequestSlotInfoData source, ItemStack targetItem, StackRequestSlotInfoData target) {
        super(id, sourceItem, source.getSlot(), targetItem, target.getSlot());
        this.sourceData = source;
        this.targetData = target;
        this.count = count;
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
        CloudItemStack old = targetInv.getItem(getTargetSlot());
        CloudItemStack take;
        if (original.getAmount() > count) {
            take = (CloudItemStack) original.withAmount(count);
            original = (CloudItemStack) original.decrementAmount(count);
        } else {
            take = original;
            original = CloudItemRegistry.get().AIR;
        }
        if (!targetInv.setItem(getTargetSlot(), take)) {
            return false;
        }

        if (!inv.setItem(getSourceSlot(), original)) {
            // Revert previous one
            targetInv.setItem(getTargetSlot(), old);
            return false;
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
