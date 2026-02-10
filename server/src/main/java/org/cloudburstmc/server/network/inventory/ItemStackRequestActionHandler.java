package org.cloudburstmc.server.network.inventory;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.server.container.screen.CloudContainerScreen;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.*;

@Log4j2
public class ItemStackRequestActionHandler {

    private final CloudPlayer player;
    private final Map<ContainerSlotType, Set<Integer>> affectedSlots = new LinkedHashMap<>();
    private CloudContainerScreen screen;
    private ItemStackRequest currentRequest;
    private boolean requestFailed;

    public ItemStackRequestActionHandler(CloudPlayer player) {
        this.player = player;
    }

    public void handleAction(ItemStackRequestAction action) {
        if (requestFailed) {
            return;
        }

        try {
            switch (action.getType()) {
                case TAKE -> handleTransfer((TakeAction) action);
                case PLACE -> handleTransfer((PlaceAction) action);
                case SWAP -> handleSwap((SwapAction) action);
                case DROP -> handleDrop((DropAction) action);
                case DESTROY -> handleDestroy((DestroyAction) action);
                case CRAFT_CREATIVE -> handleCraftCreative((CraftCreativeAction) action);
                case CRAFT_RESULTS_DEPRECATED, CREATE, CONSUME -> {
                }
                default -> log.debug("Unhandled inventory action type: {}", action.getType());
            }
        } catch (Exception e) {
            log.warn("Failed to handle inventory action {} for {}: {}",
                    action.getType(), player.getName(), e.getMessage());
            requestFailed = true;
        }
    }

    private void handleTransfer(TransferItemStackRequestAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStackRequestSlotData dstSlot = action.getDestination();

        ItemStack sourceItem = getSlot(srcSlot);
        ItemStack destItem = getSlot(dstSlot);
        int count = action.getCount();

        if (sourceItem == ItemStack.EMPTY || sourceItem.getCount() < count) {
            throw new IllegalArgumentException("Source item is empty or has insufficient count");
        }

        ItemStack newSource;
        if (sourceItem.getCount() == count) {
            newSource = ItemStack.EMPTY;
        } else {
            newSource = sourceItem.withCount(sourceItem.getCount() - count);
        }

        ItemStack newDest;
        if (destItem == ItemStack.EMPTY) {
            newDest = sourceItem.withCount(count);
        } else {
            newDest = destItem.withCount(destItem.getCount() + count);
        }

        setSlot(srcSlot, newSource);
        setSlot(dstSlot, newDest);
    }

    private void handleSwap(SwapAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStackRequestSlotData dstSlot = action.getDestination();

        ItemStack sourceItem = getSlot(srcSlot);
        ItemStack destItem = getSlot(dstSlot);

        setSlot(srcSlot, destItem);
        setSlot(dstSlot, sourceItem);
    }

    private void handleDrop(DropAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStack sourceItem = getSlot(srcSlot);
        int count = action.getCount();

        if (sourceItem == ItemStack.EMPTY || sourceItem.getCount() < count) {
            throw new IllegalArgumentException("Source item is empty or has insufficient count");
        }

        ItemStack dropItem = sourceItem.withCount(count);

        ItemStack newSource;
        if (sourceItem.getCount() == count) {
            newSource = ItemStack.EMPTY;
        } else {
            newSource = sourceItem.withCount(sourceItem.getCount() - count);
        }

        setSlot(srcSlot, newSource);
        player.dropItem(dropItem);
    }

    private void handleDestroy(DestroyAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStack sourceItem = getSlot(srcSlot);
        int count = action.getCount();

        if (sourceItem == ItemStack.EMPTY) {
            throw new IllegalArgumentException("Source item is empty");
        }

        ItemStack newSource;
        if (sourceItem.getCount() <= count) {
            newSource = ItemStack.EMPTY;
        } else {
            newSource = sourceItem.withCount(sourceItem.getCount() - count);
        }

        setSlot(srcSlot, newSource);
    }

    private void handleCraftCreative(CraftCreativeAction action) {
        int creativeNetId = action.getCreativeItemNetworkId();
        ItemStack creativeItem = CloudItemRegistry.get().getCreativeItem(creativeNetId);

        if (creativeItem == null) {
            throw new IllegalArgumentException("Unknown creative item network ID: " + creativeNetId);
        }

        creativeItem = creativeItem.withCount(64); // TODO: Use actual creative item size

        this.screen.setSlot(ContainerSlotType.CREATED_OUTPUT, 50, creativeItem);
        trackAffectedSlot(ContainerSlotType.CREATED_OUTPUT, 50);
    }

    private ItemStack getSlot(ItemStackRequestSlotData slotData) {
        return this.screen.getSlot(slotData.getContainerName().getContainer(), slotData.getSlot());
    }

    private void setSlot(ItemStackRequestSlotData slotData, ItemStack item) {
        ContainerSlotType containerType = slotData.getContainerName().getContainer();
        this.screen.setSlot(containerType, slotData.getSlot(), item);
        trackAffectedSlot(containerType, slotData.getSlot());
    }

    private void trackAffectedSlot(ContainerSlotType containerType, int slot) {
        affectedSlots.computeIfAbsent(containerType, k -> new LinkedHashSet<>()).add(slot);
    }

    public void beginRequest(ItemStackRequest request, CloudContainerScreen screen) {
        this.screen = screen;
        this.currentRequest = request;
        this.requestFailed = false;
        this.affectedSlots.clear();
    }

    public ItemStackResponse endRequest() {
        int requestId = currentRequest.getRequestId();

        if (requestFailed) {
            this.screen = null;
            this.currentRequest = null;
            this.affectedSlots.clear();
            return new ItemStackResponse(ItemStackResponseStatus.ERROR, requestId, Collections.emptyList());
        }

        List<ItemStackResponseContainer> containers = new ArrayList<>();
        for (Map.Entry<ContainerSlotType, Set<Integer>> entry : affectedSlots.entrySet()) {
            ContainerSlotType containerType = entry.getKey();
            Set<Integer> slots = entry.getValue();

            List<ItemStackResponseSlot> responseSlots = new ArrayList<>();
            for (int slot : slots) {
                ItemStack item = this.screen.getSlot(containerType, slot);
                responseSlots.add(makeResponseSlot(slot, item));
            }

            containers.add(new ItemStackResponseContainer(
                    containerType,
                    responseSlots,
                    new FullContainerName(containerType, null)
            ));
        }

        this.screen = null;
        this.currentRequest = null;
        this.affectedSlots.clear();

        return new ItemStackResponse(ItemStackResponseStatus.OK, requestId, containers);
    }

    private ItemStackResponseSlot makeResponseSlot(int slot, ItemStack item) {
        if (item == ItemStack.EMPTY) {
            return new ItemStackResponseSlot(slot, slot, 0, 0, "", 0, "");
        }

        int netId = NetworkItemStack.getNetId(item);
        Integer damage = item.get(ItemKeys.DAMAGE);
        String customName = item.get(ItemKeys.CUSTOM_NAME);

        return new ItemStackResponseSlot(
                slot,
                slot,
                item.getCount(),
                netId,
                customName == null ? "" : customName,
                damage == null ? 0 : damage,
                ""
        );
    }

    public void addFilteredStrings(int requestId, String[] filterStrings) {
        // TODO: Implement text filtering
    }
}
