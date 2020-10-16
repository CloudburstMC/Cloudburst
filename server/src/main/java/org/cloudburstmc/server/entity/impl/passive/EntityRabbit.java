package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Rabbit;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

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
        return new ItemStack[]{ItemStack.get(ItemTypes.RABBIT), ItemStack.get(ItemTypes.RABBIT_HIDE), ItemStack.get(ItemTypes.RABBIT_FOOT)};
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        setMaxHealth(10);
    }
}
