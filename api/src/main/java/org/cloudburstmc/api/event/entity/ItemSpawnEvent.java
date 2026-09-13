package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.misc.DroppedItem;

import static java.util.Objects.requireNonNull;

/**
 * Fired before a dropped item is added to a level.
 */
public class ItemSpawnEvent extends EntitySpawnEvent {

    /**
     * Creates an item spawn event.
     *
     * @param item the dropped item
     */
    public ItemSpawnEvent(DroppedItem item) {
        super(requireNonNull(item, "item"));
    }

    /**
     * Returns the dropped item being spawned.
     *
     * @return the dropped item
     */
    @Override
    public DroppedItem getEntity() {
        return (DroppedItem) this.entity;
    }
}
