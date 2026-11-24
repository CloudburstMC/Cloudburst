package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Cod;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

/**
 * Created by PetteriM1
 */
public class EntityCod extends EntityWaterAnimal implements Cod {

    public EntityCod(EntityType<Cod> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    public ItemStack getBaseBucketItem() {
        return ItemStack.from(ItemTypes.COD_BUCKET);
    }

    protected String getDefaultName() {
        return "Cod";
    }

    @Override
    public float getWidth() {
        return 0.5f;
    }

    @Override
    public float getHeight() {
        return 0.2f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(3);
    }
}
