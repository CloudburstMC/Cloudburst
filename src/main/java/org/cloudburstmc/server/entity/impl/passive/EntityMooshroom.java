package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Mooshroom;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.Location;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntityMooshroom extends Animal implements Mooshroom {

    public EntityMooshroom(EntityType<Mooshroom> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (isBaby()) {
            return 0.45f;
        }
        return 0.9f;
    }

    @Override
    public float getHeight() {
        if (isBaby()) {
            return 0.7f;
        }
        return 1.4f;
    }

    @Override
    public String getName() {
        return "Mooshroom";
    }

    @Override
    public Item[] getDrops() {
        return new Item[]{Item.get(ItemIds.LEATHER), Item.get(ItemIds.BEEF)};
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        setMaxHealth(10);
    }
}
