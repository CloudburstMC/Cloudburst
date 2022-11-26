package org.cloudburstmc.server.network.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.PlaceStackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.TakeStackRequestActionData;

public class ItemStackRequestActionHandler {

    public void handleAction(StackRequestActionData action) {
        switch (action.getType()) {
            case TAKE -> handle((TakeStackRequestActionData) action);
            case PLACE -> handle((PlaceStackRequestActionData) action);
        }
    }

    private void handle(TakeStackRequestActionData action) {

    }

    private void handle(PlaceStackRequestActionData action) {

    }

}
