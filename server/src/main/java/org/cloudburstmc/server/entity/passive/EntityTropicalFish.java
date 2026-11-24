package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.TropicalFish;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

/**
 * Created by PetteriM1
 */
public class EntityTropicalFish extends EntityWaterAnimal implements TropicalFish {

    public EntityTropicalFish(EntityType<TropicalFish> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    public ItemStack getBaseBucketItem() {
        return ItemStack.from(ItemTypes.TROPICAL_FISH_BUCKET);
    }

    protected String getDefaultName() {
        return "Tropical Fish";
    }

    @Override
    public float getWidth() {
        return 0.5f;
    }

    @Override
    public float getHeight() {
        return 0.4f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(3);
    }
}
