package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Parrot;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.Location;

/**
 * @author PikyCZ
 */
public class EntityParrot extends Animal implements Parrot {

    public EntityParrot(EntityType<Parrot> type, Location location) {
        super(type, location);
    }

    public String getName() {
        return "Parrot";
    }

    @Override
    public float getWidth() {
        return 0.5f;
    }

    @Override
    public float getHeight() {
        return 0.9f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(6);
    }

    @Override
    public Item[] getDrops() {
        return new Item[]{Item.get(ItemIds.FEATHER)};
    }
}
