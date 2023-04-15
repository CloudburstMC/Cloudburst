package org.cloudburstmc.server.network.inventory;

import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.server.inventory.screen.CloudInventoryScreen;

@Slf4j
public class ItemStackRequestActionHandler {


    private CloudInventoryScreen screen;

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
        ItemStack sourceItem = getAndVerifySlot(action.getSource());
        if (sourceItem == ItemStack.EMPTY) {
            throw new IllegalArgumentException("Source item stack is empty");
        }

        ItemStack targetItem = getAndVerifySlot(action.getDestination());

        ItemStack newSourceItem = sourceItem.decreaseCount(action.getCount());
        ItemStack newTargetItem = targetItem.increaseCount(action.getCount());

        this.screen.setSlot(action.getSource().getContainer(), action.getSource().getSlot(), newSourceItem);
        log.info("Source: " + newSourceItem);
        this.screen.setSlot(action.getDestination().getContainer(), action.getDestination().getSlot(), newTargetItem);
        log.info("Target: " + newTargetItem);
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

    public void beginRequest(ItemStackRequest request, CloudInventoryScreen screen) {
        this.screen = screen;
    }


    public ItemStackResponse endRequest() {
        this.screen = null;
        return null;
    }

    public void addFilteredStrings(int requestId, String[] filterStrings) {
    }

    private ItemStack getAndVerifySlot(ItemStackRequestSlotData slot) {
        ItemStack clientSide = NetworkItemStack.getItemStack(slot.getStackNetworkId());
        ItemStack serverSide = this.screen.getSlot(slot.getContainer(), slot.getSlot());

        if (clientSide.equals(serverSide)) {
            throw new IllegalArgumentException("Client-side and server-side item stacks do not match");
        }
        return serverSide;
    }
}
