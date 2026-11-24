package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Slime;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.EntityAbstractCubeMob;

/**
 * @author PikyCZ
 */
public class EntitySlime extends EntityAbstractCubeMob implements Slime {

    public EntitySlime(EntityType<Slime> type, Location location) {
        super(type, location, 4);
    }

    @Override
    protected String getDefaultName() {
        return "Slime";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.builder().itemType(ItemTypes.SLIME_BALL).build()};
    }
}
