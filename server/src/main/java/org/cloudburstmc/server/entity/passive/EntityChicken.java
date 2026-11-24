package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Chicken;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityChicken extends Animal implements Chicken {

    public EntityChicken(EntityType<Chicken> type, Location location) {
        super(type, location);
    }

    public void setBaby(boolean baby) {
        if (this.isBaby() != baby) {
            this.data.setFlag(BABY, baby);
            this.recalculateBoundingBox();
        }
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
    protected String getDefaultName() {
        return "Chicken";
    }

    @Override
    public ItemStack[] getDrops() {
        if (this.isBaby()) {
            return new ItemStack[0];
        }
        return new ItemStack[]{ItemStack.from(ItemTypes.CHICKEN), ItemStack.from(ItemTypes.FEATHER)};
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(4);
    }

    @Override
    public boolean isBreedingItem(ItemStack item) {
        ItemType itemType = item.getType();

        return itemType == ItemTypes.WHEAT_SEEDS || itemType == ItemTypes.MELON_SEEDS
                || itemType == ItemTypes.PUMPKIN_SEEDS || itemType == ItemTypes.BEETROOT_SEEDS
                || itemType == ItemTypes.TORCHFLOWER_SEEDS || itemType == ItemTypes.PITCHER_POD;
    }
}
