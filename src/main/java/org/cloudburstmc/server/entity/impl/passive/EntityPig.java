package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Pig;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.utils.Identifier;

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
    public Item[] getDrops() {
        return new Item[]{Item.get(ItemIds.PORKCHOP)};
    }

    @Override
    public boolean isBreedingItem(Item item) {
        Identifier id = item.getId();

        return id == ItemIds.CARROT || id == ItemIds.POTATO || id == ItemIds.BEETROOT;
    }
}
