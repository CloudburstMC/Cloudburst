package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.misc.DroppedItem;
import org.cloudburstmc.server.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemDespawnEvent extends EntityEvent implements Cancellable {

    public ItemDespawnEvent(DroppedItem item) {
        this.entity = item;
    }

    @Override
    public DroppedItem getEntity() {
        return (DroppedItem) this.entity;
    }
}
