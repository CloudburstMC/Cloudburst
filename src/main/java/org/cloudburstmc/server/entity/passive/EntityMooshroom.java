package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Mooshroom;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.registry.CloudItemRegistry;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityMooshroom extends Animal implements Mooshroom {

    public EntityMooshroom(EntityType<Mooshroom> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (isBaby()) {
            return 0.45f;
        }
        return 0.9f;
    }

    @Override
    public float getHeight() {
        if (isBaby()) {
            return 0.7f;
        }
        return 1.4f;
    }

    @Override
    public String getName() {
        return "Mooshroom";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{CloudItemRegistry.get().getItem(ItemTypes.LEATHER), CloudItemRegistry.get().getItem(ItemTypes.BEEF)};
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        setMaxHealth(10);
    }
}
