package org.cloudburstmc.server.network.inventory;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.TextProcessingEventOrigin;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.LegacySetItemSlotData;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackResponsePacket;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.container.view.CloudSlotGroupBase;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.*;

/**
 * Per-player manager for {@code ItemStackRequestPacket} processing. Queues incoming requests,
 * manages the player's open screen stack, handles text-filter state, and dispatches each
 * request to {@link ItemStackRequestActionHandler} for slot mutation and response construction.
 */
@Log4j2
public class ItemStackNetManager {

    private static final long TEXT_FILTER_TIMEOUT_TICKS = 100;

    private final CloudPlayer player;
    private final Queue<ItemStackRequest> requests = new ArrayDeque<>();
    private final Deque<CloudInventoryScreen> screenStack = new ArrayDeque<>();
    private final ItemStackRequestActionHandler handler;
    private final Deque<ServerAuthoritativeInventoryUpdate> serverAuthoritativeUpdates = new ArrayDeque<>();
    private TextFilterState textFilterState;
    private long textFilterRequestTick;
    private long textFilterRequestTimeout;
    private boolean currentRequestIsCrafting;

    public ItemStackNetManager(CloudPlayer player) {
        this.player = player;
        this.handler = new ItemStackRequestActionHandler(player);
    }

    public void handlePacket(ItemStackRequestPacket packet) {
        for (ItemStackRequest request : packet.getRequests()) {
            this.requests.offer(request);
        }
        processQueue();
    }

    public void handleSingleRequest(ItemStackRequest request) {
        this.requests.offer(request);
        processQueue();
    }

    public void acknowledgeLegacyTransaction(int requestId, List<LegacySetItemSlotData> legacySlots) {
        if (requestId == 0 || legacySlots.isEmpty()) {
            return;
        }

        ItemStackResponse response = this.handler.acknowledgeLegacyCurrentState(requestId, legacySlots);
        ItemStackResponsePacket packet = new ItemStackResponsePacket();
        packet.getEntries().add(response);
        this.player.sendPacket(packet);
    }

    private void processQueue() {
        List<ItemStackResponse> responses = new ArrayList<>();
        if (!this.requests.isEmpty()) {
            ItemStackRequest request;
            while (this.textFilterState != TextFilterState.WAITING && (request = this.requests.poll()) != null) {
                if (tryFilterText(request)) {
                    break;
                }

                handleRequest(request, responses);
            }
        }

        if (!responses.isEmpty()) {
            ItemStackResponsePacket packet = new ItemStackResponsePacket();
            packet.getEntries().addAll(responses);
            this.player.sendPacket(packet);
        }

        if (this.requests.isEmpty()) {
            if (this.textFilterState != TextFilterState.NONE) {
                this.textFilterState = TextFilterState.NONE;
            }

            // TODO: Container close callback
        }
    }

    private boolean tryFilterText(ItemStackRequest request) {
        if (this.textFilterState == TextFilterState.WAITING) {
            log.debug("Already filtering text for {} {}", player.getName(), request);
        } else if (this.textFilterState != TextFilterState.TIMED_OUT && request.getFilterStrings().length != 0) {
            this.filterStrings(request.getRequestId(), request.getFilterStrings(), request.getTextProcessingEventOrigin());
            return this.textFilterState == TextFilterState.WAITING;
        } else if (request.getFilterStrings().length != 0) {
            this.handler.addFilteredStrings(request.getRequestId(), request.getFilterStrings());
        }
        return false;
    }

    private void filterStrings(int requestId, String[] strings, TextProcessingEventOrigin eventOrigin) {
        // TODO: Expose API for this at some point. When a real async filter is wired in, set
        //       this.textFilterState = TextFilterState.WAITING here and arm the timeout clock below.
        //       For now, strings pass through unfiltered immediately.
        this.textFilterRequestTick = 0;
        this.textFilterRequestTimeout = TEXT_FILTER_TIMEOUT_TICKS;
        this.handler.addFilteredStrings(requestId, strings);
    }

    private void handleRequest(ItemStackRequest request, List<ItemStackResponse> responses) {
        if (this.textFilterState == TextFilterState.WAITING || !NetIds.isValid(request.getRequestId())) {
            return;
        }

        CloudInventoryScreen screen = this.screenStack.peekLast();
        if (screen == null) {
            log.debug("Received request {} with no open screen", request.getRequestId());
            return;
        }

        ItemStackResponse authoritativeResponse = acknowledgeServerAuthoritativeUpdate(request, screen);
        if (authoritativeResponse != null) {
            responses.add(authoritativeResponse);
            return;
        }

        this.currentRequestIsCrafting = false;
        this.handler.beginRequest(request, screen);

        for (ItemStackRequestAction action : request.getActions()) {
            if (!isRequestActionAllowed(action)) {
                break;
            }

            handler.handleAction(action);
        }

        this.currentRequestIsCrafting = false;
        responses.add(this.handler.endRequest());
    }

