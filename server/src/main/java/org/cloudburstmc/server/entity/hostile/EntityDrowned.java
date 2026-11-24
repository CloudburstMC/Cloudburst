package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Drowned;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

/**
 * Created by PetteriM1
 */
public class EntityDrowned extends EntityZombie implements Drowned {

    public EntityDrowned(EntityType<Drowned> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected String getDefaultName() {
        return "Drowned";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.builder().itemType(ItemTypes.ROTTEN_FLESH).build()};
    }
}
