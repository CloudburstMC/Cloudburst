package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.misc.DroppedItem;

import static java.util.Objects.requireNonNull;

/**
 * Called before a dropped item is added to a level.
 */
public class ItemSpawnEvent extends EntitySpawnEvent {

    public ItemSpawnEvent(DroppedItem item) {
        super(requireNonNull(item, "item"));
    }

    @Override
    public DroppedItem getEntity() {
        return (DroppedItem) this.entity;
    }
}
