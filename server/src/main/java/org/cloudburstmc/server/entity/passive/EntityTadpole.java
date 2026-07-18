package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Tadpole;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

public class EntityTadpole extends EntityWaterAnimal implements Tadpole {

    public EntityTadpole(EntityType<Tadpole> type, Location location) {
        super(type, location);
    }

    @Override
    public ItemStack getBaseBucketItem() {
        return ItemStack.from(ItemTypes.TADPOLE_BUCKET);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(6);
    }

    @Override
    public float getWidth() {
        return 0.4f;
    }

    @Override
    public float getHeight() {
        return 0.3f;
    }

    @Override
    public String getName() {
        return "Tadpole";
    }
}
