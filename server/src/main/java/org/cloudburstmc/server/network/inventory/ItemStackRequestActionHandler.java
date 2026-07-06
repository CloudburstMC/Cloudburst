package org.cloudburstmc.server.network.inventory;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.crafting.CraftingRecipe;
import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.event.inventory.FurnaceExtractEvent;
import org.cloudburstmc.api.event.inventory.InventoryClickEvent;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.LegacySetItemSlotData;
import org.cloudburstmc.server.container.screen.CloudBlockContainerScreen;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.*;

/**
 * Translates protocol {@code ItemStackRequestAction} objects into {@link org.cloudburstmc.api.event.inventory.InventoryClickEvent}s
 * and direct slot mutations. Tracks every slot affected during a single request and assembles the
 * {@code ItemStackResponse} that is sent back to the client.
 */
@Log4j2
public class ItemStackRequestActionHandler {

    private static final int CREATED_OUTPUT_PROTOCOL_SLOT = 50;
    private static final int LEGACY_ARMOR_CONTAINER_ID = 6;
    private static final int BEDROCK_HOTBAR_START_SLOT = 36;
    private static final int BEDROCK_HOTBAR_END_SLOT = 44;
    private static final int HOTBAR_SIZE = 9;

    private final CloudPlayer player;
    private final Map<ContainerSlotType, Set<Integer>> affectedSlots = new LinkedHashMap<>();
    private CloudInventoryScreen screen;
    private ItemStackRequest currentRequest;
    private boolean requestFailed;

    public ItemStackRequestActionHandler(CloudPlayer player) {
        this.player = player;
    }

    public static ContainerSlotType container(ItemStackRequestSlotData slotData) {
        FullContainerName containerName = slotData.getContainerName();
        if (containerName == null) {
            throw new IllegalArgumentException("Item stack request slot is missing containerName");
        }
        return containerName.getContainer();
    }

    public void handleAction(ItemStackRequestAction action) {
        if (requestFailed) {
            return;
        }

        try {
            dispatchAction(action);
        } catch (Exception e) {
            log.warn("Failed to handle inventory action {} for {}: {}",
                    action.getType(), player.getName(), e.getMessage());
            requestFailed = true;
        }
    }

    private void dispatchAction(ItemStackRequestAction action) {
        switch (action) {
            case TakeAction take -> handleTransfer(take);
            case PlaceAction place -> handleTransfer(place);
            case SwapAction swap -> handleSwap(swap);
            case DropAction drop -> handleDrop(drop);
            case DestroyAction destroy -> handleDestroy(destroy);
            case CraftCreativeAction craft -> handleCraftCreative(craft);
            case RecipeItemStackRequestAction recipe when isCraftRecipeAction(recipe) -> handleCraftRecipe(recipe);
            case ConsumeAction consume -> handleConsume(consume);
            default -> handleUnsupportedAction(action);
        }
    }

    private void handleUnsupportedAction(ItemStackRequestAction action) {
        switch (action.getType()) {
            case MINE_BLOCK, CRAFT_RESULTS_DEPRECATED, CREATE -> {
            }
            case CRAFT_RECIPE_OPTIONAL,
                 CRAFT_REPAIR_AND_DISENCHANT,
                 CRAFT_LOOM,
                 CRAFT_NON_IMPLEMENTED_DEPRECATED,
                 BEACON_PAYMENT,
                 LAB_TABLE_COMBINE -> {
                log.debug("Unimplemented inventory action type {} for {}", action.getType(), player.getName());
                requestFailed = true;
            }
            default -> log.debug("Unknown inventory action type: {}", action.getType());
        }
    }

