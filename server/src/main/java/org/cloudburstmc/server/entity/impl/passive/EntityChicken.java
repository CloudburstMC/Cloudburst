package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Chicken;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityChicken extends Animal implements Chicken {

    public EntityChicken(EntityType<Chicken> type, Location location) {
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
            return 0.35f;
        }
        return 0.7f;
    }

    @Override
    public String getName() {
        return "Chicken";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.get(ItemIds.CHICKEN), ItemStack.get(ItemIds.FEATHER)};
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        setMaxHealth(4);
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        Identifier id = item.getId();

        return id == ItemIds.WHEAT_SEEDS || id == ItemIds.MELON_SEEDS || id == ItemIds.PUMPKIN_SEEDS ||
                id == ItemIds.BEETROOT_SEEDS;
    }
}
