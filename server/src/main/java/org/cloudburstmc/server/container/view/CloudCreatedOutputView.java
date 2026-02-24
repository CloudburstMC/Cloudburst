package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.CreatedOutputView;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ServerSlotGroupTypes;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Slot group implementation for the ephemeral single-slot crafting output. Held by a player
 * and represents the item produced by the current crafting recipe before it is taken.
 */
public class CloudCreatedOutputView extends CloudSlotGroupBase implements CreatedOutputView {

    private final CloudPlayer player;

    public CloudCreatedOutputView(CloudPlayer player) {
        super(ServerSlotGroupTypes.CREATED_OUTPUT, new CloudContainer(1));
        this.player = player;
    }

    @Override
    public Player getHolder() {
        return this.player;
    }
}
