package org.cloudburstmc.server.entity.passive;

import lombok.val;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Chicken;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

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
        return new ItemStack[]{ItemStack.get(ItemTypes.CHICKEN), ItemStack.get(ItemTypes.FEATHER)};
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        setMaxHealth(4);
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        val id = item.getType();

        return id == ItemTypes.WHEAT_SEEDS || id == ItemTypes.MELON_SEEDS || id == ItemTypes.PUMPKIN_SEEDS ||
                id == ItemTypes.BEETROOT_SEEDS;
    }
}