    private ItemStackResponse acknowledgeServerAuthoritativeUpdate(ItemStackRequest request, CloudInventoryScreen screen) {
        while (!this.serverAuthoritativeUpdates.isEmpty() && this.serverAuthoritativeUpdates.peekFirst().isExpired()) {
            this.serverAuthoritativeUpdates.removeFirst();
        }

        Iterator<ServerAuthoritativeInventoryUpdate> iterator = this.serverAuthoritativeUpdates.iterator();
        while (iterator.hasNext()) {
            ServerAuthoritativeInventoryUpdate update = iterator.next();
            if (update.matches(request)) {
                iterator.remove();
                return this.handler.acknowledgeCurrentState(request, screen);
            }
        }
        return null;
    }

    private boolean isRequestActionAllowed(ItemStackRequestAction action) {
        switch (action.getType()) {
            case TAKE:
            case PLACE:
            case DROP:
            case DESTROY:
            case PLACE_IN_ITEM_CONTAINER:
            case TAKE_FROM_ITEM_CONTAINER:
            case LAB_TABLE_COMBINE:
            case BEACON_PAYMENT:
            case MINE_BLOCK:
                return true;
            case SWAP:
            case CRAFT_RECIPE:
            case CRAFT_RECIPE_AUTO:
            case CRAFT_CREATIVE:
            case CRAFT_RECIPE_OPTIONAL:
            case CRAFT_REPAIR_AND_DISENCHANT:
            case CRAFT_LOOM:
            case CRAFT_NON_IMPLEMENTED_DEPRECATED:
                if (this.currentRequestIsCrafting) {
                    return false;
                }
                this.currentRequestIsCrafting = true;
                return true;
            case CONSUME:
            case CREATE:
            case CRAFT_RESULTS_DEPRECATED:
                return this.currentRequestIsCrafting;
            default:
                log.debug("Received unhandled request action type {}", action.getType());
                return false;
        }
    }

    public void pushScreen(CloudInventoryScreen screen) {
        this.screenStack.addLast(screen);
    }

    public CloudInventoryScreen popScreen() {
        return this.screenStack.removeLast();
    }

    public CloudInventoryScreen getScreen() {
        return this.screenStack.peekLast();
    }

    public void recordServerAuthoritativeArmorUse(int armorSlot) {
        this.serverAuthoritativeUpdates.addLast(ServerAuthoritativeInventoryUpdate.armorUse(armorSlot));
        while (this.serverAuthoritativeUpdates.size() > 8) {
            this.serverAuthoritativeUpdates.removeFirst();
        }
    }

    public Set<Container> getAllInventories() {
        Set<Container> inventories = new HashSet<>();
        for (CloudInventoryScreen screen : this.screenStack) {
            for (SlotGroup view : screen.getAllSlotGroups()) {
                if (view instanceof CloudSlotGroupBase section) {
                    inventories.add(section.getContainer());
                } else if (view instanceof ContainerBlockEntity blockEntity) {
                    inventories.add(blockEntity.getContainer());
                }
            }
        }
        return inventories;
    }

    public void tick() {
        switch (textFilterState) {
            case NONE:
            case TIMED_OUT:
                processQueue();
                break;
            case WAITING:
                if (textFilterRequestTick >= textFilterRequestTimeout) {
                    this.textFilterState = TextFilterState.TIMED_OUT;
                    processQueue();
                } else {
                    textFilterRequestTick++;
                }
                break;
        }
    }

    private record ServerAuthoritativeInventoryUpdate(Set<SlotRef> triggerSlots, long deadline) {
        private static final long RESPONSE_WINDOW_NANOS = 2_000_000_000L;

        private static ServerAuthoritativeInventoryUpdate armorUse(int armorSlot) {
            Set<SlotRef> triggerSlots = new LinkedHashSet<>();
            triggerSlots.add(new SlotRef(ContainerSlotType.ARMOR, armorSlot));
            return new ServerAuthoritativeInventoryUpdate(triggerSlots, System.nanoTime() + RESPONSE_WINDOW_NANOS);
        }

        private boolean isExpired() {
            return System.nanoTime() > this.deadline;
        }

        private boolean matches(ItemStackRequest request) {
            for (ItemStackRequestAction action : request.getActions()) {
                if (matches(action)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matches(ItemStackRequestAction action) {
            return switch (action) {
                case TransferItemStackRequestAction transfer -> matches(transfer.getSource()) || matches(transfer.getDestination());
                case SwapAction swap -> matches(swap.getSource()) || matches(swap.getDestination());
                case DropAction drop -> matches(drop.getSource());
                case DestroyAction destroy -> matches(destroy.getSource());
                case ConsumeAction consume -> matches(consume.getSource());
                default -> false;
            };
        }

        private boolean matches(ItemStackRequestSlotData slotData) {
            try {
                return this.triggerSlots.contains(new SlotRef(ItemStackRequestActionHandler.container(slotData), slotData.getSlot()));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        private record SlotRef(ContainerSlotType container, int slot) {
        }
    }
}
