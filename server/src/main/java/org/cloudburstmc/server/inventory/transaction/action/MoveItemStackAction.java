package org.cloudburstmc.server.inventory.transaction.action;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.server.inventory.CloudInventory;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.List;

import static org.cloudburstmc.api.item.ItemBehaviors.GET_MAX_STACK_SIZE;

@Log4j2
public class MoveItemStackAction extends ItemStackAction {

    @Inject
    ItemRegistry itemRegistry;

    private final int count;

    public MoveItemStackAction(int id, int count, ItemStackRequestSlotData source, ItemStackRequestSlotData target) {
        super(id, source, target);
        this.count = count;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        CloudInventory inv = getSourceInventory(player);

        if (player.isCreative() && getSourceData().getContainer() == ContainerSlotType.CREATED_OUTPUT) {
            return true;
        }

        CloudInventory targetInv = getTargetInventory(player);
        return inv.getItem(getSourceSlot()).isSimilarMetadata(getSourceItem()) &&
                inv.getItem(getSourceSlot()).getCount() >= this.count
                && (targetInv.getItem(getTargetSlot()).getType() == BlockTypes.AIR ||
                targetInv.getItem(getTargetSlot()).getType() == inv.getItem(getSourceSlot()).getType());
    }

    @Override
    public boolean execute(CloudPlayer player) {
        CloudInventory inv = getSourceInventory(player);
        CloudInventory targetInv = getTargetInventory(player);

        ItemStack original = inv.getItem(getSourceSlot());
        ItemStack old = targetInv.getItem(getTargetSlot());
        ItemStack take, place;

        if (original.getCount() > count) {
            take = original.withCount(count);
            original = original.withCount(original.getCount() - count);
        } else if (player.isCreative() && getSourceData().getContainer() == ContainerSlotType.CREATED_OUTPUT) {
            take = original.withCount(count);
            original = ItemStack.EMPTY;
        } else {
            take = original;
            original = ItemStack.EMPTY;
        }

        if (old.getType() != BlockTypes.AIR) {
            int maxStackSize = this.itemRegistry.getBehavior(take.getType(), GET_MAX_STACK_SIZE).execute();
            if (old.getCount() + take.getCount() <= maxStackSize) {
                place = take.withCount(take.getCount() + count);
            } else {
                place = take.withCount(maxStackSize);
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
    public ItemStack getSourceItem() {
        if (getRequestId() == getSourceData().getStackNetworkId()) {
            //Unique situation when client doesn't know the Stack Net ID of the crafted item, so it sends the same as the item stack request id
            return getTransaction().getSource().getCraftingInventory().getCraftingResult();
        }
        return super.getSourceItem();
    }

    @Override
    protected List<ItemStackResponseContainer> getContainers(CloudPlayer player) {
        List<ItemStackResponseContainer> containers = new ArrayList<>();
        if (getSourceData().getContainer() != ContainerSlotType.CREATED_OUTPUT) {
            containers.add(new ItemStackResponseContainer(getSourceData().getContainer(), List.of(NetworkUtils.itemStackToNetwork(getSourceData(), getSourceInventory(player)))));
        }
        containers.add(new ItemStackResponseContainer(getTargetData().getContainer(), List.of(NetworkUtils.itemStackToNetwork(getTargetData(), getTargetInventory(player)))));
        return containers;
    }
}
