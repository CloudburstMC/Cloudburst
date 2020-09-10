package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Wolf;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Location;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityWolf extends Animal implements Wolf {

    public EntityWolf(EntityType<Wolf> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 0.85f;
    }

    @Override
    public String getName() {
        return "Wolf";
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(8);
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        return false; //only certain food
    }
}
