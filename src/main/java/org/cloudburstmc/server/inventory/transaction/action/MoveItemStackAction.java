package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class MoveItemStackAction extends ItemStackAction {
    private final int count;

    public MoveItemStackAction(int id, int count, StackRequestSlotInfoData source, StackRequestSlotInfoData target) {
        super(id, source, target);
        this.count = count;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);

        if (player.isCreative() && getSourceData().getContainer() == ContainerSlotType.CREATIVE_OUTPUT) {
            return true;
        }

        BaseInventory targetInv = getTargetInventory(player);
        return inv.getItem(getSourceSlot()).equals(getSourceItem(), false, true) &&
                inv.getItem(getSourceSlot()).getAmount() >= this.count
                && (targetInv.getItem(getTargetSlot()).getType() == BlockTypes.AIR ||
                targetInv.getItem(getTargetSlot()).getType() == inv.getItem(getSourceSlot()).getType());
    }

    @Override
    public boolean execute(CloudPlayer player) {
        BaseInventory inv = getSourceInventory(player);
        BaseInventory targetInv = getTargetInventory(player);

        CloudItemStack original = inv.getItem(getSourceSlot());
        CloudItemStack old = targetInv.getItem(getTargetSlot());
        CloudItemStack take, place;

        if (original.getAmount() > count) {
            take = (CloudItemStack) original.withAmount(count);
            original = (CloudItemStack) original.decrementAmount(count);
        } else if (player.isCreative() && getSourceData().getContainer() == ContainerSlotType.CREATIVE_OUTPUT) {
            take = (CloudItemStack) original.withAmount(count);
            original = CloudItemRegistry.get().AIR;
        } else {
            take = original;
            original = CloudItemRegistry.get().AIR;
        }

        if (old.getType() != BlockTypes.AIR) {
            if (old.getAmount() + take.getAmount() <= take.getBehavior().getMaxStackSize(take)) {
                place = (CloudItemStack) take.withAmount(take.getAmount() + count);
            } else {
                place = (CloudItemStack) take.withAmount(take.getBehavior().getMaxStackSize(take));
            }
        } else {
            place = take;
        }

        if (!targetInv.setItem(getTargetSlot(), place, false)) {
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

    @Override
    protected List<ItemStackResponsePacket.ContainerEntry> getContainers(CloudPlayer player) {
        List<ItemStackResponsePacket.ContainerEntry> containers = new ArrayList<>();
        if (getSourceData().getContainer() != ContainerSlotType.CREATIVE_OUTPUT) {
            containers.add(new ItemStackResponsePacket.ContainerEntry(getSourceData().getContainer(), List.of(NetworkUtils.itemStackToNetwork(getSourceData(), getSourceInventory(player)))));
        }
        containers.add(new ItemStackResponsePacket.ContainerEntry(getTargetData().getContainer(), List.of(NetworkUtils.itemStackToNetwork(getTargetData(), getTargetInventory(player)))));
        return containers;
    }
}
