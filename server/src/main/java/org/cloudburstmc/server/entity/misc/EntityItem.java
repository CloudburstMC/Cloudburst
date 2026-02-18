package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.level.Location;

public class EntityItem extends EntityDroppedItem {

    public EntityItem(EntityType<DroppedItem> type, Location location) {
        super(type, location);
    }
}
