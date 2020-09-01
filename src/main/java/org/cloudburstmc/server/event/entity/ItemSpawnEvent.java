package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.misc.DroppedItem;
import org.cloudburstmc.server.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemSpawnEvent extends EntityEvent implements Cancellable {

    public ItemSpawnEvent(DroppedItem item) {
        this.entity = item;
    }

    @Override
    public DroppedItem getEntity() {
        return (DroppedItem) this.entity;
    }
}
