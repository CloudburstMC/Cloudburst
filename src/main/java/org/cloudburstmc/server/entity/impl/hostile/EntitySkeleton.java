package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Smiteable;
import org.cloudburstmc.server.entity.hostile.Skeleton;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntitySkeleton extends EntityHostile implements Skeleton, Smiteable {

    public EntitySkeleton(EntityType<Skeleton> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.99f;
    }

    @Override
    public String getName() {
        return "Skeleton";
    }

    @Override
    public Item[] getDrops() {
        return new Item[]{Item.get(ItemIds.BONE), Item.get(ItemIds.ARROW)};
    }

    @Override
    public boolean isUndead() {
        return true;
    }

}
