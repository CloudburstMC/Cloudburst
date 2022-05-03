package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.inventory.transaction.ItemStackTransaction;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.List;

@Log4j2
@Getter
public abstract class ItemStackAction extends InventoryAction {

    private final StackRequestSlotInfoData sourceData, targetData;
    private final int requestId;
    private ItemStackTransaction transaction;

    public ItemStackAction(int reqId, @Nullable StackRequestSlotInfoData sourceData, @Nullable StackRequestSlotInfoData targetData) {
        super(sourceData != null ? ItemUtils.getFromNetworkId(sourceData.getStackNetworkId()).orElse(ItemStack.AIR) : ItemStack.AIR,
                targetData != null ? ItemUtils.getFromNetworkId(targetData.getStackNetworkId()).orElse(ItemStack.AIR) : ItemStack.AIR);
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
        getTransaction().setResponseStatus(ItemStackResponsePacket.ResponseStatus.OK);
        this.getTransaction().addContaiers(getContainers(source));
    }

    @Override
    public void onExecuteFail(CloudPlayer source) {
        log.debug("Failed on transaction action: {}", this.getClass().getSimpleName());
        getTransaction().setResponseStatus(ItemStackResponsePacket.ResponseStatus.ERROR);
        this.getTransaction().addContaiers(getContainers(source));
    }

    protected int getSourceSlot() {
        return sourceData.getSlot();
    }

    protected int getTargetSlot() {
        return targetData.getSlot();
    }

    @Nullable
    protected BaseInventory getSourceInventory(CloudPlayer source) {
        if (sourceData != null) {
            return source.getInventoryManager().getInventoryByType(sourceData.getContainer());
        }
        return null;
    }

    @Nullable
    protected BaseInventory getTargetInventory(CloudPlayer source) {
        if (targetData != null) {
            return source.getInventoryManager().getInventoryByType(targetData.getContainer());
        }
        return null;
    }

    protected abstract List<ItemStackResponsePacket.ContainerEntry> getContainers(CloudPlayer source);
}
