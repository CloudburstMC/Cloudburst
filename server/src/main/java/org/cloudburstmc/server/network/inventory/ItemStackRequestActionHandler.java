package org.cloudburstmc.server.network.inventory;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.event.inventory.InventoryClickEvent;
import org.cloudburstmc.api.inventory.view.SlotGroup;
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
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.*;

/**
 * Translates protocol {@code ItemStackRequestAction} objects into {@link org.cloudburstmc.api.event.inventory.InventoryClickEvent}s
 * and direct slot mutations. Tracks every slot affected during a single request and assembles the
 * {@code ItemStackResponse} that is sent back to the client.
 */
@Log4j2
public class ItemStackRequestActionHandler {

    private static final int CREATED_OUTPUT_PROTOCOL_SLOT = 50;

    private final CloudPlayer player;
    private final Map<ContainerSlotType, Set<Integer>> affectedSlots = new LinkedHashMap<>();
    private CloudInventoryScreen screen;
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

        if (sourceItem.isEmpty() || sourceItem.getCount() < count) {
            throw new IllegalArgumentException("Source item is empty or has insufficient count");
        }

        ItemStack newSource;
        if (sourceItem.getCount() == count) {
            newSource = ItemStack.EMPTY;
        } else {
            newSource = sourceItem.withCount(sourceItem.getCount() - count);
        }

        ItemStack newDest;
        if (destItem.isEmpty()) {
            newDest = sourceItem.withCount(count);
        } else {
            if (!destItem.isSimilarMetadata(sourceItem)) {
                throw new IllegalArgumentException("Cannot merge incompatible items in transfer");
            }
            newDest = destItem.withCount(destItem.getCount() + count);
        }

        SlotGroup srcGroup = screen.resolveSlotGroup(srcSlot.getContainerName().getContainer());
        int srcViewSlot = screen.resolveInventorySlot(srcSlot.getContainerName().getContainer(), srcSlot.getSlot());
        SlotGroup dstGroup = screen.resolveSlotGroup(dstSlot.getContainerName().getContainer());
        int dstViewSlot = screen.resolveInventorySlot(dstSlot.getContainerName().getContainer(), dstSlot.getSlot());

        InventoryClickEvent.ActionType actionType = switch (action.getType()) {
            case TAKE -> InventoryClickEvent.ActionType.TAKE;
            case PLACE -> InventoryClickEvent.ActionType.PLACE;
            default -> InventoryClickEvent.ActionType.UNKNOWN;
        };

