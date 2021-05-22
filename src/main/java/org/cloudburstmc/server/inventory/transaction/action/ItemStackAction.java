package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.inventory.transaction.ItemStackTransaction;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class ItemStackAction extends InventoryAction {

    private final StackRequestSlotInfoData sourceData, targetData;
    private final int requestId;
    private ItemStackTransaction transaction;

    public ItemStackAction(int reqId, StackRequestSlotInfoData sourceData, @Nullable StackRequestSlotInfoData targetData) {
        super(CloudItemRegistry.get().getItemByNetId(sourceData.getStackNetworkId()), targetData != null ? CloudItemRegistry.get().getItemByNetId(targetData.getStackNetworkId()) : CloudItemRegistry.get().AIR);
        this.requestId = reqId;
        this.sourceData = sourceData;
        this.targetData = targetData;
    }

    @Override
    public void onAddToTransaction(InventoryTransaction transaction) {
        this.transaction = (ItemStackTransaction) transaction;
        this.transaction.addInventory(transaction.getSource().getInventoryManager().getInventoryByType(getSourceData().getContainer()));
        if (getTargetData() != null) {
            this.transaction.addInventory(transaction.getSource().getInventoryManager().getInventoryByType(getTargetData().getContainer()));
        }
    }

    @Override
    public void onExecuteSuccess(CloudPlayer source) {
        List<ItemStackResponsePacket.ContainerEntry> containers = new ArrayList<>();
        containers.add(new ItemStackResponsePacket.ContainerEntry(sourceData.getContainer(), List.of(NetworkUtils.itemStackToNetwork(sourceData, source.getInventoryManager().getInventoryByType(sourceData.getContainer())))));
        if (targetData != null) {
            containers.add(new ItemStackResponsePacket.ContainerEntry(targetData.getContainer(), List.of(NetworkUtils.itemStackToNetwork(targetData, source.getInventoryManager().getInventoryByType(targetData.getContainer())))));
        }
        getTransaction().addResponse(new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.ERROR, getRequestId(), containers));
    }

    @Override
    public void onExecuteFail(CloudPlayer source) {
        List<ItemStackResponsePacket.ContainerEntry> containers = new ArrayList<>();
        containers.add(new ItemStackResponsePacket.ContainerEntry(sourceData.getContainer(), List.of(NetworkUtils.itemStackToNetwork(sourceData, source.getInventoryManager().getInventoryByType(sourceData.getContainer())))));
        if (targetData != null) {
            containers.add(new ItemStackResponsePacket.ContainerEntry(targetData.getContainer(), List.of(NetworkUtils.itemStackToNetwork(targetData, source.getInventoryManager().getInventoryByType(targetData.getContainer())))));
        }
        getTransaction().addResponse(new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.ERROR, getRequestId(), containers));
    }

    public int getSourceSlot() {
        return sourceData.getSlot();
    }

    public int getTargetSlot() {
        return targetData.getSlot();
    }

    @Override
    public CloudItemStack getSourceItem() {
        return (CloudItemStack) super.getSourceItem();
    }

    @Override
    public CloudItemStack getTargetItem() {
        return (CloudItemStack) super.getTargetItem();
    }
}
