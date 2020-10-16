package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Ocelot;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityOcelot extends Animal implements Ocelot {

    public EntityOcelot(EntityType<Ocelot> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.3f;
        }
        return 0.6f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.35f;
        }
        return 0.7f;
    }

    @Override
    public String getName() {
        return "Ocelot";
    }

    @Override
    public void initEntity() {
        super.initEntity();
        setMaxHealth(10);
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        return item.getType() == ItemTypes.FISH;
    }
}
