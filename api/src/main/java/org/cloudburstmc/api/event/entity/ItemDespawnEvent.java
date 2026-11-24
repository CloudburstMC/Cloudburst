package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Location;

import static java.util.Objects.requireNonNull;

/**
 * Fired before a dropped item is removed because its lifetime has expired.
 * Canceling lets the item remain and restarts its lifetime.
 */
public class ItemDespawnEvent extends EntityEvent implements Cancellable {

    private final Location location;

    /**
     * @param item     the item that is expiring
     * @param location the item's location when it expired
     */
    public ItemDespawnEvent(DroppedItem item, Location location) {
        this.entity = requireNonNull(item, "item");
        this.location = requireNonNull(location, "location");
    }

    @Override
    public DroppedItem getEntity() {
        return (DroppedItem) this.entity;
    }

    /**
     * Returns where the item was when it expired.
     */
    public Location getLocation() {
        return this.location;
    }
}
