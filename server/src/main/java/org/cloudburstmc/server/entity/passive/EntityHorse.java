package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Horse;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntityHorse extends EntityAbstractHorse implements Horse {

    public EntityHorse(EntityType<Horse> type, Location location) {
        super(type, location);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.builder().itemType(ItemTypes.LEATHER).build()};
    }
}
