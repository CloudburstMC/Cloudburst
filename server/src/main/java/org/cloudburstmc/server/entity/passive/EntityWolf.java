package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Wolf;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityWolf extends EntityTameable implements Wolf {

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
    protected String getDefaultName() {
        return "Wolf";
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(8);
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        return false; //only certain food
    }
}
