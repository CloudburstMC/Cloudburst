package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.AbstractCow;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

public abstract class EntityAbstractCow extends EntityBreedableAnimal implements AbstractCow {

    protected EntityAbstractCow(EntityType<? extends AbstractCow> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(10);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.45f : 0.9f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.7f : 1.4f;
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.from(ItemTypes.LEATHER), ItemStack.from(ItemTypes.BEEF)};
    }
}
