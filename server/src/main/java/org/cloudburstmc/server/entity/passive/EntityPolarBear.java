package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.PolarBear;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

/**
 * @author PikyCZ
 */
public class EntityPolarBear extends Animal implements PolarBear {

    public EntityPolarBear(EntityType<PolarBear> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.65f;
        }
        return 1.3f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.7f;
        }
        return 1.4f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(30);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{CloudItemRegistry.get().getItem(ItemTypes.FISH), CloudItemRegistry.get().getItem(ItemTypes.SALMON)};
    }
}
