package org.cloudburstmc.server.network.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ItemStackRequest;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

@Log4j2
public class ItemStackNetManager {

    private final Queue<ItemStackRequest> requests = new ArrayDeque<>();
    private final Deque<Object> screenStack = new ArrayDeque<>();
    private TextFilterState textFilterState;
    private long textFilterRequestTick;
    private long textFilterRequestTimeout;
    private boolean currentRequestIsCrafting;

    public void handleRequestBatch(Object requestBatch) {

    }

    private void queueRequests(Object requestBatch) {

    }

    private void processQueue() {

    }

    private void handleRequestData(ItemStackRequest request, List<ItemStackResponsePacket.Response> response) {
        if (!NetIds.isValid(request.getRequestId())) {
            return;
        }

        Object screen = screenStack.getLast();
        if (screen == null) {
            return;
        }

//        ItemStackResponsePacket.Response response = new ItemStackResponsePacket.Response(
//                ItemStackResponsePacket.ResponseStatus.OK,
//                request.getRequestId(),
//
//        );
        for (StackRequestActionData action : request.getActions()) {
            if (!isRequestActionAllowed(action)) {
                break;
            }
        }
    }

    private boolean isRequestActionAllowed(StackRequestActionData action) {
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

    private void getScreenStack() {

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
}
