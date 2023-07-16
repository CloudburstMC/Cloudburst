package org.cloudburstmc.server.network.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.container.Container;
import org.cloudburstmc.api.container.view.ContainerView;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.TextProcessingEventOrigin;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackResponsePacket;
import org.cloudburstmc.server.container.screen.CloudContainerScreen;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.*;

@Log4j2
@RequiredArgsConstructor
public class ItemStackNetManager {

    private final CloudPlayer player;
    private final Queue<ItemStackRequest> requests = new ArrayDeque<>();
    private final Deque<CloudContainerScreen> screenStack = new ArrayDeque<>();
    private TextFilterState textFilterState;
    private long textFilterRequestTick;
    private long textFilterRequestTimeout;
    private boolean currentRequestIsCrafting;
    private ItemStackRequestActionHandler handler = new ItemStackRequestActionHandler();

    public void handlePacket(ItemStackRequestPacket packet) {
        for (ItemStackRequest request : packet.getRequests()) {
            this.requests.offer(request);
        }
        processQueue();
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
        // TODO: Expose API for this at some point.
        this.handler.addFilteredStrings(requestId, strings);
    }

    private void handleRequest(ItemStackRequest request, List<ItemStackResponse> responses) {
        if (this.textFilterState == TextFilterState.WAITING || !NetIds.isValid(request.getRequestId())) {
            return;
        }

        CloudContainerScreen screen = this.screenStack.peekLast();
        if (screen == null) {
            log.debug("Received request {} with no open screen", request.getRequestId());
            return;
        }

        this.handler.beginRequest(request, screen);

        for (ItemStackRequestAction action : request.getActions()) {
            if (!isRequestActionAllowed(action)) {
                break;
            }

            handler.handleAction(action);
        }

        responses.add(this.handler.endRequest());
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
//            case TEST
                return true;
            case SWAP:
            case CRAFT_RECIPE:
            case CRAFT_RECIPE_AUTO:
            case CRAFT_CREATIVE:
            case CRAFT_RECIPE_OPTIONAL:
            case CRAFT_REPAIR_AND_DISENCHANT:
            case CRAFT_LOOM:
            case CRAFT_NON_IMPLEMENTED_DEPRECATED:
                return !this.currentRequestIsCrafting;
            case CONSUME:
            case CREATE:
            case CRAFT_RESULTS_DEPRECATED:
                return this.currentRequestIsCrafting;
            default:
                log.debug("Received unhandled request action type {}", action.getType());
                return false;
        }
    }

    public void pushScreen(CloudContainerScreen screen) {
        this.screenStack.addLast(screen);
    }

    public CloudContainerScreen popScreen() {
        return this.screenStack.removeLast();
    }

    public CloudContainerScreen getScreen() {
        return this.screenStack.peekLast();
    }

    public Set<Container> getAllInventories() {
        Set<Container> inventories = new HashSet<>();
        for (CloudContainerScreen screen : this.screenStack) {
            for (ContainerView view : screen.getViews()) {
                inventories.add(view.getContainer());
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

                } else {
                    textFilterRequestTick++;
                }
                break;
        }
    }

    public void onContainerScreenOpen() {

    }
}
