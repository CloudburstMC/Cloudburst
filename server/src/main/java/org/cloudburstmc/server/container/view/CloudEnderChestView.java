package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.EnderChestView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Slot group implementation for a player's personal 27-slot ender chest, delegating to the
 * player's own persistent ender chest container.
 */
public class CloudEnderChestView extends CloudPlayerContainerView implements EnderChestView {

    public CloudEnderChestView(CloudPlayer holder) {
        super(SlotGroupTypes.ENDER_CHEST, holder, holder.getEnderChestContainer());
    }
}
