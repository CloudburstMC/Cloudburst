package org.cloudburstmc.server.entity.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.component.PickItemEntityHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;

@UtilityClass
public class PickItemEntityHandlers {
    public static final PickItemEntityHandler BOAT = (entity, includeData) -> ItemStack.from(((EntityBoat) entity).getBoatItem());
}
