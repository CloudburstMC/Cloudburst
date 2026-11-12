package org.cloudburstmc.server.network.inventory;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.crafting.CraftingRecipe;
import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.crafting.ShulkerBoxRecipe;
import org.cloudburstmc.api.event.inventory.ActionType;
import org.cloudburstmc.api.event.inventory.CartographyItemEvent;
import org.cloudburstmc.api.event.inventory.ClickType;
import org.cloudburstmc.api.event.inventory.InventoryClickEvent;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.ItemLockMode;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.server.container.grindstone.GrindstoneResultCalculator;
import org.cloudburstmc.server.container.screen.*;
import org.cloudburstmc.server.container.screen.ResultContainerScreen.ResultQuote;
import org.cloudburstmc.server.container.screen.ResultContainerScreen.ResultTake;
import org.cloudburstmc.server.container.view.CloudBeaconView;
import org.cloudburstmc.server.item.ShulkerBoxItems;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Validates one inventory request against staged slot values before applying it to the active screen.
 */
@Log4j2
public class InventoryRequest {

    private static final int CREATED_OUTPUT_PROTOCOL_SLOT = 50;
    private static final int HOTBAR_SIZE = 9;

    private final CloudPlayer player;
    private final Map<ContainerSlotType, Set<Integer>> affectedSlots = new LinkedHashMap<>();
    private final InventorySlotChanges changes = new InventorySlotChanges();
    private @Nullable Set<Integer> shulkerDyeConsumes;
    private final List<Runnable> effects = new ArrayList<>();
    private final Set<InventorySlotChanges.SlotKey> stationSlots = new HashSet<>();
    private @Nullable PendingResult pendingResult;
    private @Nullable ResultTake preparedTake;
    private @Nullable CraftRecipeAction enchantmentSelection;
    private @Nullable CraftRecipeAction stonecuttingSelection;
    private @Nullable BeaconPaymentAction beaconPayment;
    private @Nullable String stationRename;
    private final CloudInventoryScreen screen;
    private final ItemStackRequest currentRequest;
    private boolean requestFailed;
    private boolean requiresFullSlotUpdate;

    public InventoryRequest(CloudPlayer player, CloudInventoryScreen screen, ItemStackRequest request) {
        this.player = player;
        this.screen = screen;
        this.currentRequest = request;
    }

    public ItemStackResponse execute() {
        if (this.screen instanceof CloudLecternContainerScreen) {
            return new ItemStackResponse(ItemStackResponseStatus.ERROR, this.currentRequest.getRequestId(), List.of());
        }

        ItemStackRequestAction[] actions = this.currentRequest.getActions();
        if (actions.length != 0 && actions[0] instanceof BeaconPaymentAction payment) {
            this.requestFailed = !(this.screen instanceof CloudBeaconContainerScreen)
                    || actions.length != 2
                    || !(actions[1] instanceof DestroyAction destroy)
                    || !this.prepareBeaconPayment(payment, destroy);
        } else if (this.screen instanceof CloudEnchantingContainerScreen && actions.length != 0 && actions[0] instanceof CraftRecipeAction selection) {
            this.handleAction(selection);
        } else if (!isActionOrderValid(actions)) {
            this.requestFailed = true;
        } else {
            for (ItemStackRequestAction action : actions) {
                if (this.requestFailed || this.screen != this.player.getInventoryManager().getScreen()) {
                    this.requestFailed = true;
                    break;
                }
                this.handleAction(action);
            }
        }
        return this.finish();
    }

