package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Squid;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

public class EntitySquid extends EntityWaterAnimal implements Squid {

    public EntitySquid(EntityType<? extends Squid> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    public float getWidth() {
        return 0.8f;
    }

    @Override
    public float getHeight() {
        return 0.8f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(10);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.from(this.getInkSacType())};
    }

    protected ItemType getInkSacType() {
        return ItemTypes.INK_SAC;
    }
}
