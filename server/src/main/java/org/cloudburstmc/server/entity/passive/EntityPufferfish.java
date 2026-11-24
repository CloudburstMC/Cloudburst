package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Pufferfish;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

/**
 * Created by PetteriM1
 */
public class EntityPufferfish extends EntityWaterAnimal implements Pufferfish {

    public EntityPufferfish(EntityType<Pufferfish> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    public ItemStack getBaseBucketItem() {
        return ItemStack.from(ItemTypes.PUFFERFISH_BUCKET);
    }

    protected String getDefaultName() {
        return "Pufferfish";
    }

    @Override
    public float getWidth() {
        return 0.35f;
    }

    @Override
    public float getHeight() {
        return 0.35f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(3);
    }
}
