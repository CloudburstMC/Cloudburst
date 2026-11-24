package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Skeleton;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntitySkeleton extends EntityAbstractSkeleton implements Skeleton {

    public EntitySkeleton(EntityType<Skeleton> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
    }

    @Override
    public float getHeight() {
        return 1.99f;
    }

    @Override
    protected String getDefaultName() {
        return "Skeleton";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.builder().itemType(ItemTypes.BONE).build(), ItemStack.builder().itemType(ItemTypes.ARROW).build()};
    }


}
