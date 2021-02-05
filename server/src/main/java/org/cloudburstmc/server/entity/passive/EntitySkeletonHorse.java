package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Smiteable;
import org.cloudburstmc.api.entity.passive.SkeletonHorse;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.item.ItemTypes;

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
        return new ItemStack[]{ItemStack.get(ItemTypes.BONE)};
    }

    @Override
    public boolean isUndead() {
        return true;
    }

}