    private boolean isCraftRecipeAction(RecipeItemStackRequestAction action) {
        return action.getType() == ItemStackRequestActionType.CRAFT_RECIPE || action.getType() == ItemStackRequestActionType.CRAFT_RECIPE_AUTO;
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

        ContainerSlotType srcContainer = container(srcSlot);
        ContainerSlotType dstContainer = container(dstSlot);
        SlotGroup srcGroup = screen.resolveSlotGroup(srcContainer);
        int srcViewSlot = screen.resolveInventorySlot(srcContainer, srcSlot.getSlot());
        SlotGroup dstGroup = screen.resolveSlotGroup(dstContainer);
        int dstViewSlot = screen.resolveInventorySlot(dstContainer, dstSlot.getSlot());

        InventoryClickEvent.ActionType actionType = actionType(action);
        InventoryClickEvent.ClickType clickType = clickType(action, sourceItem, count);

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

        if (srcContainer == ContainerSlotType.FURNACE_RESULT
                && screen instanceof CloudBlockContainerScreen blockScreen) {
            BlockEntity be = blockScreen.getBlock().getLevel().getBlockEntity(blockScreen.getBlock().getPosition());
            if (be instanceof Furnace furnace) {
                FurnaceExtractEvent extractEvent = new FurnaceExtractEvent(player, furnace, sourceItem, 0);
                player.getServer().getEventManager().fire(extractEvent);
                if (extractEvent.getExperience() != 0) {
                    player.addExperience(extractEvent.getExperience());
                }
            }
        }
    }

    private InventoryClickEvent.ActionType actionType(TransferItemStackRequestAction action) {
        return switch (action.getType()) {
            case TAKE -> InventoryClickEvent.ActionType.TAKE;
            case PLACE -> InventoryClickEvent.ActionType.PLACE;
            default -> InventoryClickEvent.ActionType.UNKNOWN;
        };
    }

    private InventoryClickEvent.ClickType clickType(TransferItemStackRequestAction action, ItemStack sourceItem, int count) {
        return switch (action.getType()) {
            case TAKE -> takeClickType(sourceItem, count);
            case PLACE -> placeClickType(sourceItem, count);
            default -> InventoryClickEvent.ClickType.UNKNOWN;
        };
    }

    private InventoryClickEvent.ClickType takeClickType(ItemStack sourceItem, int count) {
        if (count == sourceItem.getCount()) {
            return InventoryClickEvent.ClickType.TAKE_ALL;
        }

        if (count == sourceItem.getCount() / 2) {
            return InventoryClickEvent.ClickType.TAKE_HALF;
        }

        return InventoryClickEvent.ClickType.TAKE_SOME;
    }

    private InventoryClickEvent.ClickType placeClickType(ItemStack sourceItem, int count) {
        if (count == sourceItem.getCount()) {
            return InventoryClickEvent.ClickType.PLACE_ALL;
        }

        if (count == 1) {
            return InventoryClickEvent.ClickType.PLACE_ONE;
        }

        return InventoryClickEvent.ClickType.PLACE_SOME;
    }

