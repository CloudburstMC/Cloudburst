package org.cloudburstmc.server.network.inventory;

import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.server.inventory.screen.InventoryScreen;

public class ItemStackRequestActionHandler {


    private InventoryScreen screen;

    public void handleAction(ItemStackRequestAction action) {
        switch (action.getType()) {
            case TAKE -> handleTake((TakeAction) action);
            case PLACE -> handlePlace((PlaceAction) action);
            case SWAP -> handleSwap((SwapAction) action);
            case DROP -> handleDrop((DropAction) action);
            case DESTROY -> handleDestroy((DestroyAction) action);
            case PLACE_IN_ITEM_CONTAINER -> handlePlaceInItemContainer((PlaceAction) action);
            case TAKE_FROM_ITEM_CONTAINER -> handleTakeFromItemContainer((TakeAction) action);
        }
    }

    protected void handleTake(TakeAction action) {

    }

    protected void handlePlace(PlaceAction action) {

    }

    protected void handleSwap(SwapAction action) {

    }

    protected void handleDrop(DropAction action) {
        ItemStackRequestSlotData source = action.getSource();
    }

    protected void handleDestroy(DestroyAction action) {

    }

    protected void handlePlaceInItemContainer(PlaceAction action) {

    }

    protected void handleTakeFromItemContainer(TakeAction action) {

    }

    public void beginRequest(ItemStackRequest request, InventoryScreen screen) {
        this.screen = screen;
    }


    public ItemStackResponse endRequest() {
        this.screen = null;
        return null;
    }

    public void addFilteredStrings(int requestId, String[] filterStrings) {
    }
}