        InventoryClickEvent.ClickType clickType;
        if (action.getType() == ItemStackRequestActionType.TAKE) {
            if (count == sourceItem.getCount()) {
                clickType = InventoryClickEvent.ClickType.TAKE_ALL;
            } else if (count == sourceItem.getCount() / 2) {
                clickType = InventoryClickEvent.ClickType.TAKE_HALF;
            } else {
                clickType = InventoryClickEvent.ClickType.TAKE_SOME;
            }
        } else if (action.getType() == ItemStackRequestActionType.PLACE) {
            if (count == sourceItem.getCount()) {
                clickType = InventoryClickEvent.ClickType.PLACE_ALL;
            } else if (count == 1) {
                clickType = InventoryClickEvent.ClickType.PLACE_ONE;
            } else {
                clickType = InventoryClickEvent.ClickType.PLACE_SOME;
            }
        } else {
            clickType = InventoryClickEvent.ClickType.UNKNOWN;
        }

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(srcViewSlot)
                .slotGroup(srcGroup)
                .sourceItem(sourceItem)
                .cursorItem(destItem)
                .actionType(actionType)
                .clickType(clickType)
                .destinationSlot(dstViewSlot)
                .destinationSlotGroup(dstGroup)
                .resultItem(newDest)
                .build();
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            requestFailed = true;
            return;
        }

        ItemStack resolvedDest = event.getResultItem() != null ? event.getResultItem() : newDest;

        setSlot(srcSlot, newSource);
        setSlot(dstSlot, resolvedDest);
    }

    private void handleSwap(SwapAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStackRequestSlotData dstSlot = action.getDestination();

        ItemStack sourceItem = getSlot(srcSlot);
        ItemStack destItem = getSlot(dstSlot);

        SlotGroup srcGroup = screen.resolveSlotGroup(srcSlot.getContainerName().getContainer());
        int srcViewSlot = screen.resolveInventorySlot(srcSlot.getContainerName().getContainer(), srcSlot.getSlot());
        SlotGroup dstGroup = screen.resolveSlotGroup(dstSlot.getContainerName().getContainer());
        int dstViewSlot = screen.resolveInventorySlot(dstSlot.getContainerName().getContainer(), dstSlot.getSlot());

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(srcViewSlot)
                .slotGroup(srcGroup)
                .sourceItem(sourceItem)
                .cursorItem(destItem)
                .actionType(InventoryClickEvent.ActionType.SWAP)
                .clickType(InventoryClickEvent.ClickType.UNKNOWN)
                .destinationSlot(dstViewSlot)
                .destinationSlotGroup(dstGroup)
                .resultItem(sourceItem)
                .build();
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            requestFailed = true;
            return;
        }

        ItemStack resolvedDest = event.getResultItem() != null ? event.getResultItem() : sourceItem;

        setSlot(srcSlot, destItem);
        setSlot(dstSlot, resolvedDest);
    }

    private void handleDrop(DropAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStack sourceItem = getSlot(srcSlot);
        int count = action.getCount();

        if (sourceItem.isEmpty() || sourceItem.getCount() < count) {
            throw new IllegalArgumentException("Source item is empty or has insufficient count");
        }

        ItemStack dropItem = sourceItem.withCount(count);

        ItemStack newSource;
        if (sourceItem.getCount() == count) {
            newSource = ItemStack.EMPTY;
        } else {
            newSource = sourceItem.withCount(sourceItem.getCount() - count);
        }

        SlotGroup srcGroup = screen.resolveSlotGroup(srcSlot.getContainerName().getContainer());
        int srcViewSlot = screen.resolveInventorySlot(srcSlot.getContainerName().getContainer(), srcSlot.getSlot());

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(srcViewSlot)
                .slotGroup(srcGroup)
                .sourceItem(sourceItem)
                .cursorItem(ItemStack.EMPTY)
                .actionType(InventoryClickEvent.ActionType.DROP)
                .clickType(InventoryClickEvent.ClickType.UNKNOWN)
                .build();
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            requestFailed = true;
            return;
        }

        setSlot(srcSlot, newSource);
        player.dropItem(dropItem);
    }

    private void handleDestroy(DestroyAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStack sourceItem = getSlot(srcSlot);
        int count = action.getCount();

        if (sourceItem.isEmpty()) {
            throw new IllegalArgumentException("Source item is empty");
        }

        SlotGroup srcGroup = screen.resolveSlotGroup(srcSlot.getContainerName().getContainer());
        int srcViewSlot = screen.resolveInventorySlot(srcSlot.getContainerName().getContainer(), srcSlot.getSlot());

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(srcViewSlot)
                .slotGroup(srcGroup)
                .sourceItem(sourceItem)
                .cursorItem(ItemStack.EMPTY)
                .actionType(InventoryClickEvent.ActionType.DESTROY)
                .clickType(InventoryClickEvent.ClickType.UNKNOWN)
                .build();
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            requestFailed = true;
            return;
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

        SlotGroup createdOutputGroup = screen.resolveSlotGroup(ContainerSlotType.CREATED_OUTPUT);
        int createdOutputViewSlot = screen.resolveInventorySlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT);

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(createdOutputViewSlot)
                .slotGroup(createdOutputGroup)
                .sourceItem(ItemStack.EMPTY)
                .cursorItem(creativeItem)
                .actionType(InventoryClickEvent.ActionType.CRAFT_CREATIVE)
                .clickType(InventoryClickEvent.ClickType.UNKNOWN)
                .build();
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            requestFailed = true;
            return;
        }

        this.screen.setSlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT, creativeItem);
        trackAffectedSlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT);
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

    public void beginRequest(ItemStackRequest request, CloudInventoryScreen screen) {
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
        if (item.isEmpty()) {
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