    private static boolean isActionOrderValid(ItemStackRequestAction[] actions) {
        boolean crafting = false;
        for (ItemStackRequestAction action : actions) {
            switch (action.getType()) {
                case TAKE, PLACE, DROP, DESTROY, LAB_TABLE_COMBINE, MINE_BLOCK -> {
                }
                case SWAP, CRAFT_RECIPE, CRAFT_RECIPE_AUTO, CRAFT_CREATIVE, CRAFT_RECIPE_OPTIONAL,
                     CRAFT_REPAIR_AND_DISENCHANT, CRAFT_LOOM, CRAFT_NON_IMPLEMENTED_DEPRECATED -> {
                    if (crafting) {
                        return false;
                    }
                    crafting = true;
                }
                case CONSUME, CREATE, CRAFT_RESULTS_DEPRECATED -> {
                    if (!crafting) {
                        return false;
                    }
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    private void prepareRequest() {
        if (this.screen instanceof CloudCartographyContainerScreen) {
            this.stationRename = renameText(this.currentRequest);
        }
    }

    private void handleAction(ItemStackRequestAction action) {
        try {
            if (action == this.currentRequest.getActions()[0]) {
                this.prepareRequest();
            }

            if (this.screen != this.player.getInventoryManager().getScreen()) {
                this.requestFailed = true;
                return;
            }

            this.dispatchAction(action);
        } catch (Exception e) {
            log.warn("Failed to handle inventory action {} for {}", action.getType(), this.player.getName(), e);
            this.requestFailed = true;
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
            case CraftGrindstoneAction _ -> handleCraftGrindstone();
            case RecipeItemStackRequestAction recipe when isCraftRecipeAction(recipe) -> handleCraftRecipe(recipe);
            case CraftRecipeOptionalAction craftOptional -> handleCraftRecipeOptional(craftOptional);
            case ConsumeAction consume -> handleConsume(consume);
            case MineBlockAction mineBlock -> handleMineBlock(mineBlock);
            default -> handleUnsupportedAction(action);
        }
    }

    private void handleUnsupportedAction(ItemStackRequestAction action) {
        switch (action.getType()) {
            case CRAFT_RESULTS_DEPRECATED, CREATE -> {
            }
            case CRAFT_LOOM,
                 CRAFT_NON_IMPLEMENTED_DEPRECATED,
                 LAB_TABLE_COMBINE -> {
                log.debug("Unimplemented inventory action type {} for {}", action.getType(), player.getName());
                requestFailed = true;
            }
            default -> this.requestFailed = true;
        }
    }

    private ItemStackResponse finish() {
        int requestId = this.currentRequest.getRequestId();

        if (this.screen != this.player.getInventoryManager().getScreen()) {
            return new ItemStackResponse(ItemStackResponseStatus.ERROR, requestId, List.of());
        }

        if (!this.requestFailed) {
            this.requestFailed = this.changes.isStale() || !this.prepareStationAction();
            if (!this.requestFailed) {
                ResultTake take = this.preparedTake;
                this.changes.commit(take == null ? () -> { } : take.writeSlots());
                if (take != null) {
                    take.afterCommit().run();
                } else {
                    this.publishRename();
                }
                this.effects.forEach(Runnable::run);
            }
        }

        this.requiresFullSlotUpdate |= this.requestFailed;

        if (this.screen instanceof CloudEnchantingContainerScreen enchanting) {
            enchanting.sendOffers();
        }

        if (this.requiresFullSlotUpdate) {
            for (Map.Entry<ContainerSlotType, Set<Integer>> entry : this.affectedSlots.entrySet()) {
                for (int slot : entry.getValue()) {
                    this.player.getInventoryRequestQueue().resendSlotAfterResponse(this.screen, entry.getKey(), slot);
                }
            }
        }

        if (this.requestFailed) {
            return new ItemStackResponse(ItemStackResponseStatus.ERROR, requestId, List.of());
        }

        Map<ContainerSlotType, Map<Integer, ItemStack>> slots = new LinkedHashMap<>();
        for (Map.Entry<ContainerSlotType, Set<Integer>> entry : this.affectedSlots.entrySet()) {
            ContainerSlotType containerType = entry.getKey();
            for (int slot : entry.getValue()) {
                InventoryResponses.putCurrentStateSlot(slots, containerType, slot, this.screen.getSlot(containerType, slot));
            }
        }

        return InventoryResponses.okResponse(requestId, slots);
    }

    private void handleTransfer(TransferItemStackRequestAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStackRequestSlotData dstSlot = action.getDestination();
        ContainerSlotType srcContainer = container(srcSlot);
        ContainerSlotType dstContainer = container(dstSlot);

        if (screen instanceof ResultContainerScreen resultScreen && resultScreen.isResultSlot(srcContainer, srcSlot.getSlot())) {
            handleResultTransfer(resultScreen, action);
            return;
        }

        if (screen instanceof ResultContainerScreen resultScreen && resultScreen.isResultSlot(dstContainer, dstSlot.getSlot())) {
            throw new IllegalArgumentException("Cannot place into a result slot");
        }
        rejectFurnaceResultInsert(dstSlot);

        ItemStack sourceItem = getSlot(srcSlot);
        ItemStack destItem = getSlot(dstSlot);
        int count = action.getCount();

        if (isMoveBlocked(sourceItem, srcContainer, dstContainer)) {
            requestFailed = true;
            return;
        }

        if (count <= 0 || sourceItem.isEmpty() || sourceItem.getCount() < count) {
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
            if (!destItem.isStackableWith(sourceItem)) {
                throw new IllegalArgumentException("Cannot merge incompatible items in transfer");
            }
            newDest = destItem.withCount(destItem.getCount() + count);
        }

        ResolvedRequestSlot srcResolvedSlot = resolveSlot(srcSlot);
        ResolvedRequestSlot dstResolvedSlot = resolveSlot(dstSlot);
        if (srcResolvedSlot.key().equals(dstResolvedSlot.key())) {
            throw new IllegalArgumentException("Cannot transfer within the same slot");
        }

        ActionType actionType = actionType(action);
        ClickType clickType = clickType(action, sourceItem, count);

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(srcResolvedSlot.inventorySlot())
                .slotGroup(srcResolvedSlot.slotGroup())
                .sourceItem(sourceItem)
                .cursorItem(destItem)
                .actionType(actionType)
                .clickType(clickType)
                .destinationSlot(dstResolvedSlot.inventorySlot())
                .destinationSlotGroup(dstResolvedSlot.slotGroup())
                .resultItem(newDest)
                .build();
        if (this.rejectClick(event)) {
            return;
        }

        ItemStack resolvedDest = event.getResultItem() != null ? event.getResultItem() : newDest;
        validateStationSlot(dstSlot, resolvedDest);

        setSlot(srcSlot, newSource);
        setSlot(dstSlot, resolvedDest);

        this.effects.add(() -> onFurnaceResultTaken(srcSlot, sourceItem.withCount(count)));
    }

    private void handleResultTransfer(ResultContainerScreen resultScreen, TransferItemStackRequestAction action) {
        if (action.getType() != ItemStackRequestActionType.TAKE && action.getType() != ItemStackRequestActionType.PLACE) {
            throw new IllegalArgumentException("Unsupported result transfer action " + action.getType());
        }

        ItemStackRequestSlotData dstSlot = action.getDestination();
        rejectResultMutation(dstSlot);
        if (resultScreen instanceof CloudStonecutterContainerScreen && resultScreen.isInputSlot(container(dstSlot))) {
            this.requestFailed = true;
            return;
        }

        ItemStack sourceItem = getSlot(action.getSource());
        ItemStack destItem = getSlot(dstSlot);
        int count = action.getCount();

        resultScreen.trackResultSlots((type, slot) -> {
            ResolvedRequestSlot resolved = this.resolveSlot(type, slot);
            this.stationSlots.add(resolved.key());
            this.trackAffectedSlot(type, slot);
            this.requestFailed |= this.changes.hasChanges(resolved.slotGroup(), resolved.inventorySlot());
        });

        if (this.requestFailed) {
            return;
        }

        ResultQuote quote = this.quoteResult();
        if (resultScreen instanceof CloudStonecutterContainerScreen) {
            this.requiresFullSlotUpdate = true;
            if (quote == null) {
                this.trackAffectedSlot(container(dstSlot), dstSlot.getSlot());
                this.requestFailed = true;
                return;
            }
        }

        if (quote != null) {
            sourceItem = quote.item();
            if (this.changes.isStale() || this.screen != this.player.getInventoryManager().getScreen()) {
                this.requestFailed = true;
                return;
            }
        }

        if (sourceItem.isEmpty() || (quote == null ? resultScreen.isInvalidResultCount(sourceItem, count) : quote.invalidCount().test(count))) {
            resultScreen.trackResultSlots(this::trackAffectedSlot);
            trackAffectedSlot(container(dstSlot), dstSlot.getSlot());
            this.requiresFullSlotUpdate = true;
            this.requestFailed = true;
            return;
        }

        ItemStack newDest;
        if (destItem.isEmpty()) {
            newDest = sourceItem.withCount(count);
        } else {
            if (!resultScreen.canMergeResult() || !destItem.isStackableWith(sourceItem)) {
                throw new IllegalArgumentException("Cannot merge incompatible result");
            }

            newDest = destItem.withCount(destItem.getCount() + count);
        }

        ResolvedRequestSlot srcResolvedSlot = resolveSlot(action.getSource());
        ResolvedRequestSlot dstResolvedSlot = resolveSlot(dstSlot);

        if (newDest.getCount() > dstResolvedSlot.slotGroup().getMaxStackSize(dstResolvedSlot.inventorySlot())) {
            throw new IllegalArgumentException("Result exceeds destination stack limit");
        }

        InventoryClickEvent event;
        if (screen instanceof CloudCartographyContainerScreen cartography) {
            event = new CartographyItemEvent(cartography, sourceItem, destItem, takeClickType(sourceItem, count),
                    dstResolvedSlot.inventorySlot(), dstResolvedSlot.slotGroup(), newDest);
        } else {
            event = new InventoryClickEvent.Builder()
                    .screen(screen)
                    .slot(srcResolvedSlot.inventorySlot())
                    .slotGroup(srcResolvedSlot.slotGroup())
                    .sourceItem(sourceItem)
                    .cursorItem(destItem)
                    .actionType(ActionType.TAKE)
                    .clickType(takeClickType(sourceItem, count))
                    .destinationSlot(dstResolvedSlot.inventorySlot())
                    .destinationSlotGroup(dstResolvedSlot.slotGroup())
                    .resultItem(newDest)
                    .build();
        }

        if (this.rejectClick(event)) {
            return;
        }

        if (this.pendingResult != null || this.changes.hasChanges(dstResolvedSlot.slotGroup(), dstResolvedSlot.inventorySlot())) {
            requestFailed = true;
            return;
        }

        this.stationSlots.add(dstResolvedSlot.key());
        this.trackAffectedSlot(container(dstSlot), dstSlot.getSlot());
        ItemStack proposed = event.getResultItem();
        if (proposed != null) {
            this.validateStationSlot(dstSlot, proposed);
        }
        this.pendingResult = new PendingResult(resultScreen, dstResolvedSlot, sourceItem, destItem, newDest, proposed, quote, count);
    }

    private @Nullable ResultQuote quoteResult() {
        if (this.screen instanceof CloudStonecutterContainerScreen stonecutter) {
            CraftRecipeAction selection = this.stonecuttingSelection;
            return selection == null ? null
                    : stonecutter.quoteRecipe(selection.getRecipeNetworkId(), selection.getNumberOfRequestedCrafts());
        }

        String name = this.stationRename;
        if (name == null) {
            return null;
        }

        return switch (this.screen) {
            case CloudAnvilContainerScreen anvil -> anvil.quoteRename(name);
            case CloudCartographyContainerScreen cartography -> cartography.quoteRename(name);
            default -> null;
        };
    }

    private void publishRename() {
        String name = this.stationRename;
        if (name == null) {
            return;
        }

        if (this.screen instanceof CloudAnvilContainerScreen anvil) {
            anvil.updateRenameText(name);
        } else if (this.screen instanceof CloudCartographyContainerScreen cartography) {
            cartography.updateRenameText(name);
        }
    }

    private boolean prepareStationAction() {
        BeaconPaymentAction payment = this.beaconPayment;
        if (payment != null && this.screen instanceof CloudBeaconContainerScreen beacon) {
            int slot = CloudBeaconContainerScreen.PAYMENT_SLOT;
            ContainerSlotType type = ContainerSlotType.BEACON_PAYMENT;
            ResolvedRequestSlot paymentSlot = this.resolveSlot(type, slot);
            ItemStack item = this.changes.read(paymentSlot.slotGroup(), paymentSlot.inventorySlot());
            CloudBeaconContainerScreen.PaymentConfirmation confirmation = beacon.preparePayment(payment.getPrimaryEffect(), payment.getSecondaryEffect(), item);

            if (confirmation == null || this.changes.isStale()) {
                return false;
            }

            if (confirmation.consumeItem()) {
                this.stageSlot(type, slot, ItemStack.EMPTY);
            }

            this.effects.add(confirmation.commit());
            return true;
        }

        CraftRecipeAction selection = this.enchantmentSelection;
        if (selection != null) {
            return this.screen instanceof CloudEnchantingContainerScreen enchanting && enchanting.selectOffer(selection.getRecipeNetworkId());
        }

        PendingResult result = this.pendingResult;
        if (result == null) {
            CraftRecipeAction stonecutting = this.stonecuttingSelection;
            if (stonecutting != null && this.screen instanceof CloudStonecutterContainerScreen stonecutter) {
                if (this.currentRequest.getActions().length != 1) {
                    return false;
                }

                Runnable commit = stonecutter.prepareRecipeSelection(stonecutting.getRecipeNetworkId());
                if (commit == null) {
                    return false;
                }

                this.effects.add(commit);
            }
            return true;
        }

        ResultQuote quote = result.quote();
        ResultTake take = quote == null ? result.screen().prepareTake(result.source(), result.count()) : quote.prepareTake().apply(result.count());
        if (take == null || this.changes.isStale() || this.screen != this.player.getInventoryManager().getScreen()) {
            return false;
        }

        ItemStack taken = take.item();
        ItemStack proposed = result.proposed();
        ItemStack destination = result.destination();
        ItemStack resolvedDest = proposed != null && !proposed.equals(result.predicted()) ? proposed
                : destination.isEmpty() ? taken : taken.withCount(destination.getCount() + taken.getCount());
        ResolvedRequestSlot slot = result.slot();
        this.writeSlot(slot, resolvedDest);
        this.preparedTake = take;

        this.requiresFullSlotUpdate |= !resolvedDest.equals(result.predicted());
        return true;
    }

    private boolean prepareBeaconPayment(BeaconPaymentAction payment, DestroyAction destroy) {
        ContainerSlotType type = ContainerSlotType.BEACON_PAYMENT;
        int slot = CloudBeaconContainerScreen.PAYMENT_SLOT;
        this.trackAffectedSlot(type, slot);

        ItemStackRequestSlotData source = destroy.getSource();
        if (destroy.getCount() != 1 || container(source) != type || source.getSlot() != slot) {
            return false;
        }

        ItemStack item = this.getSlot(source);
        if (CloudBeaconView.isInvalidPayment(item) || item.get(ItemDataComponents.ITEM_LOCK) != null || ItemStackNetworkIds.get(item) != source.getStackNetworkId()) {
            return false;
        }

        this.beaconPayment = payment;
        return true;
    }

    private void handleSwap(SwapAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        ItemStackRequestSlotData dstSlot = action.getDestination();
        rejectResultMutation(srcSlot);
        rejectResultMutation(dstSlot);
        rejectFurnaceResultInsert(srcSlot);
        rejectFurnaceResultInsert(dstSlot);

        ItemStack sourceItem = getSlot(srcSlot);
        ItemStack destItem = getSlot(dstSlot);

        ContainerSlotType srcContainer = container(srcSlot);
        ContainerSlotType dstContainer = container(dstSlot);
        if (isMoveBlocked(sourceItem, srcContainer, dstContainer) || isMoveBlocked(destItem, dstContainer, srcContainer)) {
            requestFailed = true;
            return;
        }

        ResolvedRequestSlot srcResolvedSlot = resolveSlot(srcSlot);
        ResolvedRequestSlot dstResolvedSlot = resolveSlot(dstSlot);

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(srcResolvedSlot.inventorySlot())
                .slotGroup(srcResolvedSlot.slotGroup())
                .sourceItem(sourceItem)
                .cursorItem(destItem)
                .actionType(ActionType.SWAP)
                .clickType(ClickType.UNKNOWN)
                .destinationSlot(dstResolvedSlot.inventorySlot())
                .destinationSlotGroup(dstResolvedSlot.slotGroup())
                .resultItem(sourceItem)
                .build();
        if (this.rejectClick(event)) {
            return;
        }

        ItemStack resolvedDest = event.getResultItem() != null ? event.getResultItem() : sourceItem;
        validateStationSlot(srcSlot, destItem);
        validateStationSlot(dstSlot, resolvedDest);

        setSlot(srcSlot, destItem);
        setSlot(dstSlot, resolvedDest);
    }

    private void handleDrop(DropAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        rejectResultMutation(srcSlot);
        ItemStack sourceItem = getSlot(srcSlot);
        int count = action.getCount();

        if (sourceItem.get(ItemDataComponents.ITEM_LOCK) != null) {
            requestFailed = true;
            return;
        }

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

        ResolvedRequestSlot srcResolvedSlot = resolveSlot(srcSlot);

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(srcResolvedSlot.inventorySlot())
                .slotGroup(srcResolvedSlot.slotGroup())
                .sourceItem(sourceItem)
                .cursorItem(ItemStack.EMPTY)
                .actionType(ActionType.DROP)
                .clickType(ClickType.UNKNOWN)
                .build();
        if (this.rejectClick(event)) {
            return;
        }

        setSlot(srcSlot, newSource);
        this.effects.add(() -> {
            this.player.dropItem(dropItem);
            this.onFurnaceResultTaken(srcSlot, dropItem);
        });
    }

    private void handleDestroy(DestroyAction action) {
        ItemStackRequestSlotData srcSlot = action.getSource();
        rejectResultMutation(srcSlot);
        ItemStack sourceItem = getSlot(srcSlot);
        int count = action.getCount();

        if (sourceItem.get(ItemDataComponents.ITEM_LOCK) != null) {
            requestFailed = true;
            return;
        }

        if (sourceItem.isEmpty()) {
            throw new IllegalArgumentException("Source item is empty");
        }

        ResolvedRequestSlot srcResolvedSlot = resolveSlot(srcSlot);

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(srcResolvedSlot.inventorySlot())
                .slotGroup(srcResolvedSlot.slotGroup())
                .sourceItem(sourceItem)
                .cursorItem(ItemStack.EMPTY)
                .actionType(ActionType.DESTROY)
                .clickType(ClickType.UNKNOWN)
                .build();
        if (this.rejectClick(event)) {
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

    private void handleMineBlock(MineBlockAction action) {
        int hotbarSlot = action.getHotbarSlot();
        if (hotbarSlot < 0 || hotbarSlot >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("Invalid mining hotbar slot " + hotbarSlot);
        }

        trackAffectedSlot(ContainerSlotType.HOTBAR, hotbarSlot);
    }

    private void handleCraftCreative(CraftCreativeAction action) {
        if (!this.player.isCreative()) {
            this.requestFailed = true;
            return;
        }

        int creativeNetId = action.getCreativeItemNetworkId();
        ItemStack creativeItem = CloudItemRegistry.get().getCreativeItem(creativeNetId);

        if (creativeItem == null) {
            throw new IllegalArgumentException("Unknown creative item network ID: " + creativeNetId);
        }

        creativeItem = creativeItem.withCount(CloudItemRegistry.get()
                .requireComponent(creativeItem.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(creativeItem));

        ResolvedRequestSlot createdOutputSlot = resolveSlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT);

        InventoryClickEvent event = new InventoryClickEvent.Builder()
                .screen(screen)
                .slot(createdOutputSlot.inventorySlot())
                .slotGroup(createdOutputSlot.slotGroup())
                .sourceItem(ItemStack.EMPTY)
                .cursorItem(creativeItem)
                .actionType(ActionType.CRAFT_CREATIVE)
                .clickType(ClickType.UNKNOWN)
                .build();
        if (this.rejectClick(event)) {
            return;
        }

        this.stageSlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT, creativeItem);
    }

    private boolean isCraftRecipeAction(RecipeItemStackRequestAction action) {
        return action.getType() == ItemStackRequestActionType.CRAFT_RECIPE || action.getType() == ItemStackRequestActionType.CRAFT_RECIPE_AUTO;
    }

    private void handleCraftRecipe(RecipeItemStackRequestAction action) {
        if (this.screen instanceof CloudStonecutterContainerScreen stonecutter) {
            stonecutter.trackResultSlots(this::trackAffectedSlot);
            if (action instanceof CraftRecipeAction selection && this.currentRequest.getActions()[0] == action) {
                this.stonecuttingSelection = selection;
            } else {
                this.requestFailed = true;
            }
            return;
        }

        if (screen instanceof CloudEnchantingContainerScreen) {
            if (action instanceof CraftRecipeAction selection && this.currentRequest.getActions()[0] == action) {
                this.enchantmentSelection = selection;
            } else {
                this.requestFailed = true;
            }
            this.requiresFullSlotUpdate = true;
            this.trackAffectedSlot(ContainerSlotType.ENCHANTING_INPUT, CloudEnchantingContainerScreen.INPUT_UI_SLOT);
            this.trackAffectedSlot(ContainerSlotType.ENCHANTING_MATERIAL, CloudEnchantingContainerScreen.REAGENT_UI_SLOT);
            return;
        }

        if (screen instanceof CloudSmithingContainerScreen smithing) {
            smithing.trackResultSlots(this::trackAffectedSlot);
            return;
        }

        if (screen instanceof CloudCartographyContainerScreen cartography) {
            cartography.trackResultSlots(this::trackAffectedSlot);
            return;
        }

        CraftingRecipe recipe = resolveCraftingRecipe(action.getRecipeNetworkId());
        if (recipe == null) {
            return;
        }

        int numberOfCrafts = Math.max(1, action.getNumberOfRequestedCrafts());
        if (recipe instanceof ShulkerBoxRecipe dyeRecipe) {
            this.dyeShulkerBox(dyeRecipe, numberOfCrafts);
        } else {
            placeRecipeOutput(recipe, numberOfCrafts);
        }
    }

    private void handleCraftRecipeOptional(CraftRecipeOptionalAction action) {
        if (!(screen instanceof CloudAnvilContainerScreen) && !(screen instanceof CloudCartographyContainerScreen)) {
            log.debug("Optional craft request from {} targeted unsupported screen type {}", player.getName(), screen.getType().getIdentifier());
            requestFailed = true;
            return;
        }

        String[] filterStrings = currentRequest.getFilterStrings();
        if (filterStrings != null && filterStrings.length != 0) {
            int index = action.getFilteredStringIndex();
            if (index < 0 || index >= filterStrings.length) {
                log.debug("Rename request from {} used invalid filter string index {}", player.getName(), index);
                requestFailed = true;
                return;
            }

            if (screen instanceof CloudAnvilContainerScreen) {
                this.stationRename = Objects.requireNonNull(filterStrings[index], "rename text");
            }
        }

        ((ResultContainerScreen) screen).trackResultSlots(this::trackAffectedSlot);
    }

    private void handleCraftGrindstone() {
        if (!(this.screen instanceof CloudGrindstoneContainerScreen grindstone)) {
            this.requestFailed = true;
            return;
        }

        grindstone.trackResultSlots(this::trackAffectedSlot);
    }

    private void handleConsume(ConsumeAction action) {
        ContainerSlotType sourceContainer = container(action.getSource());
        if (this.screen instanceof CloudStonecutterContainerScreen) {
            if (this.stonecuttingSelection == null
                    || sourceContainer != ContainerSlotType.STONECUTTER_INPUT
                    || action.getSource().getSlot() != CloudStonecutterContainerScreen.INPUT_UI_SLOT
                    || action.getCount() <= 0) {
                this.requestFailed = true;
            }
            return;
        }

        if (screen instanceof ResultContainerScreen resultScreen && resultScreen.isInputSlot(sourceContainer)) {
            resultScreen.trackResultSlots(this::trackAffectedSlot);
            return;
        }

        if (sourceContainer != ContainerSlotType.CRAFTING_INPUT) {
            log.debug("Consume action from {} targeted non-crafting-input container {}", player.getName(), sourceContainer);
            requestFailed = true;
            return;
        }

        int sourceSlot = action.getSource().getSlot();
        int consumeCount = action.getCount();

        if (this.shulkerDyeConsumes != null) {
            if (consumeCount != 1 || !this.shulkerDyeConsumes.remove(sourceSlot)) {
                this.requestFailed = true;
            }
            return;
        }

        ItemStack current = this.getSlot(action.getSource());
        if (current.isEmpty()) {
            log.debug("Consume action from {} targeted empty crafting input slot {}", player.getName(), sourceSlot);
            requestFailed = true;
            return;
        }

        if (current.get(ItemDataComponents.ITEM_LOCK) != null) {
            requestFailed = true;
            return;
        }

        if (consumeCount <= 0 || current.getCount() < consumeCount) {
            log.debug("Consume action from {} requested {} but slot {} only has {}", player.getName(), consumeCount, sourceSlot, current.getCount());
            requestFailed = true;
            return;
        }

        ItemStack remaining = current.withCount(current.getCount() - consumeCount);
        this.stageSlot(sourceContainer, sourceSlot, remaining);
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
        this.stageSlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT, result);
    }

    private void dyeShulkerBox(ShulkerBoxRecipe recipe, int crafts) {
        if (crafts != 1) {
            throw new IllegalArgumentException("Only one shulker box may be dyed per request");
        }

        ContainerSlotType grid = ContainerSlotType.CRAFTING_INPUT;
        SlotGroup group = Objects.requireNonNull(this.screen.resolveSlotGroup(grid), "crafting grid");
        ItemStack box = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;

        int boxSlot = -1;
        int dyeSlot = -1;

        for (int slot = 0; slot < group.size(); slot++) {
            ItemStack item = this.changes.read(group, slot);
            if (item.isEmpty()) {
                continue;
            }

            if (item.get(ItemDataComponents.ITEM_LOCK) != null) {
                throw new IllegalArgumentException("Cannot dye a shulker box with locked ingredients");
            }

            if (box.isEmpty() && ShulkerBoxItems.TYPES.contains(item.getType()) && recipe.getShulkerBox().test(item)) {
                box = item;
                boxSlot = this.screen.resolveProtocolSlot(grid, slot);
            } else if (dye.isEmpty() && recipe.getDye().test(item)) {
                dye = item;
                dyeSlot = this.screen.resolveProtocolSlot(grid, slot);
            } else {
                throw new IllegalArgumentException("Unexpected ingredient for shulker box dyeing");
            }
        }

        if (boxSlot < 0 || dyeSlot < 0) {
            throw new IllegalArgumentException("Shulker box dyeing requires a box and dye");
        }

        ItemStack result = ShulkerBoxItems.recolor(box, recipe.getResult().getType());
        this.stageSlot(grid, boxSlot, box.decreaseCount());
        this.stageSlot(grid, dyeSlot, dye.decreaseCount());
        this.stageSlot(ContainerSlotType.CREATED_OUTPUT, CREATED_OUTPUT_PROTOCOL_SLOT, result);
        this.shulkerDyeConsumes = new HashSet<>(Set.of(boxSlot, dyeSlot));
    }

    private void rejectFurnaceResultInsert(ItemStackRequestSlotData slot) {
        if (screen instanceof CloudFurnaceContainerScreen && container(slot) == ContainerSlotType.FURNACE_RESULT) {
            throw new IllegalArgumentException("Cannot insert items into a furnace result slot");
        }
    }

    private void validateStationSlot(ItemStackRequestSlotData slot, ItemStack item) {
        if (container(slot) == ContainerSlotType.BEACON_PAYMENT && !item.isEmpty() && CloudBeaconView.isInvalidPayment(item)) {
            throw new IllegalArgumentException("Item cannot be placed in a beacon payment slot");
        }

        if (container(slot) == ContainerSlotType.SHULKER_BOX && !ShulkerBoxItems.accepts(item)) {
            throw new IllegalArgumentException("Shulker boxes cannot be stored in shulker boxes");
        }

        if (item.getCount() > 1 && ShulkerBoxItems.TYPES.contains(item.getType())) {
            throw new IllegalArgumentException("Shulker boxes cannot be stacked");
        }

        if (screen instanceof CloudFurnaceContainerScreen furnace && container(slot) == ContainerSlotType.FURNACE_FUEL && !furnace.furnaceEntity().acceptsFuel(item)) {
            throw new IllegalArgumentException("Item cannot be placed in a furnace fuel slot");
        }

        if (screen instanceof CloudBrewingContainerScreen brewing && !brewing.acceptsItem(container(slot), slot.getSlot(), item)) {
            throw new IllegalArgumentException("Item cannot be placed in that brewing stand slot");
        }

        if (screen instanceof CloudGrindstoneContainerScreen grindstone && grindstone.isInputSlot(container(slot)) && !GrindstoneResultCalculator.accepts(item)) {
            throw new IllegalArgumentException("Item cannot be placed in a grindstone input slot");
        }

        if (screen instanceof CloudEnchantingContainerScreen enchanting && !enchanting.acceptsItem(container(slot), item)) {
            throw new IllegalArgumentException("Item cannot be placed in that enchanting table slot");
        }
    }

    private void onFurnaceResultTaken(ItemStackRequestSlotData slot, ItemStack item) {
        if (screen instanceof CloudFurnaceContainerScreen furnace && container(slot) == ContainerSlotType.FURNACE_RESULT) {
            furnace.furnaceEntity().extract(player, item);
        }
    }

    private void rejectResultMutation(ItemStackRequestSlotData slot) {
        if (screen instanceof ResultContainerScreen result && result.isResultSlot(container(slot), slot.getSlot())) {
            throw new IllegalArgumentException("A prepared result must be taken with its inputs");
        }
    }

    private ActionType actionType(TransferItemStackRequestAction action) {
        return switch (action.getType()) {
            case TAKE -> ActionType.TAKE;
            case PLACE -> ActionType.PLACE;
            default -> ActionType.UNKNOWN;
        };
    }

    private ClickType clickType(TransferItemStackRequestAction action, ItemStack sourceItem, int count) {
        return switch (action.getType()) {
            case TAKE -> takeClickType(sourceItem, count);
            case PLACE -> placeClickType(sourceItem, count);
            default -> ClickType.UNKNOWN;
        };
    }

    private ClickType takeClickType(ItemStack sourceItem, int count) {
        if (count == sourceItem.getCount()) {
            return ClickType.TAKE_ALL;
        }

        if (count == sourceItem.getCount() / 2) {
            return ClickType.TAKE_HALF;
        }

        return ClickType.TAKE_SOME;
    }

    private ClickType placeClickType(ItemStack sourceItem, int count) {
        if (count == sourceItem.getCount()) {
            return ClickType.PLACE_ALL;
        }

        if (count == 1) {
            return ClickType.PLACE_ONE;
        }

        return ClickType.PLACE_SOME;
    }

    private boolean rejectClick(InventoryClickEvent event) {
        this.player.getServer().getEventManager().fire(event);
        if (event.isCancelled() || this.screen != this.player.getInventoryManager().getScreen()) {
            this.requestFailed = true;
            return true;
        }
        return false;
    }

    private static boolean isMoveBlocked(ItemStack item, ContainerSlotType source, ContainerSlotType destination) {
        ItemLockMode lockMode = item.get(ItemDataComponents.ITEM_LOCK);
        if (lockMode == null) {
            return false;
        }

        if (lockMode == ItemLockMode.LOCK_IN_SLOT) {
            return true;
        }

        return isOutsidePlayerInventory(source) || isOutsidePlayerInventory(destination);
    }

    private static boolean isOutsidePlayerInventory(ContainerSlotType containerType) {
        return switch (containerType) {
            case ARMOR, CURSOR, HOTBAR, HOTBAR_AND_INVENTORY, INVENTORY, OFFHAND -> false;
            default -> true;
        };
    }

    public static ContainerSlotType container(ItemStackRequestSlotData slotData) {
        FullContainerName containerName = slotData.getContainerName();
        if (containerName == null) {
            throw new IllegalArgumentException("Item stack request slot is missing containerName");
        }
        return containerName.getContainer();
    }

    public static @Nullable String renameText(ItemStackRequest request) {
        String[] strings = request.getFilterStrings();
        if (strings == null || strings.length == 0) {
            return null;
        }

        int index = strings.length == 1 ? 0 : -1;
        for (ItemStackRequestAction action : request.getActions()) {
            if (action instanceof CraftRecipeOptionalAction optional) {
                index = optional.getFilteredStringIndex();
                break;
            }
        }

        if (index < 0 || index >= strings.length) {
            throw new IllegalArgumentException("Invalid rename string index");
        }

        return Objects.requireNonNull(strings[index], "rename text");
    }

    private ResolvedRequestSlot resolveSlot(ItemStackRequestSlotData slotData) {
        return resolveSlot(container(slotData), slotData.getSlot());
    }

    private ResolvedRequestSlot resolveSlot(ContainerSlotType containerType, int protocolSlot) {
        SlotGroup slotGroup = this.screen.resolveSlotGroup(containerType);
        if (slotGroup == null) {
            throw new IllegalArgumentException("Screen for slot type " + containerType + " not found");
        }

        int inventorySlot = this.screen.resolveInventorySlot(containerType, protocolSlot);
        if (inventorySlot < 0) {
            throw new IllegalArgumentException("Slot " + protocolSlot + " is not valid for slot type " + containerType);
        }

        return new ResolvedRequestSlot(slotGroup, inventorySlot, containerType, protocolSlot);
    }

    private ItemStack getSlot(ItemStackRequestSlotData slotData) {
        ResolvedRequestSlot slot = this.resolveSlot(slotData);
        this.checkStationSlot(slot);
        return this.changes.read(slot.slotGroup(), slot.inventorySlot());
    }

    private void setSlot(ItemStackRequestSlotData slotData, ItemStack item) {
        this.stageSlot(container(slotData), slotData.getSlot(), item);
    }

    private void stageSlot(ContainerSlotType containerType, int protocolSlot, ItemStack item) {
        ResolvedRequestSlot slot = this.resolveSlot(containerType, protocolSlot);
        this.checkStationSlot(slot);
        this.writeSlot(slot, item);
        this.trackAffectedSlot(containerType, protocolSlot);
        if (this.screen instanceof ResultContainerScreen resultScreen && resultScreen.isInputSlot(containerType)) {
            resultScreen.trackResultSlots(this::trackAffectedSlot);
            this.requiresFullSlotUpdate = true;
        }
    }

    private void checkStationSlot(ResolvedRequestSlot slot) {
        if (this.stationSlots.contains(slot.key())) {
            throw new IllegalArgumentException("Slot is reserved for station result collection");
        }
    }

    private void writeSlot(ResolvedRequestSlot slot, ItemStack item) {
        this.changes.write(slot.slotGroup(), slot.inventorySlot(), item,
                value -> this.screen.setSlot(slot.containerType(), slot.protocolSlot(), value));
    }

    private void trackAffectedSlot(ContainerSlotType containerType, int slot) {
        this.affectedSlots.computeIfAbsent(containerType, _ -> new LinkedHashSet<>()).add(slot);
    }

    private record ResolvedRequestSlot(SlotGroup slotGroup, int inventorySlot, ContainerSlotType containerType, int protocolSlot) {
        public InventorySlotChanges.SlotKey key() {
            return InventorySlotChanges.key(this.slotGroup, this.inventorySlot);
        }
    }

    private record PendingResult(ResultContainerScreen screen, ResolvedRequestSlot slot, ItemStack source,
                                 ItemStack destination, ItemStack predicted, @Nullable ItemStack proposed,
                                 @Nullable ResultQuote quote, int count) {
    }
}
