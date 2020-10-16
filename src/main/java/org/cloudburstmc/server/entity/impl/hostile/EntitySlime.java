package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Slime;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntitySlime extends EntityHostile implements Slime {

    public EntitySlime(EntityType<Slime> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(16);
    }

    @Override
    public float getWidth() {
        return 2.04f;
    }

    @Override
    public float getHeight() {
        return 2.04f;
    }

    @Override
    public String getName() {
        return "Slime";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.get(ItemTypes.SLIME_BALL)};
    }
}
