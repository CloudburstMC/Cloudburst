package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.PlayerSlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Base slot group implementation for slot groups owned by a player. Adds a {@link CloudPlayer} holder
 * reference; used as the parent for cursor, hotbar, player inventory, ender chest, and offhand views.
 */
public class CloudPlayerContainerView extends CloudSlotGroupBase implements PlayerSlotGroup {

    protected final CloudPlayer holder;

    protected CloudPlayerContainerView(SlotGroupType<?> type, CloudPlayer holder, CloudContainer container) {
        super(type, container);
        this.holder = holder;
    }

    protected CloudPlayerContainerView(SlotGroupType<?> type, CloudPlayer holder, CloudContainer container, int size, int offset) {
        super(type, container, size, offset);
        this.holder = holder;
    }

    @Override
    public CloudPlayer getHolder() {
        return holder;
    }
}
