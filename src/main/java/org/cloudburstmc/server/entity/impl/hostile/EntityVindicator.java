package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Vindicator;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityVindicator extends EntityHostile implements Vindicator {

    public EntityVindicator(EntityType<Vindicator> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(24);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.95f;
    }

    @Override
    public String getName() {
        return "Vindicator";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.get(ItemTypes.IRON_AXE)};
    }
}
