package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Pig;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

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
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(10);
    }

    @Override
    protected String getDefaultName() {
        return "Pig";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.builder().itemType(ItemTypes.PORKCHOP).build()};
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        ItemType itemType = item.getType();

        return itemType == ItemTypes.CARROT || itemType == ItemTypes.POTATO || itemType == ItemTypes.BEETROOT;
    }
}
