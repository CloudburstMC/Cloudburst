package org.cloudburstmc.server.inventory.transaction.action;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.server.inventory.CloudInventory;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.inventory.transaction.ItemStackTransaction;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

@Log4j2
@Getter
public abstract class ItemStackAction extends InventoryAction {

    private final ItemStackRequestSlotData sourceData, targetData;
    private final int requestId;
    private ItemStackTransaction transaction;

    public ItemStackAction(int reqId, @Nullable ItemStackRequestSlotData sourceData, @Nullable ItemStackRequestSlotData targetData) {
        super(sourceData != null ? ItemUtils.getFromNetworkId(sourceData.getStackNetworkId()).orElse(ItemStack.EMPTY) : ItemStack.EMPTY,
                targetData != null ? ItemUtils.getFromNetworkId(targetData.getStackNetworkId()).orElse(ItemStack.EMPTY) : ItemStack.EMPTY);
        this.requestId = reqId;
        this.sourceData = sourceData;
        this.targetData = targetData;
    }

    @Override
    public void onAddToTransaction(InventoryTransaction transaction) {
        this.transaction = (ItemStackTransaction) transaction;
        if (getSourceData() != null) {
            this.transaction.addInventory(transaction.getSource().getInventoryManager().getInventoryByType(getSourceData().getContainer()));
        }
        if (getTargetData() != null) {
            this.transaction.addInventory(transaction.getSource().getInventoryManager().getInventoryByType(getTargetData().getContainer()));
        }
    }

    @Override
    public void onExecuteSuccess(CloudPlayer source) {
        getTransaction().setResponseStatus(ItemStackResponseStatus.OK);
        this.getTransaction().addContainers(getContainers(source));
    }

    @Override
    public void onExecuteFail(CloudPlayer source) {
        log.debug("Failed on transaction action: {}", this.getClass().getSimpleName());
        getTransaction().setResponseStatus(ItemStackResponseStatus.ERROR);
        this.getTransaction().addContainers(getContainers(source));
    }

    protected int getSourceSlot() {
        return sourceData.getSlot();
    }

    protected int getTargetSlot() {
        return targetData.getSlot();
    }

    @Nullable
    protected CloudInventory getSourceInventory(CloudPlayer source) {
        if (sourceData != null) {
            return source.getInventoryManager().getInventoryByType(sourceData.getContainer());
        }
        return null;
    }

    @Nullable
    protected CloudInventory getTargetInventory(CloudPlayer source) {
        if (targetData != null) {
            return source.getInventoryManager().getInventoryByType(targetData.getContainer());
        }
        return null;
    }

    protected abstract List<ItemStackResponseContainer> getContainers(CloudPlayer source);
}
