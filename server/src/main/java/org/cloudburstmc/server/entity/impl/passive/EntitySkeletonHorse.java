package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Smiteable;
import org.cloudburstmc.server.entity.passive.SkeletonHorse;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntitySkeletonHorse extends Animal implements SkeletonHorse, Smiteable {

    public EntitySkeletonHorse(EntityType<SkeletonHorse> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 1.4f;
    }

    @Override
    public float getHeight() {
        return 1.6f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(15);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.get(ItemIds.BONE)};
    }

    @Override
    public boolean isUndead() {
        return true;
    }

}
