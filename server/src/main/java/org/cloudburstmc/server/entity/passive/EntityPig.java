package org.cloudburstmc.server.entity.passive;

import lombok.val;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Pig;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.item.ItemTypes;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityPig extends Animal implements Pig {

    public static final int NETWORK_ID = 12;

    public EntityPig(EntityType<Pig> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.45f;
        }
        return 0.9f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.45f;
        }
        return 0.9f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(10);
    }

    @Override
    public String getName() {
        return "Pig";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{CloudItemRegistry.get().getItem(ItemTypes.PORKCHOP)};
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        val id = item.getType();

        return id == ItemTypes.CARROT || id == ItemTypes.POTATO || id == ItemTypes.BEETROOT;
    }
}
