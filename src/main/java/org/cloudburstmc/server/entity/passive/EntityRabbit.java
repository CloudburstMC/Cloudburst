package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Rabbit;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityRabbit extends Animal implements Rabbit {

    public EntityRabbit(EntityType<Rabbit> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.2f;
        }
        return 0.4f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.25f;
        }
        return 0.5f;
    }

    @Override
    public String getName() {
        return "Rabbit";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{CloudItemRegistry.get().getItem(ItemTypes.RABBIT), CloudItemRegistry.get().getItem(ItemTypes.RABBIT_HIDE), CloudItemRegistry.get().getItem(ItemTypes.RABBIT_FOOT)};
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        setMaxHealth(10);
    }
}
