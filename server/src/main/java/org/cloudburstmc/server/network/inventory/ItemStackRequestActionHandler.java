package org.cloudburstmc.server.network.inventory;

import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.PlaceAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TakeAction;

public class ItemStackRequestActionHandler {

    public void handleAction(ItemStackRequestAction action) {
        switch (action.getType()) {
            case TAKE -> handle((TakeAction) action);
            case PLACE -> handle((PlaceAction) action);
        }
    }

    private void handle(TakeAction action) {

    }

    private void handle(PlaceAction action) {

    }

}
