package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.CraftingView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * View-layer slot group for a crafting grid (2×2 = 4 slots).
 * Attached to a player — the crafting grid is always part of the player's UI.
 */
public class CloudCraftingView extends CloudPlayerContainerView implements CraftingView {

    public CloudCraftingView(CloudPlayer holder) {
        super(SlotGroupTypes.CRAFTING, holder, new CloudContainer(4));
    }
}