    private void handleSwap(SwapAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStackRequestSlotData dstSlot = action.getDestination();

        ItemStack sourceItem = getSlot(srcSlot);
        ItemStack destItem = getSlot(dstSlot);

        ContainerSlotType srcContainer = container(srcSlot);
        ContainerSlotType dstContainer = container(dstSlot);
        SlotGroup srcGroup = screen.resolveSlotGroup(srcContainer);
        int srcViewSlot = screen.resolveInventorySlot(srcContainer, srcSlot.getSlot());
        SlotGroup dstGroup = screen.resolveSlotGroup(dstContainer);
        int dstViewSlot = screen.resolveInventorySlot(dstContainer, dstSlot.getSlot());

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

        ContainerSlotType srcContainer = container(srcSlot);
        SlotGroup srcGroup = screen.resolveSlotGroup(srcContainer);
        int srcViewSlot = screen.resolveInventorySlot(srcContainer, srcSlot.getSlot());

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

        ContainerSlotType srcContainer = container(srcSlot);
        SlotGroup srcGroup = screen.resolveSlotGroup(srcContainer);
        int srcViewSlot = screen.resolveInventorySlot(srcContainer, srcSlot.getSlot());

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

    private void handleCraftRecipe(RecipeItemStackRequestAction action) {
        CraftingRecipe recipe = resolveCraftingRecipe(action.getRecipeNetworkId());
        if (recipe == null) {
            return;
        }

        int numberOfCrafts = Math.max(1, action.getNumberOfRequestedCrafts());
        placeRecipeOutput(recipe, numberOfCrafts);
    }

    private void handleConsume(ConsumeAction action) {
        ContainerSlotType sourceContainer = container(action.getSource());
        if (sourceContainer != ContainerSlotType.CRAFTING_INPUT) {
            log.debug("Consume action from {} targeted non-crafting-input container {}", player.getName(), sourceContainer);
            requestFailed = true;
            return;
        }

        int sourceSlot = action.getSource().getSlot();
        int consumeCount = action.getCount();

        ItemStack current = this.screen.getSlot(sourceContainer, sourceSlot);
        if (current.isEmpty()) {
            log.debug("Consume action from {} targeted empty crafting input slot {}", player.getName(), sourceSlot);
            requestFailed = true;
            return;
        }

        if (current.getCount() < consumeCount) {
            log.debug("Consume action from {} requested {} but slot {} only has {}", player.getName(), consumeCount, sourceSlot, current.getCount());
            requestFailed = true;
            return;
        }

        ItemStack remaining = current.withCount(current.getCount() - consumeCount);
        this.screen.setSlot(sourceContainer, sourceSlot, remaining);
        trackAffectedSlot(sourceContainer, sourceSlot);
    }

    private CraftingRecipe resolveCraftingRecipe(int networkId) {
        Recipe recipe = CloudRecipeRegistry.get().getRecipeFromNetId(networkId);
        if (!(recipe instanceof CraftingRecipe craftingRecipe)) {
            log.debug("Crafting request from {} used unknown recipe network ID {}", player.getName(), networkId);
            requestFailed = true;
            return null;
        }

        boolean isCraftingTable = screen.getType() == ScreenTypes.CRAFTING_TABLE;
        boolean isInventory = screen.getType() == ScreenTypes.INVENTORY;

        if (!isCraftingTable && !isInventory) {
            log.debug("Crafting request from {} on incompatible screen type {}", player.getName(), screen.getType().getIdentifier());
            requestFailed = true;
            return null;
        }

        if (craftingRecipe.requiresCraftingTable() && !isCraftingTable) {
            log.debug("Crafting request from {} for recipe {} requires a crafting table", player.getName(), recipe.getId());
            requestFailed = true;
            return null;
        }

        return craftingRecipe;
    }

    private void placeRecipeOutput(CraftingRecipe recipe, int numberOfCrafts) {
        ItemStack result = recipe.getResult().withCount(recipe.getResult().getCount() * numberOfCrafts);
        this.screen.setSlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT, result);
        trackAffectedSlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT);
    }

    private ItemStack getSlot(ItemStackRequestSlotData slotData) {
        return this.screen.getSlot(container(slotData), slotData.getSlot());
    }

    private void setSlot(ItemStackRequestSlotData slotData, ItemStack item) {
        ContainerSlotType containerType = container(slotData);
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

        Map<ContainerSlotType, Map<Integer, ItemStack>> slots = new LinkedHashMap<>();
        for (Map.Entry<ContainerSlotType, Set<Integer>> entry : affectedSlots.entrySet()) {
            ContainerSlotType containerType = entry.getKey();
            for (int slot : entry.getValue()) {
                putCurrentStateSlot(slots, containerType, slot, this.screen.getSlot(containerType, slot));
            }
        }

        this.screen = null;
        this.currentRequest = null;
        this.affectedSlots.clear();

        return okResponse(requestId, slots);
    }

    public ItemStackResponse acknowledgeCurrentState(ItemStackRequest request, CloudInventoryScreen screen) {
        Map<ContainerSlotType, Map<Integer, ItemStack>> slots = new LinkedHashMap<>();

        for (ItemStackRequestAction action : request.getActions()) {
            trackCurrentStateSlots(slots, screen, action);
        }
        slots.computeIfAbsent(ContainerSlotType.CURSOR, key -> new LinkedHashMap<>()).put(0, ItemStack.EMPTY);

        return okResponse(request.getRequestId(), slots);
    }

    private void trackCurrentStateSlots(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, CloudInventoryScreen screen, ItemStackRequestAction action) {
        switch (action) {
            case TransferItemStackRequestAction transfer -> {
                trackCurrentStateSlot(slots, screen, transfer.getSource());
                trackCurrentStateSlot(slots, screen, transfer.getDestination());
            }
            case SwapAction swap -> {
                trackCurrentStateSlot(slots, screen, swap.getSource());
                trackCurrentStateSlot(slots, screen, swap.getDestination());
            }
            case DropAction drop -> trackCurrentStateSlot(slots, screen, drop.getSource());
            case DestroyAction destroy -> trackCurrentStateSlot(slots, screen, destroy.getSource());
            case ConsumeAction consume -> trackCurrentStateSlot(slots, screen, consume.getSource());
            default -> {
            }
        }
    }

    public ItemStackResponse acknowledgeLegacyCurrentState(int requestId, List<LegacySetItemSlotData> legacySlots) {
        Map<ContainerSlotType, Map<Integer, ItemStack>> slots = new LinkedHashMap<>();

        for (LegacySetItemSlotData legacySlot : legacySlots) {
            for (byte rawSlot : legacySlot.getSlots()) {
                trackLegacyCurrentStateSlot(slots, legacySlot.getContainerId(), Byte.toUnsignedInt(rawSlot));
            }
        }
        slots.computeIfAbsent(ContainerSlotType.CURSOR, key -> new LinkedHashMap<>()).put(0, ItemStack.EMPTY);

        return okResponse(requestId, slots);
    }

    private void trackCurrentStateSlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, CloudInventoryScreen screen, ItemStackRequestSlotData slotData) {
        ContainerSlotType containerType = container(slotData);
        int slot = slotData.getSlot();

        if (containerType == ContainerSlotType.ARMOR) {
            putCurrentStateSlot(slots, ContainerSlotType.ARMOR, slot, this.player.getArmor().getItem(slot));
            return;
        }

        if (containerType == ContainerSlotType.CURSOR) {
            putCurrentStateSlot(slots, ContainerSlotType.CURSOR, 0, ItemStack.EMPTY);
            return;
        }

        if (isInventoryContainer(containerType)) {
            int inventorySlot = normalizeInventorySlot(slot);
            if (inventorySlot < 0 || inventorySlot >= this.player.getInventory().size()) {
                log.debug("Ignoring out-of-bounds player inventory slot {} while acknowledging request for {}", slot, player.getName());
                return;
            }

            ContainerSlotType responseContainer = responseContainerForInventorySlot(inventorySlot);
            putCurrentStateSlot(slots, responseContainer, inventorySlot, this.player.getInventory().getItem(inventorySlot));
            return;
        }

        putCurrentStateSlot(slots, containerType, slot, screen.getSlot(containerType, slot));
    }

    private void trackLegacyCurrentStateSlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, int containerId, int slot) {
        if (containerId == ContainerId.INVENTORY) {
            trackLegacyPlayerInventorySlot(slots, slot);
            return;
        }

        if (containerId == ContainerId.ARMOR || containerId == LEGACY_ARMOR_CONTAINER_ID) {
            trackLegacyArmorSlot(slots, slot);
            return;
        }

        if (containerId == ContainerId.OFFHAND) {
            putCurrentStateSlot(slots, ContainerSlotType.OFFHAND, slot, this.player.getOffhand().getOffhandItem());
            return;
        }

        ContainerSlotType containerType = containerSlotTypeByOrdinal(containerId);
        if (isInventoryContainer(containerType)) {
            trackLegacyProtocolInventorySlot(slots, containerType, slot);
            return;
        }

        if (containerType == ContainerSlotType.ARMOR) {
            trackLegacyArmorSlot(slots, slot);
            return;
        }

        if (containerType == ContainerSlotType.OFFHAND) {
            putCurrentStateSlot(slots, ContainerSlotType.OFFHAND, slot, this.player.getOffhand().getOffhandItem());
            return;
        }

        log.debug("Ignoring unsupported legacy slot {} in container {} while acknowledging request for {}", slot, containerId, player.getName());
    }

    private void trackLegacyPlayerInventorySlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, int slot) {
        int inventorySlot = normalizeInventorySlot(slot);
        if (inventorySlot < 0 || inventorySlot >= this.player.getInventory().size()) {
            log.debug("Ignoring out-of-bounds legacy inventory slot {} while acknowledging request for {}", slot, player.getName());
            return;
        }

        ContainerSlotType responseContainer = responseContainerForInventorySlot(inventorySlot);
        putCurrentStateSlot(slots, responseContainer, inventorySlot, this.player.getInventory().getItem(inventorySlot));
    }

    private void trackLegacyProtocolInventorySlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, ContainerSlotType containerType, int slot) {
        int inventorySlot = normalizeInventorySlot(slot);
        if (inventorySlot < 0 || inventorySlot >= this.player.getInventory().size()) {
            log.debug("Ignoring out-of-bounds legacy {} slot {} while acknowledging request for {}", containerType, slot, player.getName());
            return;
        }

        putCurrentStateSlot(slots, containerType, slot, this.player.getInventory().getItem(inventorySlot));
    }

    private void trackLegacyArmorSlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, int slot) {
        if (slot < 0 || slot >= 4) {
            log.debug("Ignoring out-of-bounds legacy armor slot {} while acknowledging request for {}", slot, player.getName());
            return;
        }

        putCurrentStateSlot(slots, ContainerSlotType.ARMOR, slot, this.player.getArmor().getItem(slot));
    }

    private ContainerSlotType containerSlotTypeByOrdinal(int containerId) {
        ContainerSlotType[] containerTypes = ContainerSlotType.values();
        if (containerId < 0 || containerId >= containerTypes.length) {
            return ContainerSlotType.UNKNOWN;
        }
        return containerTypes[containerId];
    }

    private boolean isInventoryContainer(ContainerSlotType containerType) {
        return containerType == ContainerSlotType.HOTBAR
                || containerType == ContainerSlotType.HOTBAR_AND_INVENTORY
                || containerType == ContainerSlotType.INVENTORY;
    }

    private ContainerSlotType responseContainerForInventorySlot(int inventorySlot) {
        return inventorySlot < HOTBAR_SIZE ? ContainerSlotType.HOTBAR : ContainerSlotType.INVENTORY;
    }

    private void putCurrentStateSlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, ContainerSlotType containerType, int slot, ItemStack item) {
        slots.computeIfAbsent(containerType, key -> new LinkedHashMap<>()).put(slot, item);
    }

    private ItemStackResponse okResponse(int requestId, Map<ContainerSlotType, Map<Integer, ItemStack>> slots) {
        List<ItemStackResponseContainer> containers = new ArrayList<>();
        for (Map.Entry<ContainerSlotType, Map<Integer, ItemStack>> entry : slots.entrySet()) {
            ContainerSlotType containerType = entry.getKey();

            List<ItemStackResponseSlot> responseSlots = new ArrayList<>();
            for (Map.Entry<Integer, ItemStack> slotEntry : entry.getValue().entrySet()) {
                responseSlots.add(makeResponseSlot(slotEntry.getKey(), slotEntry.getValue()));
            }

            containers.add(new ItemStackResponseContainer(
                    containerType,
                    responseSlots,
                    new FullContainerName(containerType, null)
            ));
        }

        return new ItemStackResponse(ItemStackResponseStatus.OK, requestId, containers);
    }

    private int normalizeInventorySlot(int slot) {
        if (slot >= BEDROCK_HOTBAR_START_SLOT && slot <= BEDROCK_HOTBAR_END_SLOT) {
            return slot - BEDROCK_HOTBAR_START_SLOT;
        }
        return slot;
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
