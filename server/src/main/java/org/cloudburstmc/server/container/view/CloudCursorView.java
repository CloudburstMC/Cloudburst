package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.CursorView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Slot group implementation for the single cursor slot, backed by a per-player container.
 */
public class CloudCursorView extends CloudPlayerContainerView implements CursorView {

    public CloudCursorView(CloudPlayer holder) {
        super(SlotGroupTypes.CURSOR, holder, new CloudContainer(1));
    }

    @Override
    public ItemStack getCursor() {
        return container.getItem(0);
    }

    @Override
    public void setCursor(ItemStack itemStack) {
        this.container.setItem(0, itemStack);
    }
}
